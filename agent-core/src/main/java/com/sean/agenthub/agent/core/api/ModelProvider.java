package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.ModelResponse;
import java.util.EnumSet;
import java.util.Set;

/**
 * 模型供应商适配接口。
 *
 * @author Sean
 */
public interface ModelProvider {
    /**
     * 声明当前模型适配器已覆盖的能力。
     *
     * @return 能力集合
     */
    default Set<ModelProviderCapability> capabilities() {
        return EnumSet.of(ModelProviderCapability.TEXT_CHAT);
    }

    /**
     * 调用模型完成一次非流式对话。
     *
     * @param request 模型请求，包含消息历史、可用 Tool 和上一次 Tool 结果
     * @return 模型文本回答或 Tool 调用请求
     */
    ModelResponse chat(ModelRequest request);

    /**
     * 调用模型完成一次流式文本对话。
     *
     * <p>默认实现会退化为非流式调用，便于旧实现保持兼容。</p>
     *
     * @param request 模型请求
     * @param listener 流式输出回调
     */
    default void streamChat(ModelRequest request, ModelStreamListener listener) {
        try {
            ModelResponse response = chat(request);
            if (response.hasToolCalls()) {
                for (com.sean.agenthub.agent.core.model.ToolCall toolCall : response.getToolCalls()) {
                    listener.onToolCall(toolCall);
                }
                listener.onComplete();
                return;
            }
            if (response.getAnswer() != null) {
                listener.onDelta(response.getAnswer());
            }
            listener.onComplete();
        } catch (RuntimeException ex) {
            listener.onError(ex.getMessage());
        }
    }
}
