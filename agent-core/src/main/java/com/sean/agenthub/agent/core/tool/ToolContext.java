package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool 执行上下文，包含当前用户和模型生成的参数。
 *
 * <p>由 Runtime 在权限检查后构建，传递给 Tool.execute()。</p>
 *
 * @author Sean
 */
public class ToolContext {
    /** 当前请求的用户上下文。 */
    private UserContext user;
    /** 模型生成的 Tool 调用参数。 */
    private Map<String, Object> arguments = new HashMap<String, Object>();

    /** 创建空的 Tool 上下文。 */
    public ToolContext() {
    }

    /**
     * 创建包含用户和参数的 Tool 上下文。
     *
     * @param user      用户上下文
     * @param arguments Tool 参数
     */
    public ToolContext(UserContext user, Map<String, Object> arguments) {
        this.user = user;
        setArguments(arguments);
    }

    /**
     * 获取用户上下文。
     *
     * @return 用户上下文
     */
    public UserContext getUser() {
        return user;
    }

    /**
     * 设置用户上下文。
     *
     * @param user 用户上下文
     */
    public void setUser(UserContext user) {
        this.user = user;
    }

    /**
     * 获取 Tool 参数。
     *
     * @return 参数映射
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * 设置 Tool 参数，null 会被替换为空 Map。
     *
     * @param arguments 参数映射
     */
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments == null ? new HashMap<String, Object>() : arguments;
    }
}
