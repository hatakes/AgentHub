package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型请求调用 Tool 时生成的调用描述。
 *
 * @author Sean
 */
public class ToolCall {
    private String id;
    private String name;
    private Map<String, Object> arguments = new HashMap<String, Object>();

    public ToolCall() {
    }

    public ToolCall(String name, Map<String, Object> arguments) {
        this.name = name;
        setArguments(arguments);
    }

    public ToolCall(String id, String name, Map<String, Object> arguments) {
        this.id = id;
        this.name = name;
        setArguments(arguments);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments == null ? new HashMap<String, Object>() : arguments;
    }
}
