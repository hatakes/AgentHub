package com.sean.agenthub.agent.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP Tool 最小 DTO。
 *
 * <p>对应 MCP 协议中 tools/list 返回的单个 Tool 定义。</p>
 *
 * @author Sean
 */
public class McpTool {
    /** Tool 名称。 */
    private String name;
    /** Tool 功能描述。 */
    private String description;
    /** 输入参数 JSON Schema。 */
    private Map<String, Object> inputSchema = new LinkedHashMap<String, Object>();

    /** 创建空的 MCP Tool。 */
    public McpTool() {
    }

    /**
     * 创建完整的 MCP Tool。
     *
     * @param name        Tool 名称
     * @param description Tool 描述
     * @param inputSchema 输入参数 Schema
     */
    public McpTool(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        setInputSchema(inputSchema);
    }

    /**
     * 获取 Tool 名称。
     *
     * @return Tool 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置 Tool 名称。
     *
     * @param name Tool 名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取 Tool 描述。
     *
     * @return Tool 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置 Tool 描述。
     *
     * @param description Tool 描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取输入参数 Schema。
     *
     * @return 输入参数 Schema
     */
    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    /**
     * 设置输入参数 Schema，null 会被替换为空 Map。
     *
     * @param inputSchema 输入参数 Schema
     */
    public void setInputSchema(Map<String, Object> inputSchema) {
        this.inputSchema = inputSchema == null ? new LinkedHashMap<String, Object>() : inputSchema;
    }
}
