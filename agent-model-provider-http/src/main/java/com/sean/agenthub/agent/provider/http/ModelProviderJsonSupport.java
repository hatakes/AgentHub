package com.sean.agenthub.agent.provider.http;

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

import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.ARGUMENTS;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.ASSISTANT;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.CALL_ID_PREFIX;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.CONTENT;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.DESCRIPTION;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.ENUM;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.FUNCTION;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.ID;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.INPUT;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.INPUT_SCHEMA;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.JSON_SCHEMA;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.NAME;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.OBJECT;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.PARAMETERS;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.PROPERTIES;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.REQUIRED;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.ROLE;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.SCHEMA;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.STRICT;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.SYSTEM;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.TOOL;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.TOOL_CALL_ID;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.TOOL_CALLS;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.TOOL_RESULT;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.TOOL_USE;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.TOOL_USE_ID;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.TYPE;
import static com.sean.agenthub.agent.provider.http.ModelProviderJsonFields.USER;

/**
 * 模型协议 JSON 转换辅助方法。
 *
 * <p>包内实现细节，封装 OpenAI / Anthropic 协议载荷转换；不作为模块公开 API 暴露。</p>
 *
 * @author Sean
 */
class ModelProviderJsonSupport {
    private final ObjectMapper objectMapper;

    ModelProviderJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<Map<String, Object>> toOpenAiMessages(ModelRequest request) {
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

    List<Map<String, Object>> toAnthropicMessages(ModelRequest request) {
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

    List<Map<String, Object>> toOpenAiTools(List<AgentTool> tools) {
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

    List<Map<String, Object>> toAnthropicTools(List<AgentTool> tools) {
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

    Map<String, Object> toOpenAiResponseFormat(ResponseFormat responseFormat) {
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

    Map<String, Object> parseArguments(String argumentsJson) {
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

    JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException ex) {
            throw new IllegalStateException("JSON parse failed: " + json, ex);
        }
    }

    Map<String, Object> toMap(JsonNode node) {
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

    private void appendOpenAiToolMessages(List<Map<String, Object>> payload, ModelRequest request) {
        List<ToolExecutionResult> executions = normalizedToolExecutions(request);
        if (executions.isEmpty()) {
            return;
        }

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

    private void appendAnthropicToolMessages(List<Map<String, Object>> payload, ModelRequest request) {
        List<ToolExecutionResult> executions = normalizedToolExecutions(request);
        if (executions.isEmpty()) {
            return;
        }

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

    private List<ToolExecutionResult> normalizedToolExecutions(ModelRequest request) {
        if (request.getLastToolExecutions() != null && !request.getLastToolExecutions().isEmpty()) {
            return request.getLastToolExecutions();
        }
        List<ToolExecutionResult> executions = new ArrayList<ToolExecutionResult>();
        if (request.getLastToolCall() != null && request.getLastToolResult() != null) {
            executions.add(new ToolExecutionResult(request.getLastToolCall(), request.getLastToolResult()));
        }
        return executions;
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
