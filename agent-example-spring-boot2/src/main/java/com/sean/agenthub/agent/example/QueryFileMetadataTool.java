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
 * 返回模拟文件元数据的只读 Tool 示例。
 *
 * @author Sean
 */
@Component
public class QueryFileMetadataTool implements AgentTool {
    @Override
    public String name() {
        return "query_file_metadata";
    }

    @Override
    public String description() {
        return "查询文件元数据。仅当用户明确要求查询文件信息、文件元数据、文件详情时调用，不要在闲聊或介绍类问题中使用";
    }

    @Override
    public ToolSchema schema() {
        ToolSchema schema = new ToolSchema();
        Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
        properties.put("fileId", new ToolSchemaProperty("string", "文件 ID"));
        schema.setProperties(properties);
        schema.setRequired(Arrays.asList("fileId"));
        return schema;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        Object fileId = context.getArguments().get("fileId");
        return ToolResult.success("{fileId=" + fileId + ", name=example.pdf, size=1024, owner=u001}");
    }
}
