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
 * @author Sean
 */
public class ToolSchema {
    private String type = "object";
    private Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
    private List<String> required = new ArrayList<String>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, ToolSchemaProperty> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, ToolSchemaProperty> properties) {
        this.properties = properties == null ? new LinkedHashMap<String, ToolSchemaProperty>() : properties;
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required == null ? new ArrayList<String>() : required;
    }
}
