package com.sean.agenthub.agent.attachment.infrastructure.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
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
    private final String mode;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public ImageAttachmentContentParser(
            @Value("${attachment.parser.image.mode:mock}") String mode,
            @Value("${attachment.parser.image.mimo.base-url:${AGENTHUB_MIMO_BASE_URL:https://api.xiaomimimo.com/v1}}") String baseUrl,
            @Value("${attachment.parser.image.mimo.api-key:${AGENTHUB_MIMO_API_KEY:}}") String apiKey,
            @Value("${attachment.parser.image.mimo.model:${AGENTHUB_MODEL_MIMO_IMAGE:mimo-v2-omni}}") String model,
            @Value("${attachment.parser.image.mimo.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${attachment.parser.image.mimo.read-timeout-ms:120000}") int readTimeoutMs) {
        this.mode = mode;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    @Override
    public ParsedAttachmentContent parse(MultipartFile file) throws IOException {
        if ("mimo".equalsIgnoreCase(mode)) {
            return parseWithMimo(file);
        }
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment content is empty after parsing");
        }
        return new ParsedAttachmentContent(text, "image-mock");
    }

    private ParsedAttachmentContent parseWithMimo(MultipartFile file) throws IOException {
        requireText(apiKey, "attachment.parser.image.mimo.api-key");
        requireText(baseUrl, "attachment.parser.image.mimo.base-url");
        requireText(model, "attachment.parser.image.mimo.model");

        String contentType = file.getContentType() == null ? "image/png" : file.getContentType();
        String imageUrl = "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(file.getBytes());

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("messages", buildMessages(imageUrl));
        body.put("temperature", 0);

        JsonNode response = postJson(endpoint(), body);
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

    private JsonNode postJson(String endpoint, Map<String, Object> body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        byte[] bytes = objectMapper.writeValueAsBytes(body);
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(bytes);
        } finally {
            outputStream.close();
        }

        int status = connection.getResponseCode();
        InputStream inputStream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String responseBody = readAll(inputStream, StandardCharsets.UTF_8);
        return objectMapper.readTree(responseBody);
    }

    private String readAll(InputStream inputStream, Charset charset) throws IOException {
        if (inputStream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            reader.close();
        }
        return sb.toString();
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

    private String endpoint() {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        return normalized + "/v1/chat/completions";
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

    private void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " is required when attachment.parser.image.mode=mimo");
        }
    }
}
