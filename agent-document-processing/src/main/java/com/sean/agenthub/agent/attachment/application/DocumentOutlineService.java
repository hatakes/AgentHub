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

    /** 发送给模型的最大文本字符数 */
    private static final int MAX_MODEL_CHARS = 18000;

    /** 附件存储仓库 */
    private final AttachmentRepository repository;

    /** MiMo 聊天客户端，用于调用大模型 */
    private final MimoChatClient mimoChatClient;

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 大纲提炼配置 */
    private final AttachmentAnalysisProperties.Outline properties;

    /**
     * 构造器注入依赖。
     *
     * @param repository  附件仓库
     * @param mimoChatClient MiMo 聊天客户端
     * @param properties  附件分析配置
     */
    public DocumentOutlineService(
            AttachmentRepository repository,
            MimoChatClient mimoChatClient,
            AttachmentAnalysisProperties properties) {
        this.repository = repository;
        this.mimoChatClient = mimoChatClient;
        this.properties = properties.getOutline();
    }

    /**
     * 上传文件并触发文档大纲和重点提炼。
     * <p>仅允许 attachment-reviewer 用户调用。根据配置选择本地规则或 MiMo 模型进行提炼。</p>
     *
     * @param filename    文件名
     * @param contentType 文件 MIME 类型
     * @param parsed      解析后的附件内容
     * @param userId      发起分析的用户 ID
     * @return 文档大纲提炼结果响应
     * @throws IOException MiMo 调用异常
     */
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

    /**
     * 使用本地规则提炼文档大纲和重点。
     *
     * @param record 附件记录
     * @return 文档大纲提炼结果
     */
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

    /**
     * 使用 MiMo 大模型提炼文档大纲和重点。
     *
     * @param record 附件记录
     * @return 文档大纲提炼结果
     * @throws IOException MiMo 调用异常
     */
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

    /**
     * 解析 MiMo 模型返回的大纲结果。
     * <p>尝试从模型输出中提取 JSON，解析失败时回退到本地规则。</p>
     *
     * @param record  附件记录
     * @param content 模型返回的原始文本
     * @return 文档大纲提炼结果
     * @throws IOException JSON 解析异常
     */
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

    /**
     * 构建基础的大纲结果对象，填充附件 ID、文件名、解析器名称、标题和元数据。
     *
     * @param record 附件记录
     * @return 基础大纲结果对象
     */
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

    /**
     * 从附件记录中提取大纲节点。
     * <p>优先使用 metadata 中的 headings，否则从正文中按标题格式规则提取，最多返回 12 个节点。</p>
     *
     * @param record 附件记录
     * @return 大纲节点列表
     */
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

    /**
     * 从正文中提取重点内容。
     * <p>排除已作为大纲节点的行，优先选取包含关键词或长度足够的行，最多返回 8 条。</p>
     *
     * @param text    文档正文
     * @param outline 已提取的大纲节点
     * @return 重点内容列表
     */
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

    /**
     * 构建发送给 MiMo 模型的消息列表。
     *
     * @param record 附件记录
     * @return 消息列表，包含 system 和 user 角色
     */
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

    /**
     * 构建单条消息 Map。
     *
     * @param role    消息角色（system/user）
     * @param content 消息内容
     * @return 消息 Map
     */
    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 解析文档标题。
     * <p>优先使用 metadata 中的 title，其次取大纲第一条，最后使用文件名或默认值。</p>
     *
     * @param record 附件记录
     * @return 文档标题
     */
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

    /**
     * 将 JSON 数组节点转换为字符串列表。
     *
     * @param node JSON 数组节点
     * @return 字符串列表
     * @throws IOException JSON 解析异常
     */
    private List<String> toStringList(JsonNode node) throws IOException {
        if (node == null || !node.isArray()) {
            return new ArrayList<String>();
        }
        return objectMapper.readValue(node.traverse(), new TypeReference<List<String>>() {
        });
    }

    /**
     * 从文本中提取 JSON 对象字符串。
     *
     * @param content 原始文本
     * @return JSON 字符串，未找到则返回 null
     */
    private String extractJson(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        return first >= 0 && last > first ? trimmed.substring(first, last + 1) : null;
    }

    /**
     * 判断文本行是否看起来像标题。
     * <p>支持 Markdown 标题、中文序号、章节标记和数字编号等格式。</p>
     *
     * @param value 文本行
     * @return 是否像标题
     */
    private boolean looksLikeHeading(String value) {
        return value.matches("^#{1,6}\\s+.+")
                || value.matches("^[一二三四五六七八九十]+[、.．].+")
                || value.matches("^第[一二三四五六七八九十0-9]+[章节条].+")
                || value.matches("^[0-9]+([.．][0-9]+)*[.．、\\s].+");
    }

    /**
     * 判断文本行是否是有用的重点内容。
     * <p>长度超过 12 字符或包含特定关键词时认为是有用的。</p>
     *
     * @param value 文本行
     * @return 是否是有用的重点
     */
    private boolean isUsefulPoint(String value) {
        return value.length() >= 12
                || value.contains("重点")
                || value.contains("要求")
                || value.contains("建议")
                || value.contains("风险")
                || value.contains("目标")
                || value.contains("政策");
    }

    /**
     * 标准化文本行，去除首尾空白和 Markdown 标记。
     *
     * @param line 原始文本行
     * @return 标准化后的文本
     */
    private String normalizeLine(String line) {
        if (line == null) {
            return "";
        }
        return line.trim().replaceFirst("^#{1,6}\\s*", "").trim();
    }

    /**
     * 限制列表长度，超出时截断。
     *
     * @param values 原始列表
     * @param max    最大长度
     * @return 截断后的列表
     */
    private List<String> limit(List<String> values, int max) {
        if (values.size() <= max) {
            return values;
        }
        return new ArrayList<String>(values.subList(0, max));
    }

    /**
     * 截断字符串到指定最大长度。
     *
     * @param value 原始字符串
     * @param max   最大长度
     * @return 截断后的字符串
     */
    private String trimTo(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    /**
     * 安全地处理 null 字符串。
     *
     * @param value 原始字符串
     * @return 非 null 的字符串
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 判断字符串是否包含有效文本。
     *
     * @param value 字符串
     * @return 是否包含有效文本
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
