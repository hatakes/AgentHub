package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool 入参 Schema，第一版使用 JSON Schema 子集。
 *
 * <p>由 AgentTool.schema() 返回，Runtime 用于校验必填参数，
 * ModelProvider 转换为 OpenAI/Anthropic 的 Tool 定义格式。</p>
 *
 * @author Sean
 */
public class ToolSchema {
    /** Schema 类型，默认 "object"。 */
    private String type = "object";
    /** 参数属性定义，key 为参数名。 */
    private Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
    /** 必填参数名称列表。 */
    private List<String> required = new ArrayList<String>();

    /**
     * 获取 Schema 类型。
     *
     * @return Schema 类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置 Schema 类型。
     *
     * @param type Schema 类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取参数属性定义。
     *
     * @return 属性定义映射
     */
    public Map<String, ToolSchemaProperty> getProperties() {
        return properties;
    }

    /**
     * 设置参数属性定义，null 会被替换为空 Map。
     *
     * @param properties 属性定义映射
     */
    public void setProperties(Map<String, ToolSchemaProperty> properties) {
        this.properties = properties == null ? new LinkedHashMap<String, ToolSchemaProperty>() : properties;
    }

    /**
     * 获取必填参数名称列表。
     *
     * @return 必填参数列表
     */
    public List<String> getRequired() {
        return required;
    }

    /**
     * 设置必填参数名称列表，null 会被替换为空列表。
     *
     * @param required 必填参数列表
     */
    public void setRequired(List<String> required) {
        this.required = required == null ? new ArrayList<String>() : required;
    }
}
