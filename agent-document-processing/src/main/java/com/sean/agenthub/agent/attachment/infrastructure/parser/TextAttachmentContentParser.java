package com.sean.agenthub.agent.attachment.infrastructure.parser;

import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文本附件解析器。
 *
 * @author Sean
 */
@Component
@Order(100)
public class TextAttachmentContentParser implements AttachmentContentParser {
    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        return (contentType != null && contentType.startsWith("text/"))
                || (filename != null && filename.toLowerCase().endsWith(".txt"));
    }

    @Override
    public ParsedAttachmentContent parse(MultipartFile file) throws IOException {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment content is empty after parsing");
        }
        return new ParsedAttachmentContent(text, "text");
    }
}
