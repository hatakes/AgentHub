package com.sean.agenthub.agent.attachment.domain;

/**
 * 文档大纲提炼接口响应。
 *
 * @author Sean
 */
public class DocumentOutlineResponse {

    /** 附件 ID */
    private String attachmentId;

    /** 分析是否成功 */
    private boolean ok;

    /** 文档大纲提炼结果 */
    private DocumentOutlineResult outline;

    /** 错误信息，分析失败时填充 */
    private String errorMessage;

    /**
     * 获取附件 ID。
     *
     * @return 附件 ID
     */
    public String getAttachmentId() {
        return attachmentId;
    }

    /**
     * 设置附件 ID。
     *
     * @param attachmentId 附件 ID
     */
    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    /**
     * 获取分析是否成功。
     *
     * @return 是否成功
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * 设置分析是否成功。
     *
     * @param ok 是否成功
     */
    public void setOk(boolean ok) {
        this.ok = ok;
    }

    /**
     * 获取文档大纲提炼结果。
     *
     * @return 大纲提炼结果
     */
    public DocumentOutlineResult getOutline() {
        return outline;
    }

    /**
     * 设置文档大纲提炼结果。
     *
     * @param outline 大纲提炼结果
     */
    public void setOutline(DocumentOutlineResult outline) {
        this.outline = outline;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置错误信息。
     *
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
