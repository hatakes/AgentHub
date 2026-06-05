package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.ToolCallResult;

/**
 * Agent 流式输出回调。
 *
 * @author Sean
 */
public interface AgentStreamListener {
    /**
     * 接收最终回答文本增量。
     *
     * @param delta 文本片段
     */
    void onDelta(String delta);

    /**
     * 接收 Tool 调用摘要。
     *
     * @param result Tool 调用结果
     */
    void onToolCall(ToolCallResult result);

    /**
     * Agent 流式输出完成。
     */
    void onComplete();

    /**
     * Agent 流式输出异常。
     *
     * @param error 异常信息
     */
    void onError(String error);
}
