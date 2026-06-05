package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.AgentContext;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;

/**
 * Agent 执行链路抽象。
 *
 * @author Sean
 */
public interface AgentRuntime {
    /**
     * 根据请求和上下文驱动模型调用、Tool 调用、权限校验和审计记录。
     *
     * @param request 用户请求
     * @param context 当前执行上下文
     * @return 执行结果
     */
    AgentResponse run(AgentRequest request, AgentContext context);

    /**
     * 根据请求和上下文驱动一次流式 Agent 对话。
     *
     * @param request 用户请求
     * @param context 当前执行上下文
     * @param listener 流式输出回调
     */
    void runStream(AgentRequest request, AgentContext context, AgentStreamListener listener);
}
