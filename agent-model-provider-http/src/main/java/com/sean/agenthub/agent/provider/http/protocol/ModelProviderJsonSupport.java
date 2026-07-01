package com.sean.agenthub.agent.provider.http.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.model.AgentMessage;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ResponseFormat;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.core.model.ToolExecutionResult;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.core.tool.ToolSchemaProperty;
import com.sean.agenthub.agent.core.tool.ToolResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ARGUMENTS;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ASSISTANT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.CALL_ID_PREFIX;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.CONTENT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.DESCRIPTION;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ENUM;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.FUNCTION;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ID;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.INPUT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.INPUT_SCHEMA;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.JSON_SCHEMA;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.NAME;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.OBJECT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.PARAMETERS;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.PROPERTIES;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.REQUIRED;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ROLE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.SCHEMA;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.STRICT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.SYSTEM;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_CALL_ID;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_CALLS;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_RESULT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_USE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_USE_ID;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TYPE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.USER;

/**
 * 模型协议 JSON 转换辅助方法。
 *
 * <p>包内实现细节，封装 OpenAI / Anthropic 协议载荷转换；不作为模块公开 API 暴露。</p>
 *
 * <p>这里的核心目标是保持 agent-core 模型对象稳定。OpenAI、Anthropic 对 system message、Tool 定义、
 * Tool 结果回填的 JSON 结构都不一样，但这些差异不应泄漏到 Runtime 或业务 Tool 里。</p>
 *
 * @author Sean
 */
public class ModelProviderJsonSupport {
    private final ObjectMapper objectMapper;

