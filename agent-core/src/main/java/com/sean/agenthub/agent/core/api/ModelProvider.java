package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;

import java.util.EnumSet;
import java.util.Set;

/**
 * 模型供应商适配接口，是 agent-core 和具体大模型之间的隔离层。
 *
 * <p>AgentRuntime 必须调用模型来完成回答或产生 ToolCall，但 core 不能直接依赖 DeepSeek、MiMo、
 * OpenAI、Anthropic、Spring AI 或 LangChain4j 的协议细节。否则新增模型、切换模型或写单元测试时，
 * 都会被迫修改 Runtime。这个接口把 core 的依赖收敛成统一的 ModelRequest / ModelResponse 契约，
 * 具体 HTTP 请求、鉴权、tool_call 字段解析和流式协议差异都放到 provider 实现里处理。</p>
 *
 * <p>业务系统也可以直接声明自己的 ModelProvider Bean，例如规则型 mock provider，用来稳定返回
 * ToolCall 验证业务链路。Starter 的默认 provider 使用 {@code @ConditionalOnMissingBean}，因此业务
 * 自定义实现会优先生效。</p>
 *
 * @author Sean
 */
public interface ModelProvider {
    /**
     * 声明当前模型适配器已覆盖的能力。
     *
     * <p>不同厂商或框架对 ToolCall、流式 ToolCall、Structured Output 的支持并不一致。
     * 用能力声明可以让上层在验收和诊断时明确知道当前 provider 的真实边界，避免仅凭配置名误判能力。</p>
     *
     * @return 能力集合
     */
    default Set<ModelProviderCapability> capabilities() {
        return EnumSet.of(ModelProviderCapability.TEXT_CHAT);
    }

    /**
     * 调用模型完成一次非流式对话。
     *
     * <p>返回值可能是自然语言答案，也可能是模型要求 Runtime 执行的 ToolCall。
     * Runtime 不关心具体模型协议，只根据 ModelResponse 判断下一步是直接返回，还是进入 Tool 执行链路。</p>
     *
     * @param request 模型请求，包含消息历史、可用 Tool 和上一次 Tool 结果
     * @return 模型文本回答或 Tool 调用请求
     */
    ModelResponse chat(ModelRequest request);

    /**
     * 调用模型完成一次流式文本对话。
     *
     * <p>默认实现会退化为非流式调用，便于旧实现保持兼容。这样早期 provider 只实现 chat 也可以接入
     * AgentHub，等需要真实流式体验时再覆盖 streamChat。</p>
     *
     * @param request 模型请求
     * @param listener 流式输出回调
     */
    default void streamChat(ModelRequest request, ModelStreamListener listener) {
        try {
            ModelResponse response = chat(request);
            if (response.hasToolCalls()) {
                for (ToolCall toolCall : response.getToolCalls()) {
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
