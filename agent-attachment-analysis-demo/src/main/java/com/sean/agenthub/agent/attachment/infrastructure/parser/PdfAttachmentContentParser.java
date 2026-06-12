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
    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        return (contentType != null && "application/pdf".equalsIgnoreCase(contentType))
                || (filename != null && filename.toLowerCase().endsWith(".pdf"));
    }

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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
