package com.sean.agenthub.agent.attachment.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.attachment.domain.DocumentOutlineResponse;
import com.sean.agenthub.agent.attachment.domain.AttachmentRecord;
import com.sean.agenthub.agent.attachment.domain.DocumentOutlineResult;
import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import com.sean.agenthub.agent.attachment.infrastructure.AttachmentRepository;
import java.io.IOException;
import com.sean.agenthub.agent.attachment.application.AttachmentAnalysisProperties.OutlineMode;
import com.sean.agenthub.agent.attachment.infrastructure.mimo.MimoChatClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 文档大纲和重点提炼用例。
 *
 * @author Sean
 */
@Service
public class DocumentOutlineService {
    private static final int MAX_MODEL_CHARS = 18000;

    private final AttachmentRepository repository;
    private final MimoChatClient mimoChatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AttachmentAnalysisProperties.Outline properties;

    public DocumentOutlineService(
            AttachmentRepository repository,
            MimoChatClient mimoChatClient,
            AttachmentAnalysisProperties properties) {
        this.repository = repository;
        this.mimoChatClient = mimoChatClient;
        this.properties = properties.getOutline();
    }

    public DocumentOutlineResponse analyzeFile(String filename,
                                               String contentType,
                                               ParsedAttachmentContent parsed,
                                               String userId) throws IOException {
        if (!"attachment-reviewer".equals(userId)) {
            DocumentOutlineResponse response = new DocumentOutlineResponse();
            response.setOk(false);
            response.setErrorMessage("Only attachment-reviewer can analyze document outlines");
            return response;
        }
        AttachmentRecord record = repository.save(filename, contentType, parsed);
        DocumentOutlineResult result = OutlineMode.MIMO == properties.getMode()
                ? analyzeWithMimo(record)
                : analyzeLocally(record);
        DocumentOutlineResponse response = new DocumentOutlineResponse();
        response.setAttachmentId(record.getAttachmentId());
        response.setOk(true);
        response.setOutline(result);
        return response;
    }

    private DocumentOutlineResult analyzeLocally(AttachmentRecord record) {
        DocumentOutlineResult result = baseResult(record);
        List<String> outline = extractOutline(record);
        result.setOutline(outline);
        result.setKeyPoints(extractKeyPoints(record.getText(), outline));
        result.setSummary("已解析 " + safe(record.getFilename()) + "，提炼出 "
                + result.getOutline().size() + " 个大纲节点和 "
                + result.getKeyPoints().size() + " 条重点。");
        return result;
    }

    private DocumentOutlineResult analyzeWithMimo(AttachmentRecord record) throws IOException {
        AttachmentAnalysisProperties.Mimo mimo = properties.getMimo();
        mimoChatClient.requireText(mimo.getApiKey(), "attachment.outline.mimo.api-key");
        mimoChatClient.requireText(mimo.getBaseUrl(), "attachment.outline.mimo.base-url");
        mimoChatClient.requireText(mimo.getModel(), "attachment.outline.mimo.model");

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", mimo.getModel());
        body.put("messages", buildMessages(record));
        body.put("temperature", 0);

        JsonNode response = mimoChatClient.chatCompletions(
                mimo.getBaseUrl(), mimo.getApiKey(), body,
                mimo.getConnectTimeoutMs(), mimo.getReadTimeoutMs());
        JsonNode error = response.path("error");
        if (error.isObject()) {
            throw new IllegalStateException("MiMo outline parser error: "
                    + error.path("message").asText(error.toString()));
        }
        String content = response.path("choices").path(0).path("message").path("content").asText("");
        return parseMimoResult(record, content);
    }

    private DocumentOutlineResult parseMimoResult(AttachmentRecord record, String content) throws IOException {
        String json = extractJson(content);
        if (json == null) {
            DocumentOutlineResult fallback = analyzeLocally(record);
            fallback.setSummary(content == null || content.trim().isEmpty() ? fallback.getSummary() : content.trim());
            return fallback;
        }
        JsonNode node = objectMapper.readTree(json);
        DocumentOutlineResult result = baseResult(record);
        if (hasText(node.path("title").asText())) {
            result.setTitle(node.path("title").asText().trim());
        }
        if (hasText(node.path("summary").asText())) {
            result.setSummary(node.path("summary").asText().trim());
        }
        result.setOutline(toStringList(node.path("outline")));
        result.setKeyPoints(toStringList(node.path("keyPoints")));
        if (result.getOutline().isEmpty() && result.getKeyPoints().isEmpty()) {
            return analyzeLocally(record);
        }
        return result;
    }

