package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 返回给调用方的 Tool 调用摘要。
 *
 * <p>包含 Tool 名称、执行状态和错误信息，用于 AgentResponse.toolCalls 列表。</p>
 *
 * @author Sean
 */
public class ToolCallResult {
    /** Tool 名称。 */
    private String tool;
    /** 执行是否成功。 */
    private boolean success;
    /** 错误信息，仅在 success=false 时有值。 */
    private String errorMessage;

    /** 创建空的 ToolCallResult。 */
    public ToolCallResult() {
    }

    /**
     * 创建完整的 ToolCallResult。
     *
     * @param tool         Tool 名称
     * @param success      是否成功
     * @param errorMessage 错误信息
     */
    public ToolCallResult(String tool, boolean success, String errorMessage) {
        this.tool = tool;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * 获取 Tool 名称。
     *
     * @return Tool 名称
     */
    public String getTool() {
        return tool;
    }

    /**
     * 设置 Tool 名称。
     *
     * @param tool Tool 名称
     */
    public void setTool(String tool) {
        this.tool = tool;
    }

    /**
     * 判断是否成功。
     *
     * @return 成功返回 true
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置执行状态。
     *
     * @param success 是否成功
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息，成功时为 null
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
