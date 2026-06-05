package com.sean.agenthub.agent.example;

import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.PermissionEngine;
import com.sean.agenthub.agent.core.model.PermissionResult;
import com.sean.agenthub.agent.core.model.UserContext;
import com.sean.agenthub.agent.core.tool.ToolContext;
import org.springframework.stereotype.Component;

/**
 * 示例应用权限实现，用于验证 Tool 调用前置权限边界。
 *
 * @author Sean
 */
@Component
public class ExamplePermissionEngine implements PermissionEngine {
    @Override
    public PermissionResult check(UserContext user, AgentTool tool, ToolContext context) {
        if (!"query_budget_balance".equals(tool.name())) {
            return PermissionResult.allowed();
        }
        if (user != null && "finance-admin".equals(user.getUserId())) {
            return PermissionResult.allowed();
        }
        return PermissionResult.denied("Only finance-admin can query budget balance");
    }
}
