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
    private final AttachmentRepository repository;
    private final AgentService agentService;

    public AttachmentAnalysisService(AttachmentRepository repository, AgentService agentService) {
        this.repository = repository;
        this.agentService = agentService;
    }

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

    public AttachmentAnalysisResponse analyzeFile(String filename,
                                                  String contentType,
                                                  ParsedAttachmentContent parsed,
                                                  String userId) {
        AttachmentRecord record = repository.save(filename, contentType, parsed);
        return analyze(record.getAttachmentId(), userId, null);
    }

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

    private String resolveSessionId(String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            return sessionId;
        }
        return "attachment-analysis-" + UUID.randomUUID().toString();
    }
}
