package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;

/**
 * 可被 Agent 调用的业务能力插件。
 *
 * @author Sean
 */
public interface AgentTool {
    /**
     * Tool 唯一名称，供模型和注册中心定位。
     */
    String name();

    /**
     * Tool 能力说明，会提供给模型用于判断是否需要调用。
     */
    String description();

    /**
     * Tool 入参结构，第一版使用 JSON Schema 子集表达。
     */
    ToolSchema schema();

    /**
     * Tool 风险等级，MVP 阶段只允许 READ。
     */
    ToolRiskLevel riskLevel();

    /**
     * 执行业务能力。
     *
     * @param context 用户上下文和模型生成的 Tool 参数
     * @return Tool 执行结果
     */
    ToolResult execute(ToolContext context);
}
