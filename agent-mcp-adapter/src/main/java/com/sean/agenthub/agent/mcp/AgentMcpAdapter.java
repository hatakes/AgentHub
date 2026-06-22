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
 * <p>MCP 调用和 AgentRuntime 调用共享同一套 ToolRegistry、PermissionEngine 和 AuditService。
 * 这样无论 Tool 是被模型通过 Agent 对话触发，还是被 MCP 客户端直接触发，都能经过一致的只读限制、
 * 权限判断和审计记录。</p>
 *
 * @author Sean
 */
public class AgentMcpAdapter {
    /** Tool 注册中心，提供已注册 Tool 的查找能力。 */
    private final ToolRegistry toolRegistry;
    /** 权限检查引擎。 */
    private final PermissionEngine permissionEngine;
    /** 审计记录服务。 */
    private final AuditService auditService;

    /**
     * 创建 MCP 适配器。
     *
     * @param toolRegistry     Tool 注册中心
     * @param permissionEngine 权限引擎
     * @param auditService     审计服务
     */
    public AgentMcpAdapter(ToolRegistry toolRegistry,
                           PermissionEngine permissionEngine,
                           AuditService auditService) {
        this.toolRegistry = toolRegistry;
        this.permissionEngine = permissionEngine;
        this.auditService = auditService;
    }

    /**
     * 列出所有已注册的 Tool（MCP tools/list）。
     *
     * @return MCP Tool 列表
     */
    public List<McpTool> listTools() {
        List<McpTool> result = new ArrayList<McpTool>();
        for (AgentTool tool : toolRegistry.list()) {
            result.add(toMcpTool(tool));
        }
        return result;
    }

    /**
     * 执行 Tool 调用（MCP tools/call）。
     *
     * <p>与 AgentRuntime 共享同一套安全边界：ToolRegistry 查找、READ 限制、参数校验、权限检查和审计。</p>
     *
     * @param request MCP Tool 调用请求
     * @return MCP Tool 调用响应
     */
    public McpToolCallResponse callTool(McpToolCallRequest request) {
        long startedAt = System.currentTimeMillis();
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setTraceId(UUID.randomUUID().toString());
        auditEvent.setSessionId(request.getSessionId());
        auditEvent.setUserId(request.getUserId());
        auditEvent.setToolName(request.getName());
        auditEvent.setRequestSummary(String.valueOf(request.getArguments()));

        try {
            // MCP 客户端只能调用 ToolRegistry 中已经注册的 Tool，不能通过任意名称触发业务代码。
            Optional<AgentTool> optionalTool = toolRegistry.get(request.getName());
            if (!optionalTool.isPresent()) {
                throw new IllegalArgumentException("Tool not found: " + request.getName());
            }

            AgentTool tool = optionalTool.get();
            // MVP 阶段保持与 AgentRuntime 一致，只开放 READ Tool。
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

    /**
     * 将 AgentTool 转换为 MCP Tool DTO。
     *
     * @param tool AgentTool
     * @return MCP Tool
     */
    private McpTool toMcpTool(AgentTool tool) {
        return new McpTool(tool.name(), tool.description(), toInputSchema(tool.schema()));
    }

    /**
     * 将 AgentHub ToolSchema 转换为 MCP inputSchema 格式。
     *
     * @param schema AgentHub ToolSchema
     * @return MCP inputSchema
     */
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
        // AgentHub 的 ToolSchema 是 JSON Schema 子集，这里转换成 MCP tools/list 所需的 inputSchema。
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

    /**
     * 校验 Tool 风险等级，MVP 只允许 READ。
     *
     * @param tool 待校验的 Tool
     * @throws IllegalStateException 如果 Tool 不是 READ 级别
     */
    private void validateReadOnly(AgentTool tool) {
        if (tool.riskLevel() != ToolRiskLevel.READ) {
            throw new IllegalStateException("Only READ tools are enabled for MCP adapter MVP: " + tool.name());
        }
    }

    /**
     * 校验 Tool 必填参数。
     *
     * @param schema    Tool 参数 Schema
     * @param arguments 调用参数
     * @throws IllegalArgumentException 如果缺少必填参数
     */
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
