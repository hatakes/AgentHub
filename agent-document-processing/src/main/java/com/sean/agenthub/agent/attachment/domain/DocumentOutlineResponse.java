package com.sean.agenthub.agent.attachment.domain;

/**
 * 文档大纲提炼接口响应。
 *
 * @author Sean
 */
public class DocumentOutlineResponse {
    private String attachmentId;
    private boolean ok;
    private DocumentOutlineResult outline;
    private String errorMessage;

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

    public DocumentOutlineResult getOutline() {
        return outline;
    }

    public void setOutline(DocumentOutlineResult outline) {
        this.outline = outline;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