    public ModelProviderJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 把 ModelRequest 转换成 OpenAI messages 数组。
     *
     * <p>转换逻辑：
     * <ul>
     *   <li>system prompt 作为 system role 消息放在最前面</li>
     *   <li>历史消息按原样转换，role 保持不变</li>
     *   <li>如果历史消息中没有 user 消息，追加当前 userMessage</li>
     *   <li>如果有 Tool 执行结果，追加 assistant tool_calls 消息和 tool 角色结果消息</li>
     * </ul>
     *
     * @param request 模型请求
     * @return OpenAI messages 数组
     */
    public List<Map<String, Object>> toOpenAiMessages(ModelRequest request) {
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(ROLE, SYSTEM);
            item.put(CONTENT, request.getSystemPrompt());
            payload.add(item);
        }
        for (AgentMessage message : request.getMessages()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(ROLE, normalizeRole(message.getRole()));
            item.put(CONTENT, message.getContent());
            payload.add(item);
        }
        boolean hasUserMessage = false;
        for (AgentMessage message : request.getMessages()) {
            if (USER.equals(normalizeRole(message.getRole()))) {
                hasUserMessage = true;
                break;
            }
        }
        if (!hasUserMessage && request.getUserMessage() != null) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(ROLE, USER);
            item.put(CONTENT, request.getUserMessage());
            payload.add(item);
        }
        appendOpenAiToolMessages(payload, request);
        return payload;
    }

    /**
     * 把 ModelRequest 转换成 Anthropic messages 数组。
     *
     * <p>与 OpenAI 不同，Anthropic 的消息结构：
     * <ul>
     *   <li>system prompt 是顶层字段，不在 messages 里</li>
     *   <li>role 只有 user 和 assistant 两种</li>
     *   <li>Tool 结果作为 user 侧的 tool_result content block</li>
     * </ul>
     *
     * @param request 模型请求
     * @return Anthropic messages 数组
     */
    public List<Map<String, Object>> toAnthropicMessages(ModelRequest request) {
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>();
        for (AgentMessage message : request.getMessages()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(ROLE, ASSISTANT.equals(message.getRole()) ? ASSISTANT : USER);
            item.put(CONTENT, message.getContent());
            payload.add(item);
        }
        if (payload.isEmpty() && request.getUserMessage() != null) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(ROLE, USER);
            item.put(CONTENT, request.getUserMessage());
            payload.add(item);
        }
        appendAnthropicToolMessages(payload, request);
        return payload;
    }

    /**
     * 把 AgentTool 列表转换成 OpenAI tools 数组。
     *
     * <p>OpenAI 的 Tool 定义格式：
     * <pre>
     * {
     *   "type": "function",
     *   "function": {
     *     "name": "tool_name",
     *     "description": "tool description",
     *     "parameters": { ... }
     *   }
     * }
     * </pre>
     *
     * @param tools AgentTool 列表
     * @return OpenAI tools 数组
     */
    public List<Map<String, Object>> toOpenAiTools(List<AgentTool> tools) {
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>();
        for (AgentTool tool : tools) {
            Map<String, Object> function = new LinkedHashMap<String, Object>();
            function.put(NAME, tool.name());
            function.put(DESCRIPTION, tool.description());
            function.put(PARAMETERS, toJsonSchema(tool.schema()));

            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(TYPE, FUNCTION);
            item.put(FUNCTION, function);
            payload.add(item);
        }
        return payload;
    }

    /**
     * 把 AgentTool 列表转换成 Anthropic tools 数组。
     *
     * <p>与 OpenAI 不同，Anthropic 的 Tool 定义格式：
     * <pre>
     * {
     *   "name": "tool_name",
     *   "description": "tool description",
     *   "input_schema": { ... }
     * }
     * </pre>
     *
     * @param tools AgentTool 列表
     * @return Anthropic tools 数组
     */
    public List<Map<String, Object>> toAnthropicTools(List<AgentTool> tools) {
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>();
        for (AgentTool tool : tools) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(NAME, tool.name());
            item.put(DESCRIPTION, tool.description());
            item.put(INPUT_SCHEMA, toJsonSchema(tool.schema()));
            payload.add(item);
        }
        return payload;
    }

    public Map<String, Object> toOpenAiResponseFormat(ResponseFormat responseFormat) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put(TYPE, responseFormat.getType());

        Map<String, Object> jsonSchema = new LinkedHashMap<String, Object>();
        jsonSchema.put(NAME, responseFormat.getName());
        jsonSchema.put(SCHEMA, responseFormat.getSchema());
        if (responseFormat.getStrict() != null) {
            jsonSchema.put(STRICT, responseFormat.getStrict());
        }
        result.put(JSON_SCHEMA, jsonSchema);
        return result;
    }

    /**
     * 解析 Tool 参数 JSON 字符串。
     *
     * <p>OpenAI 的 arguments 是 JSON 字符串，需要解析成 Map。
     * 如果解析失败，返回空 Map，让后续的必填参数校验捕获错误。</p>
     *
     * @param argumentsJson JSON 字符串
     * @return 参数 Map
     */
    public Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException ex) {
            // Malformed tool arguments should not kill the entire response.
            // Return empty map so the tool can still be invoked (with missing args
            // caught by required-argument validation later).
            return new LinkedHashMap<String, Object>();
        }
    }

    public JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException ex) {
            throw new IllegalStateException("JSON parse failed: " + json, ex);
        }
    }

    public Map<String, Object> toMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return new LinkedHashMap<String, Object>();
        }
        return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
        });
    }

    private String normalizeRole(String role) {
        if (ASSISTANT.equals(role) || SYSTEM.equals(role) || TOOL.equals(role)) {
            return role;
        }
        return USER;
    }

    /**
     * 追加 OpenAI 格式的 Tool 结果消息。
     *
     * <p>OpenAI 要求先补一条 assistant tool_calls 消息，再补每个 tool 角色的结果消息。
     * 这等价于告诉模型："这些 ToolCall 是你刚才要求的，下面是 Runtime 执行后的结果"。</p>
     *
     * @param payload 消息列表
     * @param request 模型请求
     */
    private void appendOpenAiToolMessages(List<Map<String, Object>> payload, ModelRequest request) {
        List<ToolExecutionResult> executions = normalizedToolExecutions(request);
        if (executions.isEmpty()) {
            return;
        }

        // OpenAI 要求先补一条 assistant tool_calls 消息，再补每个 tool 角色的结果消息。
        // 这等价于告诉模型：“这些 ToolCall 是你刚才要求的，下面是 Runtime 执行后的结果”。
        Map<String, Object> assistant = new LinkedHashMap<String, Object>();
        assistant.put(ROLE, ASSISTANT);
        assistant.put(CONTENT, null);
        List<Map<String, Object>> toolCalls = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < executions.size(); i++) {
            ToolCall toolCall = executions.get(i).getToolCall();
            Map<String, Object> function = new LinkedHashMap<String, Object>();
            function.put(NAME, toolCall.getName());
            function.put(ARGUMENTS, toJsonString(toolCall.getArguments()));

            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(ID, toolCallId(toolCall, i));
            item.put(TYPE, FUNCTION);
            item.put(FUNCTION, function);
            toolCalls.add(item);
        }
        assistant.put(TOOL_CALLS, toolCalls);
        payload.add(assistant);

        for (int i = 0; i < executions.size(); i++) {
            ToolExecutionResult execution = executions.get(i);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(ROLE, TOOL);
            item.put(TOOL_CALL_ID, toolCallId(execution.getToolCall(), i));
            item.put(CONTENT, toolResultContent(execution.getToolResult()));
            payload.add(item);
        }
    }

    /**
     * 追加 Anthropic 格式的 Tool 结果消息。
     *
     * <p>与 OpenAI 不同，Anthropic 的 Tool 回填由一条 assistant tool_use 和一条 user tool_result 组成。
     * 它把 Tool 结果视作用户侧提供的新 content block。</p>
     *
     * @param payload 消息列表
     * @param request 模型请求
     */
    private void appendAnthropicToolMessages(List<Map<String, Object>> payload, ModelRequest request) {
        List<ToolExecutionResult> executions = normalizedToolExecutions(request);
        if (executions.isEmpty()) {
            return;
        }

        // Anthropic 的 Tool 回填由一条 assistant tool_use 和一条 user tool_result 组成。
        // 与 OpenAI 不同，它把 Tool 结果视作用户侧提供的新 content block。
        List<Map<String, Object>> toolUseContent = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> toolResultContent = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < executions.size(); i++) {
            ToolExecutionResult execution = executions.get(i);
            ToolCall toolCall = execution.getToolCall();

            Map<String, Object> toolUse = new LinkedHashMap<String, Object>();
            toolUse.put(TYPE, TOOL_USE);
            toolUse.put(ID, toolCallId(toolCall, i));
            toolUse.put(NAME, toolCall.getName());
            toolUse.put(INPUT, toolCall.getArguments());
            toolUseContent.add(toolUse);

            Map<String, Object> toolResult = new LinkedHashMap<String, Object>();
            toolResult.put(TYPE, TOOL_RESULT);
            toolResult.put(TOOL_USE_ID, toolCallId(toolCall, i));
            toolResult.put(CONTENT, toolResultContent(execution.getToolResult()));
            toolResultContent.add(toolResult);
        }

        Map<String, Object> assistant = new LinkedHashMap<String, Object>();
        assistant.put(ROLE, ASSISTANT);
        assistant.put(CONTENT, toolUseContent);
        payload.add(assistant);

        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put(ROLE, USER);
        user.put(CONTENT, toolResultContent);
        payload.add(user);
    }

    /**
     * 标准化 Tool 执行结果列表。
     *
     * <p>兼容两种输入格式：
     * <ul>
     *   <li>lastToolExecutions：新的多 Tool 执行结果列表</li>
     *   <li>lastToolCall + lastToolResult：旧的单 Tool 兼容字段</li>
     * </ul>
     *
     * @param request 模型请求
     * @return 标准化后的 Tool 执行结果列表
     */
    private List<ToolExecutionResult> normalizedToolExecutions(ModelRequest request) {
        if (request.getLastToolExecutions() != null && !request.getLastToolExecutions().isEmpty()) {
            return request.getLastToolExecutions();
        }
        // 兼容早期只支持单 Tool 的字段，避免 provider 升级后破坏旧测试和旧调用方。
        List<ToolExecutionResult> executions = new ArrayList<ToolExecutionResult>();
        addDeprecatedSingleToolExecution(request, executions);
        return executions;
    }

    /**
     * 从旧单 Tool 字段补齐执行结果。
     *
     * @param request    模型请求
     * @param executions 标准化结果列表
     */
    @SuppressWarnings("deprecation")
    private void addDeprecatedSingleToolExecution(ModelRequest request, List<ToolExecutionResult> executions) {
        if (request.getLastToolCall() != null && request.getLastToolResult() != null) {
            executions.add(new ToolExecutionResult(request.getLastToolCall(), request.getLastToolResult()));
        }
    }

    private String toolCallId(ToolCall toolCall, int index) {
        if (toolCall.getId() != null && !toolCall.getId().trim().isEmpty()) {
            return toolCall.getId();
        }
        return CALL_ID_PREFIX + (index + 1);
    }

    private String toolResultContent(ToolResult toolResult) {
        if (toolResult == null) {
            return "";
        }
        if (!toolResult.isSuccess()) {
            return toolResult.getErrorMessage() == null ? "" : toolResult.getErrorMessage();
        }
        Object data = toolResult.getData();
        if (data == null) {
            return "";
        }
        if (data instanceof String) {
            return String.valueOf(data);
        }
        return toJsonString(data);
    }

    private String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new IllegalStateException("JSON serialize failed", ex);
        }
    }

    private Map<String, Object> toJsonSchema(ToolSchema schema) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put(TYPE, schema == null || schema.getType() == null ? OBJECT : schema.getType());
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        if (schema != null) {
            for (Map.Entry<String, ToolSchemaProperty> entry : schema.getProperties().entrySet()) {
                ToolSchemaProperty property = entry.getValue();
                Map<String, Object> propertyPayload = new LinkedHashMap<String, Object>();
                propertyPayload.put(TYPE, property.getType());
                propertyPayload.put(DESCRIPTION, property.getDescription());
                if (property.getEnumValues() != null && !property.getEnumValues().isEmpty()) {
                    propertyPayload.put(ENUM, property.getEnumValues());
                }
                properties.put(entry.getKey(), propertyPayload);
            }
            result.put(REQUIRED, schema.getRequired());
        }
        result.put(PROPERTIES, properties);
        return result;
    }
}
