package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.AgentTool;
import org.junit.Assert;
import org.junit.Test;

/**
 * InMemoryToolRegistry 单元测试。
 *
 * @author Sean
 */
public class InMemoryToolRegistryTest {

    @Test
    public void shouldRegisterAndFindToolByName() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        AgentTool tool = new SimpleTool("query_user");

        registry.register(tool);

        Assert.assertEquals(1, registry.list().size());
        Assert.assertTrue(registry.get("query_user").isPresent());
        Assert.assertSame(tool, registry.get("query_user").get());
    }

    @Test
    public void shouldIgnoreNullTool() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();

        registry.register(null);

        Assert.assertTrue(registry.list().isEmpty());
    }

    private static class SimpleTool implements AgentTool {
        private final String name;

        private SimpleTool(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "测试 Tool";
        }

        @Override
        public ToolSchema schema() {
            return new ToolSchema();
        }

        @Override
        public ToolRiskLevel riskLevel() {
            return ToolRiskLevel.READ;
        }

        @Override
        public ToolResult execute(ToolContext context) {
            return ToolResult.success("ok");
        }
    }
}
