package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.ToolCall;

/**
 * 模型流式输出回调。
 *
 * @author Sean
 */
public interface ModelStreamListener {
    /**
     * 接收模型文本增量。
     *
     * @param delta 文本片段
     */
    void onDelta(String delta);

    /**
     * 接收模型流式返回的 ToolCall。
     *
     * <p>默认空实现用于保持旧 ModelStreamListener 实现兼容。</p>
     *
     * @param toolCall Tool 调用请求
     */
    default void onToolCall(ToolCall toolCall) {
    }

    /**
     * 模型流式输出完成。
     */
    void onComplete();

    /**
     * 模型流式输出异常。
     *
     * @param error 异常信息
     */
    void onError(String error);
}
