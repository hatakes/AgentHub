package com.sean.agenthub.agent.core.runtime;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * AgentService 默认实现，将请求转换为运行时上下文后交给 AgentRuntime。
 *
 * <p>从 AgentRequest 中提取 userId 和 attributes 构建 UserContext，
 * 再组装为 AgentContext 传给 Runtime。上层（如 Controller）只面对 AgentService 接口。</p>
 *
 * @author Sean
 */
public class DefaultAgentService implements AgentService {
    /** Agent 执行器，负责编排模型调用和 Tool 执行。 */
    private final AgentRuntime runtime;

    /**
     * 创建默认 Agent 服务。
     *
     * @param runtime Agent 执行器
     */
    public DefaultAgentService(AgentRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * 执行一次非流式 Agent 对话。
     *
     * <p>从 request 中提取用户信息构建上下文，委托给 runtime.run()。</p>
     *
     * @param request 用户请求
     * @return Agent 响应
     */
    @Override
    public AgentResponse chat(AgentRequest request) {
        UserContext user = new UserContext(request.getUserId());
        user.setAttributes(request.getAttributes());
        return runtime.run(request, new AgentContext(user));
    }

    /**
     * 执行一次流式 Agent 对话。
     *
     * <p>从 request 中提取用户信息构建上下文，委托给 runtime.runStream()。</p>
     *
     * @param request  用户请求
     * @param listener 流式输出回调
     */
    @Override
    public void streamChat(AgentRequest request, AgentStreamListener listener) {
        UserContext user = new UserContext(request.getUserId());
        user.setAttributes(request.getAttributes());
        runtime.runStream(request, new AgentContext(user), listener);
    }
}
