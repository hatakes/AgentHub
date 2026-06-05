package com.sean.agenthub.agent.mcp;

import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.AuditService;
import com.sean.agenthub.agent.core.api.PermissionEngine;
import com.sean.agenthub.agent.core.api.ToolRegistry;
import com.sean.agenthub.agent.core.model.AuditEvent;
import com.sean.agenthub.agent.core.model.PermissionResult;
import com.sean.agenthub.agent.core.model.UserContext;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.core.tool.ToolSchemaProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AgentHub Tool 到 MCP Tool 的最小适配器。
 *
 * <p>该类不实现完整 MCP Server，只提供 tools/list 和 tools/call 所需的核心映射。</p>
 *
 * @author Sean
 */
public class AgentMcpAdapter {
    private final ToolRegistry toolRegistry;
    private final PermissionEngine permissionEngine;
    private final AuditService auditService;

    public AgentMcpAdapter(ToolRegistry toolRegistry,
                           PermissionEngine permissionEngine,
                           AuditService auditService) {
        this.toolRegistry = toolRegistry;
        this.permissionEngine = permissionEngine;
        this.auditService = auditService;
    }

    public List<McpTool> listTools() {
        List<McpTool> result = new ArrayList<McpTool>();
        for (AgentTool tool : toolRegistry.list()) {
            result.add(toMcpTool(tool));
        }
        return result;
    }

    public McpToolCallResponse callTool(McpToolCallRequest request) {
        long startedAt = System.currentTimeMillis();
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setTraceId(UUID.randomUUID().toString());
        auditEvent.setSessionId(request.getSessionId());
        auditEvent.setUserId(request.getUserId());
        auditEvent.setToolName(request.getName());
        auditEvent.setRequestSummary(String.valueOf(request.getArguments()));

        try {
            Optional<AgentTool> optionalTool = toolRegistry.get(request.getName());
            if (!optionalTool.isPresent()) {
                throw new IllegalArgumentException("Tool not found: " + request.getName());
            }

            AgentTool tool = optionalTool.get();
            validateReadOnly(tool);
            validateRequiredArguments(tool.schema(), request.getArguments());

            UserContext user = new UserContext(request.getUserId());
            user.setAttributes(request.getUserAttributes());
            ToolContext toolContext = new ToolContext(user, request.getArguments());
            PermissionResult permission = permissionEngine.check(user, tool, toolContext);
            if (!permission.isAllowed()) {
                throw new IllegalStateException("Tool permission denied: " + permission.getReason());
            }

            ToolResult toolResult = tool.execute(toolContext);
            auditEvent.setSuccess(toolResult.isSuccess());
            auditEvent.setToolResultSummary(String.valueOf(toolResult.getData()));
            auditEvent.setErrorMessage(toolResult.getErrorMessage());
            if (!toolResult.isSuccess()) {
                return McpToolCallResponse.error(toolResult.getErrorMessage());
            }
            return McpToolCallResponse.success(toolResult.getData());
        } catch (RuntimeException ex) {
            auditEvent.setSuccess(false);
            auditEvent.setErrorMessage(ex.getMessage());
            return McpToolCallResponse.error(ex.getMessage());
        } finally {
            auditEvent.setLatencyMs(System.currentTimeMillis() - startedAt);
            auditService.record(auditEvent);
        }
    }

    private McpTool toMcpTool(AgentTool tool) {
        return new McpTool(tool.name(), tool.description(), toInputSchema(tool.schema()));
    }

    private Map<String, Object> toInputSchema(ToolSchema schema) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (schema == null) {
            result.put("type", "object");
            result.put("properties", new LinkedHashMap<String, Object>());
            result.put("required", new ArrayList<String>());
            return result;
        }
        result.put("type", schema.getType());
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, ToolSchemaProperty> entry : schema.getProperties().entrySet()) {
            ToolSchemaProperty property = entry.getValue();
            Map<String, Object> propertySchema = new LinkedHashMap<String, Object>();
            propertySchema.put("type", property.getType());
            propertySchema.put("description", property.getDescription());
            if (!property.getEnumValues().isEmpty()) {
                propertySchema.put("enum", property.getEnumValues());
            }
            properties.put(entry.getKey(), propertySchema);
        }
        result.put("properties", properties);
        result.put("required", schema.getRequired());
        return result;
    }

    private void validateReadOnly(AgentTool tool) {
        if (tool.riskLevel() != ToolRiskLevel.READ) {
            throw new IllegalStateException("Only READ tools are enabled for MCP adapter MVP: " + tool.name());
        }
    }

    private void validateRequiredArguments(ToolSchema schema, Map<String, Object> arguments) {
        if (schema == null || schema.getRequired() == null) {
            return;
        }
        for (String key : schema.getRequired()) {
            if (arguments == null || !arguments.containsKey(key) || arguments.get(key) == null) {
                throw new IllegalArgumentException("Missing required tool argument: " + key);
            }
        }
    }
}
