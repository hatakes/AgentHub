package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool Schema 中的单个字段定义。
 *
 * <p>描述参数的类型、用途和可选枚举值，由 ModelProvider 转换为对应协议的参数定义。</p>
 *
 * @author Sean
 */
public class ToolSchemaProperty {
    /** 参数类型，如 "string"、"number"、"boolean"。 */
    private String type;
    /** 参数用途描述，模型根据此描述决定何时传入该参数。 */
    private String description;
    /** 枚举值列表，限制参数的可选值。 */
    private List<String> enumValues = new ArrayList<String>();

    /** 创建空的属性定义。 */
    public ToolSchemaProperty() {
    }

    /**
     * 创建指定类型和描述的属性定义。
     *
     * @param type        参数类型
     * @param description 参数描述
     */
    public ToolSchemaProperty(String type, String description) {
        this.type = type;
        this.description = description;
    }

    /**
     * 获取参数类型。
     *
     * @return 参数类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置参数类型。
     *
     * @param type 参数类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取参数描述。
     *
     * @return 参数描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置参数描述。
     *
     * @param description 参数描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取枚举值列表。
     *
     * @return 枚举值列表
     */
    public List<String> getEnumValues() {
        return enumValues;
    }

    /**
     * 设置枚举值列表，null 会被替换为空列表。
     *
     * @param enumValues 枚举值列表
     */
    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues == null ? new ArrayList<String>() : enumValues;
    }
}
