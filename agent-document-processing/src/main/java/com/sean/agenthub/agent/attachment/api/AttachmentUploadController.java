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

    /** 附件存储仓库 */
    private final AttachmentRepository repository;

    /** 附件解析器注册表，负责根据文件类型选择合适的解析器 */
    private final AttachmentContentParserRegistry parserRegistry;

    /**
     * 构造器注入依赖。
     *
     * @param repository    附件仓库
     * @param parserRegistry 解析器注册表
     */
    public AttachmentUploadController(AttachmentRepository repository,
                                      AttachmentContentParserRegistry parserRegistry) {
        this.repository = repository;
        this.parserRegistry = parserRegistry;
    }

    /**
     * 上传附件并返回 attachmentId。
     * <p>流程：先通过解析器注册表解析文件内容，再保存到内存仓库，最后返回附件 ID 和元数据。</p>
     *
     * @param file 上传的文件
     * @return 包含 attachmentId、filename 和 parserName 的响应 Map
     * @throws IOException 文件读取异常
     */
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
