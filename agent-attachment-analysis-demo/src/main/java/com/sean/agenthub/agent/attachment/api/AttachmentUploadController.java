package com.sean.agenthub.agent.attachment.api;

import com.sean.agenthub.agent.attachment.domain.AttachmentRecord;
import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import com.sean.agenthub.agent.attachment.infrastructure.AttachmentRepository;
import com.sean.agenthub.agent.attachment.infrastructure.parser.AttachmentContentParserRegistry;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件上传入口，返回后续 Agent 分析使用的 attachmentId。
 *
 * @author Sean
 */
@RestController
public class AttachmentUploadController {
    private final AttachmentRepository repository;
    private final AttachmentContentParserRegistry parserRegistry;

    public AttachmentUploadController(AttachmentRepository repository,
                                      AttachmentContentParserRegistry parserRegistry) {
        this.repository = repository;
        this.parserRegistry = parserRegistry;
    }

    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        ParsedAttachmentContent parsed = parserRegistry.parse(file);
        AttachmentRecord record = repository.save(file.getOriginalFilename(), file.getContentType(), parsed);

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("attachmentId", record.getAttachmentId());
        response.put("filename", record.getFilename());
        response.put("parserName", record.getParserName());
        return response;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", false);
        response.put("errorMessage", ex.getMessage());
        return response;
    }
}
