package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;

/**
 * Agent 统一服务入口。
 *
 * @author Sean
 */
public interface AgentService {
    /**
     * 执行一次非流式 Agent 对话。
     *
     * @param request 用户请求和会话信息
     * @return Agent 回答、错误信息和 Tool 调用摘要
     */
    AgentResponse chat(AgentRequest request);

    /**
     * 执行一次流式 Agent 对话。
     *
     * @param request 用户请求和会话信息
     * @param listener 流式输出回调
     */
    void streamChat(AgentRequest request, AgentStreamListener listener);
}