    private DocumentOutlineResult baseResult(AttachmentRecord record) {
        DocumentOutlineResult result = new DocumentOutlineResult();
        result.setAttachmentId(record.getAttachmentId());
        result.setFilename(record.getFilename());
        result.setParserName(record.getParserName());
        result.setTitle(resolveTitle(record));
        Map<String, Object> metadata = new LinkedHashMap<String, Object>(record.getMetadata());
        metadata.put("textLength", record.getText() == null ? 0 : record.getText().length());
        result.setMetadata(metadata);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractOutline(AttachmentRecord record) {
        Object headings = record.getMetadata().get("headings");
        if (headings instanceof List) {
            List<String> result = new ArrayList<String>();
            for (Object heading : (List<Object>) headings) {
                if (heading != null && hasText(String.valueOf(heading))) {
                    result.add(String.valueOf(heading).trim());
                }
            }
            if (!result.isEmpty()) {
                return limit(result, 12);
            }
        }

        List<String> result = new ArrayList<String>();
        String[] lines = record.getText() == null ? new String[0] : record.getText().split("\\r?\\n");
        for (String line : lines) {
            String normalized = normalizeLine(line);
            if (!hasText(normalized)) {
                continue;
            }
            if (looksLikeHeading(normalized)) {
                result.add(normalized);
            }
            if (result.size() >= 12) {
                break;
            }
        }
        if (result.isEmpty()) {
            for (String line : lines) {
                String normalized = normalizeLine(line);
                if (hasText(normalized)) {
                    result.add(trimTo(normalized, 80));
                }
                if (result.size() >= 5) {
                    break;
                }
            }
        }
        return result;
    }

    private List<String> extractKeyPoints(String text, List<String> outline) {
        List<String> keyPoints = new ArrayList<String>();
        String[] lines = text == null ? new String[0] : text.split("\\r?\\n");
        for (String line : lines) {
            String normalized = normalizeLine(line);
            if (!hasText(normalized) || outline.contains(normalized)) {
                continue;
            }
            if (isUsefulPoint(normalized)) {
                keyPoints.add(trimTo(normalized, 120));
            }
            if (keyPoints.size() >= 8) {
                break;
            }
        }
        if (keyPoints.isEmpty()) {
            for (String line : lines) {
                String normalized = normalizeLine(line);
                if (hasText(normalized) && !outline.contains(normalized)) {
                    keyPoints.add(trimTo(normalized, 120));
                }
                if (keyPoints.size() >= 5) {
                    break;
                }
            }
        }
        return keyPoints;
    }

    private List<Map<String, Object>> buildMessages(AttachmentRecord record) {
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(message("system", "你是文档分析助手。只输出 JSON，不要输出 Markdown。"));
        messages.add(message("user", "请为以下 PDF 或 Markdown 文档提炼大纲和重点。"
                + "JSON 字段必须包含 title、summary、outline、keyPoints。"
                + "outline 和 keyPoints 必须是字符串数组。"
                + "\n文件名：" + safe(record.getFilename())
                + "\n解析器：" + safe(record.getParserName())
                + "\n正文：\n" + trimTo(record.getText(), MAX_MODEL_CHARS)));
        return messages;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String resolveTitle(AttachmentRecord record) {
        Object title = record.getMetadata().get("title");
        if (title != null && hasText(String.valueOf(title))) {
            return String.valueOf(title).trim();
        }
        List<String> outline = extractOutline(record);
        if (!outline.isEmpty()) {
            return outline.get(0);
        }
        return hasText(record.getFilename()) ? record.getFilename() : "未命名文档";
    }

    private List<String> toStringList(JsonNode node) throws IOException {
        if (node == null || !node.isArray()) {
            return new ArrayList<String>();
        }
        return objectMapper.readValue(node.traverse(), new TypeReference<List<String>>() {
        });
    }

    private String extractJson(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        return first >= 0 && last > first ? trimmed.substring(first, last + 1) : null;
    }

    private boolean looksLikeHeading(String value) {
        return value.matches("^#{1,6}\\s+.+")
                || value.matches("^[一二三四五六七八九十]+[、.．].+")
                || value.matches("^第[一二三四五六七八九十0-9]+[章节条].+")
                || value.matches("^[0-9]+([.．][0-9]+)*[.．、\\s].+");
    }

    private boolean isUsefulPoint(String value) {
        return value.length() >= 12
                || value.contains("重点")
                || value.contains("要求")
                || value.contains("建议")
                || value.contains("风险")
                || value.contains("目标")
                || value.contains("政策");
    }

    private String normalizeLine(String line) {
        if (line == null) {
            return "";
        }
        return line.trim().replaceFirst("^#{1,6}\\s*", "").trim();
    }

    private List<String> limit(List<String> values, int max) {
        if (values.size() <= max) {
            return values;
        }
        return new ArrayList<String>(values.subList(0, max));
    }

    private String trimTo(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
