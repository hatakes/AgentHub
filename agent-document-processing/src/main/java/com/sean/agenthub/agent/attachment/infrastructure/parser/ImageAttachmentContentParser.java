package com.sean.agenthub.agent.attachment.infrastructure.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.attachment.application.AttachmentAnalysisProperties;
import com.sean.agenthub.agent.attachment.application.AttachmentAnalysisProperties.ImageParserMode;
import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.io.IOException;
import com.sean.agenthub.agent.attachment.infrastructure.mimo.MimoChatClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片附件解析器。
 *
 * <p>默认使用 mock 模式，便于本地测试；设置 attachment.parser.image.mode=mimo 后，
 * 通过 OpenAI-compatible 多模态消息调用 MiMo。</p>
 *
 * @author Sean
 */
@Component
public class ImageAttachmentContentParser implements AttachmentContentParser {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MimoChatClient mimoChatClient;
    private final AttachmentAnalysisProperties.Image properties;

    public ImageAttachmentContentParser(
            MimoChatClient mimoChatClient,
            AttachmentAnalysisProperties properties) {
        this.mimoChatClient = mimoChatClient;
        this.properties = properties.getParser().getImage();
    }

    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    @Override
    public ParsedAttachmentContent parse(MultipartFile file) throws IOException {
        if (ImageParserMode.MIMO == properties.getMode()) {
            return parseWithMimo(file);
        }
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment content is empty after parsing");
        }
        return new ParsedAttachmentContent(text, "image-mock");
    }

    private ParsedAttachmentContent parseWithMimo(MultipartFile file) throws IOException {
        AttachmentAnalysisProperties.Mimo mimo = properties.getMimo();
        mimoChatClient.requireText(mimo.getApiKey(), "attachment.parser.image.mimo.api-key");
        mimoChatClient.requireText(mimo.getBaseUrl(), "attachment.parser.image.mimo.base-url");
        mimoChatClient.requireText(mimo.getModel(), "attachment.parser.image.mimo.model");

        String contentType = file.getContentType() == null ? "image/png" : file.getContentType();
        String imageUrl = "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(file.getBytes());

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", mimo.getModel());
        body.put("messages", buildMessages(imageUrl));
        body.put("temperature", 0);

        JsonNode response = mimoChatClient.chatCompletions(
                mimo.getBaseUrl(), mimo.getApiKey(), body,
                mimo.getConnectTimeoutMs(), mimo.getReadTimeoutMs());
        JsonNode error = response.path("error");
        if (error.isObject()) {
            throw new IllegalStateException("MiMo image parser error: " + error.path("message").asText(error.toString()));
        }
        String content = response.path("choices").path(0).path("message").path("content").asText("");
        String text = toSearchableText(content);
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment content is empty after parsing");
        }
        return new ParsedAttachmentContent(text, "mimo-image");
    }

    private List<Map<String, Object>> buildMessages(String imageUrl) {
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("type", "text");
        text.put("text", "请识别图片中的证件或业务材料。只输出 JSON，不要输出 Markdown。"
                + "字段包括 documentType、name、idNumber、birthDate、validUntil、rawText。"
                + "如果是居民身份证，documentType 必须输出 ID_CARD，birthDate 必须使用 yyyy-MM-dd。"
                + "不要猜测图片中不存在的信息，识别不到的字段输出空字符串。");
        content.add(text);

        Map<String, Object> image = new LinkedHashMap<String, Object>();
        image.put("type", "image_url");
        Map<String, Object> imageUrlObject = new LinkedHashMap<String, Object>();
        imageUrlObject.put("url", imageUrl);
        image.put("image_url", imageUrlObject);
        content.add(image);

        user.put("content", content);
        messages.add(user);
        return messages;
    }

    private String toSearchableText(String content) throws IOException {
        String json = extractJson(content);
        if (json == null) {
            return content == null ? "" : content;
        }
        JsonNode node = objectMapper.readTree(json);
        StringBuilder text = new StringBuilder();
        append(text, node.path("rawText").asText(""));
        String documentType = node.path("documentType").asText("");
        if (isIdCard(documentType)) {
            append(text, "居民身份证");
            append(text, "ID_CARD");
        }
        append(text, documentType);
        appendLabel(text, "姓名", node.path("name").asText(""));
        appendLabel(text, "公民身份号码", node.path("idNumber").asText(""));
        appendLabel(text, "出生日期", normalizeDate(node.path("birthDate").asText("")));
        appendLabel(text, "有效期", node.path("validUntil").asText(""));
        return text.toString();
    }

    private boolean isIdCard(String documentType) {
        return documentType != null
                && ("ID_CARD".equalsIgnoreCase(documentType)
                || documentType.contains("身份证")
                || documentType.contains("居民身份证"));
    }

    private String normalizeDate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim()
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
                .replace(".", "-")
                .replace("/", "-");
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("((19|20)\\d{2})-(\\d{1,2})-(\\d{1,2})")
                .matcher(normalized);
        if (!matcher.find()) {
            return value.trim();
        }
        return matcher.group(1) + "-"
                + leftPad2(matcher.group(3)) + "-"
                + leftPad2(matcher.group(4));
    }

    private String leftPad2(String value) {
        return value.length() == 1 ? "0" + value : value;
    }

    private String extractJson(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int first = trimmed.indexOf('{');
            int last = trimmed.lastIndexOf('}');
            return first >= 0 && last > first ? trimmed.substring(first, last + 1) : null;
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        return first >= 0 && last > first ? trimmed.substring(first, last + 1) : null;
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(value.trim());
        }
    }

    private void appendLabel(StringBuilder sb, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            append(sb, label + " " + value.trim());
        }
    }

}
