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

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** MiMo 聊天客户端 */
    private final MimoChatClient mimoChatClient;

    /** 图片解析配置 */
    private final AttachmentAnalysisProperties.Image properties;

    /**
     * 构造器注入依赖。
     *
     * @param mimoChatClient MiMo 聊天客户端
     * @param properties     附件分析配置
     */
    public ImageAttachmentContentParser(
            MimoChatClient mimoChatClient,
            AttachmentAnalysisProperties properties) {
        this.mimoChatClient = mimoChatClient;
        this.properties = properties.getParser().getImage();
    }

    /**
     * 判断是否支持解析当前文件。
     * <p>当文件 content-type 以 image/ 开头时支持。</p>
     *
     * @param file 上传的文件
     * @return 是否支持
     */
    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 解析图片附件。
     * <p>根据配置选择 mock 模式或 MiMo 多模态模式进行解析。</p>
     *
     * @param file 上传的图片文件
     * @return 解析后的内容
     * @throws IOException 文件读取或模型调用异常
     */
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

    /**
     * 使用 MiMo 多模态模型解析图片。
     * <p>将图片转为 base64 data URL，构造 OpenAI-compatible 多模态消息调用 MiMo，
     * 模型返回 JSON 格式的证件识别结果。</p>
     *
     * @param file 上传的图片文件
     * @return 解析后的内容，parserName 为 mimo-image
     * @throws IOException 模型调用或解析异常
     */
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

    /**
     * 构建发送给 MiMo 的多模态消息。
     * <p>包含文字指令和图片 URL，要求模型以 JSON 格式输出证件识别结果。</p>
     *
     * @param imageUrl 图片的 base64 data URL
     * @return 消息列表
     */
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

    /**
     * 将模型返回的 JSON 内容转换为可搜索的纯文本。
     * <p>提取 rawText、证件类型、姓名、证件号、出生日期和有效期等字段，
     * 拼接为空格分隔的文本，便于后续 Tool 进行关键词匹配。</p>
     *
     * @param content 模型返回的原始文本
     * @return 可搜索的纯文本
     * @throws IOException JSON 解析异常
     */
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

    /**
     * 判断证件类型是否为居民身份证。
     *
     * @param documentType 证件类型标识
     * @return 是否为身份证
     */
    private boolean isIdCard(String documentType) {
        return documentType != null
                && ("ID_CARD".equalsIgnoreCase(documentType)
                || documentType.contains("身份证")
                || documentType.contains("居民身份证"));
    }

    /**
     * 标准化日期格式为 yyyy-MM-dd。
     * <p>支持中文、点号、斜杠等常见日期分隔符。</p>
     *
     * @param value 原始日期字符串
     * @return 标准化后的日期字符串
     */
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

    /**
     * 左填充零到两位数。
     *
     * @param value 数字字符串
     * @return 两位数字符串
     */
    private String leftPad2(String value) {
        return value.length() == 1 ? "0" + value : value;
    }

    /**
     * 从文本中提取 JSON 对象字符串。
     * <p>支持处理 Markdown 代码块包裹的 JSON。</p>
     *
     * @param content 原始文本
     * @return JSON 字符串，未找到则返回 null
     */
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

    /**
     * 向 StringBuilder 追加非空文本，前面加空格分隔。
     *
     * @param sb    目标 StringBuilder
     * @param value 待追加的文本
     */
    private void append(StringBuilder sb, String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(value.trim());
        }
    }

    /**
     * 向 StringBuilder 追加带标签的文本。
     *
     * @param sb    目标 StringBuilder
     * @param label 标签文本
     * @param value 值文本
     */
    private void appendLabel(StringBuilder sb, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            append(sb, label + " " + value.trim());
        }
    }

}
