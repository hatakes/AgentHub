package com.sean.agenthub.agent.attachment.infrastructure;

import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.PermissionEngine;
import com.sean.agenthub.agent.core.model.PermissionResult;
import com.sean.agenthub.agent.core.model.UserContext;
import com.sean.agenthub.agent.core.tool.ToolContext;
import org.springframework.stereotype.Component;

/**
 * 文档处理能力权限实现。
 *
 * @author Sean
 */
@Component
public class AttachmentPermissionEngine implements PermissionEngine {
    @Override
    public PermissionResult check(UserContext user, AgentTool tool, ToolContext context) {
        if (tool.name() == null || !tool.name().endsWith("_attachment")
                && !tool.name().contains("document")
                && !"summarize_attachment_analysis".equals(tool.name())) {
            return PermissionResult.allowed();
        }
        if (user != null && "attachment-reviewer".equals(user.getUserId())) {
            return PermissionResult.allowed();
        }
        return PermissionResult.denied("Only attachment-reviewer can analyze attachments");
    }
}
