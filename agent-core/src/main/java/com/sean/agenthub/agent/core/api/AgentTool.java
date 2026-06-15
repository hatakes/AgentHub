package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;

/**
 * 可被 Agent 调用的业务能力插件。
 *
 * <p>AgentHub 不让模型直接调用任意业务接口，而是要求业务能力先封装成 AgentTool。
 * Tool 提供稳定名称、自然语言说明、参数 Schema、风险等级和执行逻辑。模型只能在 Runtime 下发的
 * 已注册 Tool 中选择；真正执行前还会经过参数校验、权限校验和审计记录。</p>
 *
 * <p>这个接口是企业落地的关键边界：业务系统可以把数据库查询、文件解析、外部 HTTP API 等能力
 * 注册成 Tool，但 core 不需要知道这些能力背后的业务细节。</p>
 *
 * @author Sean
 */
public interface AgentTool {
    /**
     * Tool 唯一名称，供模型和注册中心定位。
     *
     * <p>名称需要稳定且语义明确，因为模型返回 ToolCall 时会用这个名字匹配 ToolRegistry。</p>
     */
    String name();

    /**
     * Tool 能力说明，会提供给模型用于判断是否需要调用。
     *
     * <p>说明应该写清触发条件和不应触发的场景，减少模型在闲聊或解释类问题中误调用工具。</p>
     */
    String description();

    /**
     * Tool 入参结构，第一版使用 JSON Schema 子集表达。
     *
     * <p>Runtime 会根据 required 参数做基础校验，避免缺失必填参数时进入业务代码。</p>
     */
    ToolSchema schema();

    /**
     * Tool 风险等级，MVP 阶段只允许 READ。
     *
     * <p>当前平台先验证只读链路，避免模型误触发写操作。后续开放 WRITE / APPROVAL 等能力时，
     * 应在 Runtime、PermissionEngine 和审计层同时增强。</p>
     */
    ToolRiskLevel riskLevel();

    /**
     * 执行业务能力。
     *
     * <p>execute 只应该包含业务执行逻辑。权限判断、必填参数校验和审计由 Runtime 统一处理，
     * 这样不同 Tool 的横切规则保持一致。</p>
     *
     * @param context 用户上下文和模型生成的 Tool 参数
     * @return Tool 执行结果
     */
    ToolResult execute(ToolContext context);
}
