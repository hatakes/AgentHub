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
    /** 以 Tool 名称为 key 的有序注册表，保持注册顺序。 */
    private final Map<String, AgentTool> tools = new LinkedHashMap<String, AgentTool>();

    /**
     * 列出所有已注册的 Tool。
     *
     * @return Tool 列表（按注册顺序）
     */
    @Override
    public List<AgentTool> list() {
        return new ArrayList<AgentTool>(tools.values());
    }

    /**
     * 按名称查找 Tool。
     *
     * @param name Tool 名称
     * @return 匹配的 Tool，不存在时返回空
     */
    @Override
    public Optional<AgentTool> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 注册一个 Tool，null 会被忽略。
     *
     * @param tool 待注册的 Tool
     */
    @Override
    public void register(AgentTool tool) {
        if (tool == null) {
            return;
        }
        tools.put(tool.name(), tool);
    }
}
