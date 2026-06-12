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
    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        return (contentType != null && ("text/markdown".equalsIgnoreCase(contentType)
                || "text/x-markdown".equalsIgnoreCase(contentType)))
                || (filename != null && (filename.toLowerCase().endsWith(".md")
                || filename.toLowerCase().endsWith(".markdown")));
    }

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
