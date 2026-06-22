package com.sean.agenthub.agent.attachment.application;

import com.sean.agenthub.agent.attachment.domain.AttachmentAnalysisResponse;
import com.sean.agenthub.agent.attachment.domain.AttachmentRecord;
import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import com.sean.agenthub.agent.attachment.infrastructure.AttachmentRepository;
import com.sean.agenthub.agent.attachment.support.AttachmentToolSupport;
import com.sean.agenthub.agent.core.api.AgentService;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 附件分析业务用例编排。
 *
 * @author Sean
 */
@Service
public class AttachmentAnalysisService {

    /** 附件存储仓库 */
    private final AttachmentRepository repository;

    /** Agent 核心服务，负责调度模型和 Tool */
    private final AgentService agentService;

    /**
     * 构造器注入依赖。
     *
     * @param repository  附件仓库
     * @param agentService Agent 核心服务
     */
    public AttachmentAnalysisService(AttachmentRepository repository, AgentService agentService) {
        this.repository = repository;
        this.agentService = agentService;
    }

    /**
     * 根据附件 ID 触发分析。
     * <p>先校验附件是否存在，再构建 AgentRequest 委托给 AgentService 执行 Tool 调度链路。</p>
     *
     * @param attachmentId 附件 ID
     * @param userId       发起分析的用户 ID
     * @param sessionId    会话 ID，为空时自动生成
     * @return 附件分析结果响应
     */
    public AttachmentAnalysisResponse analyze(String attachmentId, String userId, String sessionId) {
        try {
            repository.getRequired(attachmentId);
        } catch (IllegalArgumentException ex) {
            AttachmentAnalysisResponse response = new AttachmentAnalysisResponse();
            response.setAttachmentId(attachmentId);
            response.setOk(false);
            response.setErrorMessage(ex.getMessage());
            return response;
        }

        AgentRequest request = new AgentRequest();
        request.setSessionId(resolveSessionId(sessionId));
        request.setUserId(userId);
        request.setMessage("请分析附件 " + attachmentId);

        AgentResponse agentResponse = agentService.chat(request);
        return toResponse(attachmentId, agentResponse);
    }

    /**
     * 上传并分析文件。
     * <p>先保存附件到仓库，再调用 {@link #analyze} 触发分析链路。</p>
     *
     * @param filename    文件名
     * @param contentType 文件 MIME 类型
     * @param parsed      解析后的附件内容
     * @param userId      发起分析的用户 ID
     * @return 附件分析结果响应
     */
    public AttachmentAnalysisResponse analyzeFile(String filename,
                                                  String contentType,
                                                  ParsedAttachmentContent parsed,
                                                  String userId) {
        AttachmentRecord record = repository.save(filename, contentType, parsed);
        return analyze(record.getAttachmentId(), userId, null);
    }

    /**
     * 将 AgentResponse 转换为 AttachmentAnalysisResponse。
     *
     * @param attachmentId 附件 ID
     * @param agentResponse Agent 服务返回的响应
     * @return 附件分析结果响应
     */
    private AttachmentAnalysisResponse toResponse(String attachmentId, AgentResponse agentResponse) {
        AttachmentAnalysisResponse response = new AttachmentAnalysisResponse();
        response.setAttachmentId(attachmentId);
        response.setOk(agentResponse.isOk());
        response.setAnswer(agentResponse.getAnswer());
        if (agentResponse.isOk()) {
            response.setAnalysis(AttachmentToolSupport.analysisResult(attachmentId, repository.getRequired(attachmentId)));
        }
        response.setErrorMessage(agentResponse.getErrorMessage());
        response.setToolCalls(agentResponse.getToolCalls());
        return response;
    }

    /**
     * 解析会话 ID，为空时自动生成。
     *
     * @param sessionId 会话 ID
     * @return 有效的会话 ID
     */
    private String resolveSessionId(String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            return sessionId;
        }
        return "attachment-analysis-" + UUID.randomUUID().toString();
    }
}
