package com.sean.agenthub.agent.attachment.api;

import com.sean.agenthub.agent.attachment.application.AttachmentAnalysisService;
import com.sean.agenthub.agent.attachment.application.DocumentOutlineService;
import com.sean.agenthub.agent.attachment.domain.AnalyzeAttachmentRequest;
import com.sean.agenthub.agent.attachment.domain.AttachmentAnalysisResponse;
import com.sean.agenthub.agent.attachment.domain.DocumentOutlineResponse;
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

    /** 附件分析业务用例服务 */
    private final AttachmentAnalysisService analysisService;

    /** 文档大纲提炼服务 */
    private final DocumentOutlineService outlineService;

    /** 附件解析器注册表，负责根据文件类型选择合适的解析器 */
    private final AttachmentContentParserRegistry parserRegistry;

    /**
     * 构造器注入所有依赖的服务。
     *
     * @param analysisService 附件分析服务
     * @param outlineService  文档大纲服务
     * @param parserRegistry  解析器注册表
     */
    public AttachmentAnalysisController(AttachmentAnalysisService analysisService,
                                        DocumentOutlineService outlineService,
                                        AttachmentContentParserRegistry parserRegistry) {
        this.analysisService = analysisService;
        this.outlineService = outlineService;
        this.parserRegistry = parserRegistry;
    }

    /**
     * 根据已有附件 ID 触发分析。
     *
     * @param request 包含 attachmentId、userId 和 sessionId 的分析请求
     * @return 附件分析结果响应
     */
    @PostMapping("/attachment-analysis/analyze")
    public AttachmentAnalysisResponse analyze(@RequestBody AnalyzeAttachmentRequest request) {
        return analysisService.analyze(request.getAttachmentId(), request.getUserId(), request.getSessionId());
    }

    /**
     * 上传文件并立即触发附件分析。
     *
     * @param file   上传的文件
     * @param userId 发起分析的用户 ID
     * @return 附件分析结果响应
     * @throws IOException 文件读取异常
     */
    @PostMapping(value = "/attachment-analysis/analyze-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentAnalysisResponse analyzeFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("userId") String userId) throws IOException {
        ParsedAttachmentContent parsed = parserRegistry.parse(file);
        return analysisService.analyzeFile(file.getOriginalFilename(), file.getContentType(), parsed, userId);
    }

    /**
     * 上传文件并触发文档大纲和重点提炼。
     *
     * @param file   上传的文件
     * @param userId 发起分析的用户 ID
     * @return 文档大纲提炼结果响应
     * @throws IOException 文件读取异常
     */
    @PostMapping(value = "/attachment-analysis/outline-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentOutlineResponse outlineFile(@RequestParam("file") MultipartFile file,
                                               @RequestParam("userId") String userId) throws IOException {
        ParsedAttachmentContent parsed = parserRegistry.parse(file);
        return outlineService.analyzeFile(file.getOriginalFilename(), file.getContentType(), parsed, userId);
    }

    /**
     * 处理非法参数异常，返回 400 状态码。
     *
     * @param ex 非法参数异常
     * @return 包含错误信息的响应 Map
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(IllegalArgumentException ex) {
        return errorResponse(ex);
    }

    /**
     * 处理非法状态异常，返回 400 状态码。
     *
     * @param ex 非法状态异常
     * @return 包含错误信息的响应 Map
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidState(IllegalStateException ex) {
        return errorResponse(ex);
    }

    /**
     * 构建统一的错误响应结构。
     *
     * @param ex 运行时异常
     * @return 包含 ok=false 和 errorMessage 的响应 Map
     */
    private Map<String, Object> errorResponse(RuntimeException ex) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", false);
        response.put("errorMessage", ex.getMessage());
        return response;
    }
}
