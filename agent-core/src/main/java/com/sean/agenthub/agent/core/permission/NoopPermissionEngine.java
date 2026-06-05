package com.sean.agenthub.agent.core.permission;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 默认放行的权限实现。
 *
 * <p>业务系统接入时建议提供自己的 PermissionEngine Bean，复用原系统权限逻辑。</p>
 *
 * @author Sean
 */
public class NoopPermissionEngine implements PermissionEngine {
    @Override
    public PermissionResult check(UserContext user, AgentTool tool, ToolContext context) {
        return PermissionResult.allowed();
    }
}
