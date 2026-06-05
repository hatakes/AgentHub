package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.PermissionResult;
import com.sean.agenthub.agent.core.model.UserContext;
import com.sean.agenthub.agent.core.tool.ToolContext;

/**
 * Tool 调用权限检查接口。
 *
 * @author Sean
 */
public interface PermissionEngine {
    /**
     * 判断当前用户是否允许调用指定 Tool。
     *
     * @param user 当前用户
     * @param tool 即将调用的 Tool
     * @param context Tool 参数和用户上下文
     * @return 权限检查结果
     */
    PermissionResult check(UserContext user, AgentTool tool, ToolContext context);
}
