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
 * 返回模拟字典项的只读 Tool 示例。
 *
 * @author Sean
 */
@Component
public class QueryDictItemTool implements AgentTool {
    @Override
    public String name() {
        return "query_dict_item";
    }

    @Override
    public String description() {
        return "查询业务字典项。仅当用户明确要求查询字典、字典编码、字典项时调用，不要在闲聊或介绍类问题中使用";
    }

    @Override
    public ToolSchema schema() {
        ToolSchema schema = new ToolSchema();
        Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
        properties.put("code", new ToolSchemaProperty("string", "字典编码"));
        schema.setProperties(properties);
        schema.setRequired(Arrays.asList("code"));
        return schema;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        return ToolResult.success("{code=status, items=[enabled, disabled]}");
    }
}
