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
     * <p>通常根据文件的 content-type 或扩展名判断。</p>
     *
     * @param file 上传的文件
     * @return 是否支持解析
     */
    boolean supports(MultipartFile file);

    /**
     * 解析附件内容，提取纯文本和元数据。
     *
     * @param file 上传的文件
     * @return 解析后的内容
     * @throws IOException 文件读取异常
     */
    ParsedAttachmentContent parse(MultipartFile file) throws IOException;
}
