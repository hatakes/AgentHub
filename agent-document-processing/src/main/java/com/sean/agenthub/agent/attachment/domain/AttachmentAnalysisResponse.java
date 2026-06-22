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

    /** 附件 ID */
    private String attachmentId;

    /** 分析是否成功 */
    private boolean ok;

    /** 模型生成的文本回答 */
    private String answer;

    /** 结构化的分析结果 */
    private AttachmentAnalysisResult analysis;

    /** 错误信息，分析失败时填充 */
    private String errorMessage;

    /** 本次分析中执行的 Tool 调用记录 */
    private List<ToolCallResult> toolCalls = new ArrayList<ToolCallResult>();

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
     * 获取模型生成的文本回答。
     *
     * @return 文本回答
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * 设置模型生成的文本回答。
     *
     * @param answer 文本回答
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * 获取结构化的分析结果。
     *
     * @return 分析结果
     */
    public AttachmentAnalysisResult getAnalysis() {
        return analysis;
    }

    /**
     * 设置结构化的分析结果。
     *
     * @param analysis 分析结果
     */
    public void setAnalysis(AttachmentAnalysisResult analysis) {
        this.analysis = analysis;
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

    /**
     * 获取 Tool 调用记录。
     *
     * @return Tool 调用记录列表
     */
    public List<ToolCallResult> getToolCalls() {
        return toolCalls;
    }

    /**
     * 设置 Tool 调用记录。
     *
     * @param toolCalls Tool 调用记录列表，null 时使用空列表
     */
    public void setToolCalls(List<ToolCallResult> toolCalls) {
        this.toolCalls = toolCalls == null ? new ArrayList<ToolCallResult>() : toolCalls;
    }
}
