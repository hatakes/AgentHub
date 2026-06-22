package com.sean.agenthub.agent.attachment.infrastructure.parser;

import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文本型 PDF 附件解析器。
 *
 * @author Sean
 */
@Component
@Order(20)
public class PdfAttachmentContentParser implements AttachmentContentParser {

    /**
     * 判断是否支持解析当前文件。
     * <p>支持 application/pdf content-type 以及 .pdf 扩展名。</p>
     *
     * @param file 上传的文件
     * @return 是否支持
     */
    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        return (contentType != null && "application/pdf".equalsIgnoreCase(contentType))
                || (filename != null && filename.toLowerCase().endsWith(".pdf"));
    }

    /**
     * 解析 PDF 文件。
     * <p>使用 Apache PDFBox 提取文本内容，同时读取页数和文档标题等元数据。</p>
     *
     * @param file 上传的 PDF 文件
     * @return 解析后的内容，parserName 为 pdfbox
     * @throws IOException 文件读取异常
     */
    @Override
    public ParsedAttachmentContent parse(MultipartFile file) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Attachment content is empty after parsing");
            }
            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            metadata.put("format", "pdf");
            metadata.put("pageCount", document.getNumberOfPages());
            PDDocumentInformation information = document.getDocumentInformation();
            if (information != null && hasText(information.getTitle())) {
                metadata.put("title", information.getTitle().trim());
            }
            return new ParsedAttachmentContent(text, "pdfbox", metadata);
        } finally {
            document.close();
        }
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
