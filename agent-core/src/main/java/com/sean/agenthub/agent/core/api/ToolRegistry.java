package com.sean.agenthub.agent.core.api;

import java.util.List;
import java.util.Optional;

/**
 * Tool 注册中心。
 *
 * @author Sean
 */
public interface ToolRegistry {
    /**
     * 返回当前可用的全部 Tool。
     */
    List<AgentTool> list();

    /**
     * 根据 Tool 名称查找注册项。
     */
    Optional<AgentTool> get(String name);

    /**
     * 注册一个 Tool。
     */
    void register(AgentTool tool);
}
