package com.sean.agenthub.agent.core.runtime;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * AgentService 默认实现，将请求转换为运行时上下文后交给 AgentRuntime。
 *
 * @author Sean
 */
public class DefaultAgentService implements AgentService {
    private final AgentRuntime runtime;

    public DefaultAgentService(AgentRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public AgentResponse chat(AgentRequest request) {
        UserContext user = new UserContext(request.getUserId());
        user.setAttributes(request.getAttributes());
        return runtime.run(request, new AgentContext(user));
    }

    @Override
    public void streamChat(AgentRequest request, AgentStreamListener listener) {
        UserContext user = new UserContext(request.getUserId());
        user.setAttributes(request.getAttributes());
        runtime.runStream(request, new AgentContext(user), listener);
    }
}
