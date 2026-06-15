package com.sean.agenthub.agent.attachment.domain;

import com.sean.agenthub.agent.core.model.ToolCallResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 附件分析业务接口响应。
 *
 * @author Sean
 */
public class AttachmentAnalysisResponse {
    private String attachmentId;
    private boolean ok;
    private String answer;
    private AttachmentAnalysisResult analysis;
    private String errorMessage;
    private List<ToolCallResult> toolCalls = new ArrayList<ToolCallResult>();

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public AttachmentAnalysisResult getAnalysis() {
        return analysis;
    }

    public void setAnalysis(AttachmentAnalysisResult analysis) {
        this.analysis = analysis;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<ToolCallResult> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallResult> toolCalls) {
        this.toolCalls = toolCalls == null ? new ArrayList<ToolCallResult>() : toolCalls;
    }
}
