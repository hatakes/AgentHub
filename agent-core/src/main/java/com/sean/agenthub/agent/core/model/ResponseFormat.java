package com.sean.agenthub.agent.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型响应格式约束，用于 Structured Output。
 *
 * <p>当需要模型返回符合特定 JSON Schema 的结构化数据时使用。</p>
 *
 * @author Sean
 */
public class ResponseFormat {
    private String type;
    private String name;
    private Map<String, Object> schema;
    private Boolean strict;

    public static ResponseFormat jsonSchema(String name, Map<String, Object> schema) {
        ResponseFormat format = new ResponseFormat();
        format.setType("json_schema");
        format.setName(name);
        format.setSchema(schema);
        format.setStrict(true);
        return format;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }
}
