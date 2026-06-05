package com.sean.agenthub.agent.example;

import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.core.tool.ToolSchemaProperty;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 返回模拟用户信息的只读 Tool 示例。
 *
 * @author Sean
 */
@Component
public class QueryUserInfoMockTool implements AgentTool {
    @Override
    public String name() {
        return "query_user_info_mock";
    }

    @Override
    public String description() {
        return "查询模拟用户信息。仅当用户明确要求查询用户信息、用户详情时调用，不要在闲聊或介绍类问题中使用";
    }

    @Override
    public ToolSchema schema() {
        ToolSchema schema = new ToolSchema();
        Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
        properties.put("userId", new ToolSchemaProperty("string", "用户 ID"));
        schema.setProperties(properties);
        schema.setRequired(Arrays.asList("userId"));
        return schema;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        Object userId = context.getArguments().get("userId");
        return ToolResult.success("{userId=" + userId + ", name=测试用户, department=AI 平台组}");
    }
}
