package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 一次 Agent 执行过程中的上下文对象。
 *
 * @author Sean
 */
public class AgentContext {
    private UserContext user;

    public AgentContext() {
    }

    public AgentContext(UserContext user) {
        this.user = user;
    }

    public UserContext getUser() {
        return user;
    }

    public void setUser(UserContext user) {
        this.user = user;
    }
}
