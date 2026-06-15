package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.AgentContext;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;

/**
 * Agent 执行链路抽象。
 *
 * <p>AgentRuntime 是 AgentHub 的编排核心，负责把模型、Tool、权限、审计和记忆串成一次完整执行。
 * 它不应该包含具体业务规则，也不应该直接依赖某个模型厂商。业务能力通过 AgentTool 插入，
 * 模型能力通过 ModelProvider 插入，Runtime 只负责统一执行顺序和安全边界。</p>
 *
 * @author Sean
 */
public interface AgentRuntime {
    /**
     * 根据请求和上下文驱动模型调用、Tool 调用、权限校验和审计记录。
     *
     * <p>非流式入口适合普通 HTTP API、测试和后台任务。返回前会得到完整 AgentResponse。</p>
     *
     * @param request 用户请求
     * @param context 当前执行上下文
     * @return 执行结果
     */
    AgentResponse run(AgentRequest request, AgentContext context);

    /**
     * 根据请求和上下文驱动一次流式 Agent 对话。
     *
     * <p>流式入口复用同一套 Tool、权限和审计规则，只是通过 listener 逐步把模型输出和 ToolCall 状态
     * 推给调用方。</p>
     *
     * @param request 用户请求
     * @param context 当前执行上下文
     * @param listener 流式输出回调
     */
    void runStream(AgentRequest request, AgentContext context, AgentStreamListener listener);
}
