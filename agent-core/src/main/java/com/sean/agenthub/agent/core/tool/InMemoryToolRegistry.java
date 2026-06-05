package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于内存 Map 的 Tool 注册中心。
 *
 * <p>Starter 会把 Spring 容器里的 AgentTool Bean 自动注册到这里。</p>
 *
 * @author Sean
 */
public class InMemoryToolRegistry implements ToolRegistry {
    private final Map<String, AgentTool> tools = new LinkedHashMap<String, AgentTool>();

    @Override
    public List<AgentTool> list() {
        return new ArrayList<AgentTool>(tools.values());
    }

    @Override
    public Optional<AgentTool> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    @Override
    public void register(AgentTool tool) {
        if (tool == null) {
            return;
        }
        tools.put(tool.name(), tool);
    }
}
