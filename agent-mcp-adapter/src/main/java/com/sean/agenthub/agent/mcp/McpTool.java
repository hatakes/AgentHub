package com.sean.agenthub.agent.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP Tool 最小 DTO。
 *
 * @author Sean
 */
public class McpTool {
    private String name;
    private String description;
    private Map<String, Object> inputSchema = new LinkedHashMap<String, Object>();

    public McpTool() {
    }

    public McpTool(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        setInputSchema(inputSchema);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Map<String, Object> inputSchema) {
        this.inputSchema = inputSchema == null ? new LinkedHashMap<String, Object>() : inputSchema;
    }
}
