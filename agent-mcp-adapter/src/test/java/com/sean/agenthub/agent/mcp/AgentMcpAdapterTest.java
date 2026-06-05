package com.sean.agenthub.agent.mcp;

import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.AuditService;
import com.sean.agenthub.agent.core.api.PermissionEngine;
import com.sean.agenthub.agent.core.model.AuditEvent;
import com.sean.agenthub.agent.core.model.PermissionResult;
import com.sean.agenthub.agent.core.model.UserContext;
import com.sean.agenthub.agent.core.tool.InMemoryToolRegistry;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.core.tool.ToolSchemaProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * AgentMcpAdapter 单元测试。
 *
 * @author Sean
 */
public class AgentMcpAdapterTest {
    @Test
    public void shouldListToolsWithInputSchema() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        registry.register(new SimpleTool("query_budget_balance", ToolRiskLevel.READ));
        RecordingAuditService auditService = new RecordingAuditService();
        AgentMcpAdapter adapter = new AgentMcpAdapter(registry, allowAll(), auditService);

        List<McpTool> tools = adapter.listTools();

        Assert.assertEquals(1, tools.size());
        Assert.assertEquals("query_budget_balance", tools.get(0).getName());
        Assert.assertTrue(tools.get(0).getDescription().contains("预算"));
        Assert.assertEquals("object", tools.get(0).getInputSchema().get("type"));
        Assert.assertTrue(String.valueOf(tools.get(0).getInputSchema()).contains("budgetCode"));
        Assert.assertTrue(auditService.events.isEmpty());
    }

    @Test
    public void shouldCallToolWithPermissionAndAudit() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        SimpleTool tool = new SimpleTool("query_budget_balance", ToolRiskLevel.READ);
        registry.register(tool);
        RecordingAuditService auditService = new RecordingAuditService();
        AgentMcpAdapter adapter = new AgentMcpAdapter(registry, allowAll(), auditService);
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("budgetCode", "BUDGET-001");

        McpToolCallResponse response = adapter.callTool(
                new McpToolCallRequest("query_budget_balance", arguments, "mcp-s001", "finance-admin"));

        Assert.assertTrue(response.isSuccess());
        Assert.assertTrue(String.valueOf(response.getResult()).contains("BUDGET-001"));
        Assert.assertEquals(1, tool.calls);
        Assert.assertEquals(1, auditService.events.size());
        Assert.assertEquals("mcp-s001", auditService.events.get(0).getSessionId());
        Assert.assertEquals("finance-admin", auditService.events.get(0).getUserId());
        Assert.assertEquals("query_budget_balance", auditService.events.get(0).getToolName());
        Assert.assertTrue(auditService.events.get(0).isSuccess());
    }

    @Test
    public void shouldDenyBeforeToolExecution() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        SimpleTool tool = new SimpleTool("query_budget_balance", ToolRiskLevel.READ);
        registry.register(tool);
        RecordingAuditService auditService = new RecordingAuditService();
        AgentMcpAdapter adapter = new AgentMcpAdapter(registry, denyAll(), auditService);
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("budgetCode", "BUDGET-001");

        McpToolCallResponse response = adapter.callTool(
                new McpToolCallRequest("query_budget_balance", arguments, "mcp-s002", "u001"));

        Assert.assertFalse(response.isSuccess());
        Assert.assertTrue(response.getErrorMessage().contains("Tool permission denied"));
        Assert.assertEquals(0, tool.calls);
        Assert.assertEquals(1, auditService.events.size());
        Assert.assertFalse(auditService.events.get(0).isSuccess());
    }

    @Test
    public void shouldValidateRequiredArgumentsBeforePermission() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        SimpleTool tool = new SimpleTool("query_budget_balance", ToolRiskLevel.READ);
        registry.register(tool);
        RecordingAuditService auditService = new RecordingAuditService();
        CountingPermissionEngine permissionEngine = new CountingPermissionEngine();
        AgentMcpAdapter adapter = new AgentMcpAdapter(registry, permissionEngine, auditService);

        McpToolCallResponse response = adapter.callTool(
                new McpToolCallRequest("query_budget_balance", new HashMap<String, Object>(), "mcp-s003", "u001"));

        Assert.assertFalse(response.isSuccess());
        Assert.assertTrue(response.getErrorMessage().contains("Missing required tool argument: budgetCode"));
        Assert.assertEquals(0, permissionEngine.calls);
        Assert.assertEquals(0, tool.calls);
        Assert.assertEquals(1, auditService.events.size());
    }

    @Test
    public void shouldRejectWriteTool() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        SimpleTool tool = new SimpleTool("update_budget_balance", ToolRiskLevel.WRITE);
        registry.register(tool);
        RecordingAuditService auditService = new RecordingAuditService();
        AgentMcpAdapter adapter = new AgentMcpAdapter(registry, allowAll(), auditService);
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("budgetCode", "BUDGET-001");

        McpToolCallResponse response = adapter.callTool(
                new McpToolCallRequest("update_budget_balance", arguments, "mcp-s004", "finance-admin"));

        Assert.assertFalse(response.isSuccess());
        Assert.assertTrue(response.getErrorMessage().contains("Only READ tools"));
        Assert.assertEquals(0, tool.calls);
        Assert.assertEquals(1, auditService.events.size());
    }

    private static PermissionEngine allowAll() {
        return new PermissionEngine() {
            @Override
            public PermissionResult check(UserContext user, AgentTool tool, ToolContext context) {
                return PermissionResult.allowed();
            }
        };
    }

    private static PermissionEngine denyAll() {
        return new PermissionEngine() {
            @Override
            public PermissionResult check(UserContext user, AgentTool tool, ToolContext context) {
                return PermissionResult.denied("No MCP permission");
            }
        };
    }

    private static class CountingPermissionEngine implements PermissionEngine {
        private int calls;

        @Override
        public PermissionResult check(UserContext user, AgentTool tool, ToolContext context) {
            calls++;
            return PermissionResult.allowed();
        }
    }

    private static class RecordingAuditService implements AuditService {
        private final List<AuditEvent> events = new ArrayList<AuditEvent>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }
    }

    private static class SimpleTool implements AgentTool {
        private final String name;
        private final ToolRiskLevel riskLevel;
        private int calls;

        private SimpleTool(String name, ToolRiskLevel riskLevel) {
            this.name = name;
            this.riskLevel = riskLevel;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "查询预算余额";
        }

        @Override
        public ToolSchema schema() {
            ToolSchema schema = new ToolSchema();
            Map<String, ToolSchemaProperty> properties = new HashMap<String, ToolSchemaProperty>();
            properties.put("budgetCode", new ToolSchemaProperty("string", "预算编码"));
            schema.setProperties(properties);
            schema.setRequired(Arrays.asList("budgetCode"));
            return schema;
        }

        @Override
        public ToolRiskLevel riskLevel() {
            return riskLevel;
        }

        @Override
        public ToolResult execute(ToolContext context) {
            calls++;
            return ToolResult.success("{budgetCode=" + context.getArguments().get("budgetCode")
                    + ", availableAmount=128000.00}");
        }
    }
}
