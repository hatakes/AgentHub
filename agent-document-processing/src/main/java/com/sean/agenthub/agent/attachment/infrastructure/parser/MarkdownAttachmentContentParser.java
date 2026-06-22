package com.sean.agenthub.agent.attachment.infrastructure.parser;

import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Markdown 文档解析器。
 *
 * @author Sean
 */
@Component
@Order(10)
public class MarkdownAttachmentContentParser implements AttachmentContentParser {

    /**
     * 判断是否支持解析当前文件。
     * <p>支持 text/markdown、text/x-markdown content-type，以及 .md、.markdown 扩展名。</p>
     *
     * @param file 上传的文件
     * @return 是否支持
     */
    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        return (contentType != null && ("text/markdown".equalsIgnoreCase(contentType)
                || "text/x-markdown".equalsIgnoreCase(contentType)))
                || (filename != null && (filename.toLowerCase().endsWith(".md")
                || filename.toLowerCase().endsWith(".markdown")));
    }

    /**
     * 解析 Markdown 文件。
     * <p>读取文件内容为 UTF-8 文本，提取标题列表作为元数据。</p>
     *
     * @param file 上传的 Markdown 文件
     * @return 解析后的内容，parserName 为 markdown
     * @throws IOException 文件读取异常
     */
    @Override
    public ParsedAttachmentContent parse(MultipartFile file) throws IOException {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment content is empty after parsing");
        }
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("format", "markdown");
        metadata.put("headings", extractHeadings(text));
        return new ParsedAttachmentContent(text, "markdown", metadata);
    }

    /**
     * 从 Markdown 文本中提取标题列表。
     * <p>识别以 # 开头的行，去除 # 符号后返回标题文本。</p>
     *
     * @param text Markdown 文本
     * @return 标题列表
     */
    private List<String> extractHeadings(String text) {
        List<String> headings = new ArrayList<String>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.startsWith("#")) {
                String heading = trimmed.replaceFirst("^#+\\s*", "").trim();
                if (!heading.isEmpty()) {
                    headings.add(heading);
                }
            }
        }
        return headings;
    }
}
