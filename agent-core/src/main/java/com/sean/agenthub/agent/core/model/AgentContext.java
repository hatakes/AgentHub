package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 一次 Agent 执行过程中的上下文对象。
 *
 * <p>当前只包含用户上下文，后续可扩展请求来源、租户等信息。</p>
 *
 * @author Sean
 */
public class AgentContext {
    /** 当前请求的用户上下文，用于权限检查和 Tool 执行。 */
    private UserContext user;

    /** 创建空的执行上下文。 */
    public AgentContext() {
    }

    /**
     * 创建包含用户上下文的执行上下文。
     *
     * @param user 用户上下文
     */
    public AgentContext(UserContext user) {
        this.user = user;
    }

    /**
     * 获取当前用户上下文。
     *
     * @return 用户上下文
     */
    public UserContext getUser() {
        return user;
    }

    /**
     * 设置当前用户上下文。
     *
     * @param user 用户上下文
     */
    public void setUser(UserContext user) {
        this.user = user;
    }
}
