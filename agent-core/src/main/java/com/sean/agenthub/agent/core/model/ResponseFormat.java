package com.sean.agenthub.agent.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型响应格式约束，用于 Structured Output。
 *
 * <p>当需要模型返回符合特定 JSON Schema 的结构化数据时使用。
 * 通过 {@link #jsonSchema(String, Map)} 创建 JSON Schema 格式约束。</p>
 *
 * @author Sean
 */
public class ResponseFormat {
    /** 响应格式类型，如 "json_schema"。 */
    private String type;
    /** JSON Schema 名称。 */
    private String name;
    /** JSON Schema 定义。 */
    private Map<String, Object> schema;
    /** 是否启用严格模式，确保输出完全符合 Schema。 */
    private Boolean strict;

    /**
     * 创建 JSON Schema 格式约束。
     *
     * @param name   Schema 名称
     * @param schema Schema 定义
     * @return 响应格式约束
     */
    public static ResponseFormat jsonSchema(String name, Map<String, Object> schema) {
        ResponseFormat format = new ResponseFormat();
        format.setType("json_schema");
        format.setName(name);
        format.setSchema(schema);
        format.setStrict(true);
        return format;
    }

    /**
     * 获取格式类型。
     *
     * @return 格式类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置格式类型。
     *
     * @param type 格式类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取 Schema 名称。
     *
     * @return Schema 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置 Schema 名称。
     *
     * @param name Schema 名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取 Schema 定义。
     *
     * @return Schema 定义
     */
    public Map<String, Object> getSchema() {
        return schema;
    }

    /**
     * 设置 Schema 定义。
     *
     * @param schema Schema 定义
     */
    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    /**
     * 获取严格模式标志。
     *
     * @return 严格模式标志
     */
    public Boolean getStrict() {
        return strict;
    }

    /**
     * 设置严格模式标志。
     *
     * @param strict 严格模式标志
     */
    public void setStrict(Boolean strict) {
        this.strict = strict;
    }
}
