package com.sean.agenthub.agent.core.api;

import java.util.List;
import java.util.Optional;

/**
 * Tool 注册中心。
 *
 * <p>ToolRegistry 是 Runtime 看到的可用能力清单。Spring Starter 会收集业务系统里的 AgentTool Bean
 * 并注册进来；模型只能看到 registry.list() 下发的 Tool，也只能通过 registry.get(name) 匹配到
 * 真实可执行 Tool。</p>
 *
 * @author Sean
 */
public interface ToolRegistry {
    /**
     * 返回当前可用的全部 Tool。
     *
     * <p>Runtime 会把这个列表放进 ModelRequest，让 provider 转成具体模型协议的 tools/functions。</p>
     */
    List<AgentTool> list();

    /**
     * 根据 Tool 名称查找注册项。
     *
     * <p>模型返回 ToolCall 后，Runtime 用名称在这里做受控查找。找不到的 ToolCall 会被拒绝执行。</p>
     */
    Optional<AgentTool> get(String name);

    /**
     * 注册一个 Tool。
     */
    void register(AgentTool tool);
}
