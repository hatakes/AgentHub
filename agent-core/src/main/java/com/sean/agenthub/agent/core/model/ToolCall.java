package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型请求调用 Tool 时生成的调用描述。
 *
 * <p>包含 Tool 名称和参数，由 ModelProvider 解析模型响应后生成。
 * Runtime 通过 ToolRegistry 查找同名 Tool 并执行。</p>
 *
 * @author Sean
 */
public class ToolCall {
    /** Tool 调用 ID，由模型分配（部分模型可能为 null）。 */
    private String id;
    /** Tool 名称，用于 ToolRegistry 查找。 */
    private String name;
    /** Tool 调用参数，键值对形式。 */
    private Map<String, Object> arguments = new HashMap<String, Object>();

    /** 创建空的 ToolCall。 */
    public ToolCall() {
    }

    /**
     * 创建不含 ID 的 ToolCall。
     *
     * @param name      Tool 名称
     * @param arguments Tool 参数
     */
    public ToolCall(String name, Map<String, Object> arguments) {
        this.name = name;
        setArguments(arguments);
    }

    /**
     * 创建完整的 ToolCall。
     *
     * @param id        调用 ID
     * @param name      Tool 名称
     * @param arguments Tool 参数
     */
    public ToolCall(String id, String name, Map<String, Object> arguments) {
        this.id = id;
        this.name = name;
        setArguments(arguments);
    }

    /**
     * 获取调用 ID。
     *
     * @return 调用 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置调用 ID。
     *
     * @param id 调用 ID
     */
    public void setId(String id) {
        this.id = id;
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
     * 获取 Tool 参数。
     *
     * @return 参数映射
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * 设置 Tool 参数，null 会被替换为空 Map。
     *
     * @param arguments 参数映射
     */
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments == null ? new HashMap<String, Object>() : arguments;
    }
}
