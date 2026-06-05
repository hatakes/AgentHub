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
 * 查询预算余额的只读业务 Tool 示例。
 *
 * @author Sean
 */
@Component
public class QueryBudgetBalanceTool implements AgentTool {
    @Override
    public String name() {
        return "query_budget_balance";
    }

    @Override
    public String description() {
        return "查询预算余额。仅当用户明确要求查询预算余额、预算可用金额、预算结余时调用，不要在闲聊或介绍类问题中使用";
    }

    @Override
    public ToolSchema schema() {
        ToolSchema schema = new ToolSchema();
        Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
        properties.put("budgetCode", new ToolSchemaProperty("string", "预算编码"));
        schema.setProperties(properties);
        schema.setRequired(Arrays.asList("budgetCode"));
        return schema;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        Object budgetCode = context.getArguments().get("budgetCode");
        if ("BUDGET-FAIL".equals(String.valueOf(budgetCode))) {
            return ToolResult.error("Budget service unavailable");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("budgetCode", budgetCode);
        result.put("departmentName", "AI 平台组");
        result.put("availableAmount", "128000.00");
        result.put("currency", "CNY");
        result.put("ownerMasked", "Sean");
        return ToolResult.success(result);
    }
}
