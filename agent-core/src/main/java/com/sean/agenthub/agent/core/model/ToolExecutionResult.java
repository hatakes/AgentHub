package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.tool.ToolResult;

/**
 * 一次 Tool 调用及其执行结果，用于模型总结阶段回传上下文。
 *
 * @author Sean
 */
public class ToolExecutionResult {
    private ToolCall toolCall;
    private ToolResult toolResult;

    public ToolExecutionResult() {
    }

    public ToolExecutionResult(ToolCall toolCall, ToolResult toolResult) {
        this.toolCall = toolCall;
        this.toolResult = toolResult;
    }

    public ToolCall getToolCall() {
        return toolCall;
    }

    public void setToolCall(ToolCall toolCall) {
        this.toolCall = toolCall;
    }

    public ToolResult getToolResult() {
        return toolResult;
    }

    public void setToolResult(ToolResult toolResult) {
        this.toolResult = toolResult;
    }
}
