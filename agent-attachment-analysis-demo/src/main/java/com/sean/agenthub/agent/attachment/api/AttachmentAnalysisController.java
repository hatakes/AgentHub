package com.sean.agenthub.agent.attachment.api;

import com.sean.agenthub.agent.attachment.application.AttachmentAnalysisService;
import com.sean.agenthub.agent.attachment.application.DocumentOutlineService;
import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import com.sean.agenthub.agent.attachment.infrastructure.parser.AttachmentContentParserRegistry;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 面向业务系统的附件分析接口。
 *
 * @author Sean
 */
@RestController
public class AttachmentAnalysisController {
    private final AttachmentAnalysisService analysisService;
    private final DocumentOutlineService outlineService;
    private final AttachmentContentParserRegistry parserRegistry;

    public AttachmentAnalysisController(AttachmentAnalysisService analysisService,
                                        DocumentOutlineService outlineService,
                                        AttachmentContentParserRegistry parserRegistry) {
        this.analysisService = analysisService;
        this.outlineService = outlineService;
        this.parserRegistry = parserRegistry;
    }

    @PostMapping("/attachment-analysis/analyze")
    public AttachmentAnalysisResponse analyze(@RequestBody AnalyzeAttachmentRequest request) {
        return analysisService.analyze(request.getAttachmentId(), request.getUserId(), request.getSessionId());
    }

    @PostMapping(value = "/attachment-analysis/analyze-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentAnalysisResponse analyzeFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("userId") String userId) throws IOException {
        ParsedAttachmentContent parsed = parserRegistry.parse(file);
        return analysisService.analyzeFile(file.getOriginalFilename(), file.getContentType(), parsed, userId);
    }

    @PostMapping(value = "/attachment-analysis/outline-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentOutlineResponse outlineFile(@RequestParam("file") MultipartFile file,
                                               @RequestParam("userId") String userId) throws IOException {
        ParsedAttachmentContent parsed = parserRegistry.parse(file);
        return outlineService.analyzeFile(file.getOriginalFilename(), file.getContentType(), parsed, userId);
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
