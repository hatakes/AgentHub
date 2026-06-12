package com.sean.agenthub.agent.attachment.infrastructure.parser;

import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件解析器选择器。
 *
 * @author Sean
 */
@Component
public class AttachmentContentParserRegistry {
    private final List<AttachmentContentParser> parsers;

    public AttachmentContentParserRegistry(List<AttachmentContentParser> parsers) {
        this.parsers = parsers;
    }

    public ParsedAttachmentContent parse(MultipartFile file) throws IOException {
        for (AttachmentContentParser parser : parsers) {
            if (parser.supports(file)) {
                return parser.parse(file);
            }
        }
        throw new IllegalArgumentException("Unsupported attachment content type: " + file.getContentType());
    }
}
