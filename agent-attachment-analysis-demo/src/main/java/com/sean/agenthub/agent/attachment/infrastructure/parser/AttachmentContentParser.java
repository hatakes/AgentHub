package com.sean.agenthub.agent.attachment.infrastructure.parser;

import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件内容解析器。
 *
 * @author Sean
 */
public interface AttachmentContentParser {
    /**
     * 是否支持解析当前文件。
     */
    boolean supports(MultipartFile file);

    /**
     * 解析附件内容。
     */
    ParsedAttachmentContent parse(MultipartFile file) throws IOException;
}
