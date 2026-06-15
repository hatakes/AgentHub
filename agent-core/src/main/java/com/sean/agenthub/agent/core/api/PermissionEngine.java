package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.PermissionResult;
import com.sean.agenthub.agent.core.model.UserContext;
import com.sean.agenthub.agent.core.tool.ToolContext;

/**
 * Tool 调用权限检查接口。
 *
 * <p>权限检查放在 core 抽象里，是为了保证所有 Tool 调用都经过同一个安全入口。
 * 业务 Tool 本身不应该各自散落权限判断，否则很容易出现某些 Tool 漏校验、审计口径不一致的问题。</p>
 *
 * @author Sean
 */
public interface PermissionEngine {
    /**
     * 判断当前用户是否允许调用指定 Tool。
     *
     * <p>Runtime 会在参数校验之后、Tool.execute 之前调用该方法。实现可以基于用户、Tool 名称、
     * Tool 风险等级和参数内容做 RBAC、数据权限或租户隔离判断。</p>
     *
     * @param user 当前用户
     * @param tool 即将调用的 Tool
     * @param context Tool 参数和用户上下文
     * @return 权限检查结果
     */
    PermissionResult check(UserContext user, AgentTool tool, ToolContext context);
}
