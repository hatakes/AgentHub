package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool 执行上下文，包含当前用户和模型生成的参数。
 *
 * @author Sean
 */
public class ToolContext {
    private UserContext user;
    private Map<String, Object> arguments = new HashMap<String, Object>();

    public ToolContext() {
    }

    public ToolContext(UserContext user, Map<String, Object> arguments) {
        this.user = user;
        setArguments(arguments);
    }

    public UserContext getUser() {
        return user;
    }

    public void setUser(UserContext user) {
        this.user = user;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments == null ? new HashMap<String, Object>() : arguments;
    }
}
