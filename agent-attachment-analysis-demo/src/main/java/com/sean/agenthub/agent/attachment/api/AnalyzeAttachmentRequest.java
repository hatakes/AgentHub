package com.sean.agenthub.agent.attachment.api;

/**
 * 分析已有附件的请求。
 *
 * @author Sean
 */
public class AnalyzeAttachmentRequest {
    private String attachmentId;
    private String userId;
    private String sessionId;

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
