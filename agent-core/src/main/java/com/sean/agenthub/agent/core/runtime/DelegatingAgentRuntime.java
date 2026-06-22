package com.sean.agenthub.agent.core.runtime;

import com.sean.agenthub.agent.core.api.AgentRuntime;
import com.sean.agenthub.agent.core.api.AgentStreamListener;
import com.sean.agenthub.agent.core.model.AgentContext;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;

/**
 * AgentRuntime 包装器基类。
 *
 * <p>当业务只需要在默认执行链路前后追加日志、指标、Trace、限流或异常转换时，可以继承该类并委托给
 * DefaultAgentRuntime，而不必复制默认编排逻辑。</p>
 *
 * @author Sean
 */
public class DelegatingAgentRuntime implements AgentRuntime {
    /** 被委托的实际执行器。 */
    private final AgentRuntime delegate;

    /**
     * 创建委托执行器。
     *
     * @param delegate 实际执行器，不可为 null
     * @throws IllegalArgumentException 如果 delegate 为 null
     */
    public DelegatingAgentRuntime(AgentRuntime delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate AgentRuntime must not be null");
        }
        this.delegate = delegate;
    }

    /**
     * 获取被委托的实际执行器，供子类使用。
     *
     * @return 实际执行器
     */
    protected AgentRuntime getDelegate() {
        return delegate;
    }

    /**
     * 委托非流式执行给实际执行器。
     *
     * @param request 用户请求
     * @param context 执行上下文
     * @return Agent 响应
     */
    @Override
    public AgentResponse run(AgentRequest request, AgentContext context) {
        return delegate.run(request, context);
    }

    /**
     * 委托流式执行给实际执行器。
     *
     * @param request  用户请求
     * @param context  执行上下文
     * @param listener 流式输出回调
     */
    @Override
    public void runStream(AgentRequest request, AgentContext context, AgentStreamListener listener) {
        delegate.runStream(request, context, listener);
    }
}
