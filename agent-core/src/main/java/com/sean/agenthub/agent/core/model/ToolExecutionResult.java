package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.tool.ToolResult;

/**
 * 一次 Tool 调用及其执行结果，用于模型总结阶段回传上下文。
 *
 * <p>Runtime 在 Tool 执行完成后创建此对象，附加到 ModelRequest.lastToolExecutions，
 * 由 provider 转换为 tool_result 消息回传给模型。</p>
 *
 * @author Sean
 */
public class ToolExecutionResult {
    /** 模型发起的 Tool 调用描述。 */
    private ToolCall toolCall;
    /** Tool 实际执行结果。 */
    private ToolResult toolResult;

    /** 创建空的 ToolExecutionResult。 */
    public ToolExecutionResult() {
    }

    /**
     * 创建完整的 ToolExecutionResult。
     *
     * @param toolCall   Tool 调用描述
     * @param toolResult Tool 执行结果
     */
    public ToolExecutionResult(ToolCall toolCall, ToolResult toolResult) {
        this.toolCall = toolCall;
        this.toolResult = toolResult;
    }

    /**
     * 获取 Tool 调用描述。
     *
     * @return Tool 调用描述
     */
    public ToolCall getToolCall() {
        return toolCall;
    }

    /**
     * 设置 Tool 调用描述。
     *
     * @param toolCall Tool 调用描述
     */
    public void setToolCall(ToolCall toolCall) {
        this.toolCall = toolCall;
    }

    /**
     * 获取 Tool 执行结果。
     *
     * @return Tool 执行结果
     */
    public ToolResult getToolResult() {
        return toolResult;
    }

    /**
     * 设置 Tool 执行结果。
     *
     * @param toolResult Tool 执行结果
     */
    public void setToolResult(ToolResult toolResult) {
        this.toolResult = toolResult;
    }
}
