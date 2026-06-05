package com.sean.agenthub.agent.provider.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.api.ModelStreamListener;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ResponseFormat;
import com.sean.agenthub.agent.core.model.ToolCall;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI-compatible Chat Completions 协议适配。
 *
 * @author Sean
 */
public class OpenAiCompatibleModelProvider implements ModelProvider {
    private final HttpModelProviderProperties properties;
    private final HttpJsonClient httpJsonClient;
    private final ModelProviderJsonSupport jsonSupport;

    public OpenAiCompatibleModelProvider(HttpModelProviderProperties properties) {
        this(properties, new ObjectMapper());
    }

    OpenAiCompatibleModelProvider(HttpModelProviderProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpJsonClient = new HttpJsonClient(objectMapper, properties.getConnectTimeoutMs(), properties.getReadTimeoutMs());
        this.jsonSupport = new ModelProviderJsonSupport(objectMapper);
    }

    @Override
    public Set<ModelProviderCapability> capabilities() {
        return java.util.EnumSet.of(
                ModelProviderCapability.TEXT_CHAT,
                ModelProviderCapability.TEXT_STREAM,
                ModelProviderCapability.STREAM_TOOL_CALL,
                ModelProviderCapability.TOOL_CALL,
                ModelProviderCapability.MULTI_TOOL_CALL,
                ModelProviderCapability.TOOL_RESULT_MESSAGES,
                ModelProviderCapability.STRUCTURED_OUTPUT
        );
    }

    @Override
    public ModelResponse chat(ModelRequest request) {
        Map<String, Object> body = buildRequestBody(request);

        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", properties.getApiKey() == null ? null : "Bearer " + properties.getApiKey());
        JsonNode response = httpJsonClient.postJson(endpoint("/v1/chat/completions"), headers, body);
        return parseResponse(response);
    }

    @Override
    public void streamChat(ModelRequest request, final ModelStreamListener listener) {
        Map<String, Object> body = buildRequestBody(request);
        body.put("stream", true);

        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", properties.getApiKey() == null ? null : "Bearer " + properties.getApiKey());
        final OpenAiStreamToolCallAccumulator toolCallAccumulator = new OpenAiStreamToolCallAccumulator();
        try {
            httpJsonClient.postJsonStream(endpoint("/v1/chat/completions"), headers, body, new HttpJsonClient.LineHandler() {
                @Override
                public void onLine(String line) {
                    handleOpenAiStreamLine(line, listener, toolCallAccumulator);
                }
            });
            toolCallAccumulator.emit(listener);
            listener.onComplete();
        } catch (RuntimeException ex) {
            listener.onError(ex.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(ModelRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", properties.getModel());
        body.put("messages", jsonSupport.toOpenAiMessages(request));
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", jsonSupport.toOpenAiTools(request.getTools()));
            body.put("tool_choice", request.getToolChoice() != null ? request.getToolChoice() : "auto");
        }
        if (request.getResponseFormat() != null) {
            body.put("response_format", jsonSupport.toOpenAiResponseFormat(request.getResponseFormat()));
        }
        return body;
    }

    private ModelResponse parseResponse(JsonNode response) {
        JsonNode error = response.path("error");
        if (error.isObject()) {
            String errorMsg = error.path("message").asText(error.toString());
            throw new IllegalStateException("Model provider error: " + errorMsg);
        }
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            return ModelResponse.answer("");
        }
        JsonNode message = choices.path(0).path("message");
        JsonNode toolCalls = message.path("tool_calls");
        if (toolCalls.isArray() && toolCalls.size() > 0) {
            java.util.List<ToolCall> parsedToolCalls = new java.util.ArrayList<ToolCall>();
            for (JsonNode item : toolCalls) {
                JsonNode function = item.path("function");
                parsedToolCalls.add(new ToolCall(
                        item.path("id").asText(null),
                        function.path("name").asText(),
                        jsonSupport.parseArguments(function.path("arguments").asText())
                ));
            }
            return ModelResponse.toolCalls(parsedToolCalls);
        }
        return ModelResponse.answer(message.path("content").asText(""));
    }

    private void handleOpenAiStreamLine(String line,
                                        ModelStreamListener listener,
                                        OpenAiStreamToolCallAccumulator toolCallAccumulator) {
        if (line == null || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring("data:".length()).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return;
        }
        try {
            JsonNode node = jsonSupport.parseJson(data);
            JsonNode delta = node.path("choices").path(0).path("delta");
            if (delta.has("content") && !delta.path("content").isNull()) {
                listener.onDelta(delta.path("content").asText());
            }
            JsonNode toolCalls = delta.path("tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                toolCallAccumulator.append(toolCalls);
            }
        } catch (RuntimeException ex) {
            // Skip malformed stream lines (e.g. truncated JSON from proxy/network issues).
            // A single bad line should not terminate the entire stream.
        }
    }

    private String endpoint(String defaultPath) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        String normalizedBaseUrl = trimTrailingSlash(baseUrl);
        if (normalizedBaseUrl.endsWith("/v1")) {
            return normalizedBaseUrl + "/chat/completions";
        }
        return normalizedBaseUrl + defaultPath;
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private class OpenAiStreamToolCallAccumulator {
        private final Map<Integer, OpenAiStreamToolCallBuilder> builders =
                new LinkedHashMap<Integer, OpenAiStreamToolCallBuilder>();

        void append(JsonNode toolCalls) {
            for (int i = 0; i < toolCalls.size(); i++) {
                JsonNode item = toolCalls.get(i);
                int index = item.has("index") ? item.path("index").asInt() : i;
                OpenAiStreamToolCallBuilder builder = builders.get(index);
                if (builder == null) {
                    builder = new OpenAiStreamToolCallBuilder();
                    builders.put(index, builder);
                }
                builder.append(item);
            }
        }

        void emit(ModelStreamListener listener) {
            List<ToolCall> toolCalls = new java.util.ArrayList<ToolCall>();
            for (OpenAiStreamToolCallBuilder builder : builders.values()) {
                if (builder.hasToolCall()) {
                    toolCalls.add(builder.build());
                }
            }
            for (ToolCall toolCall : toolCalls) {
                listener.onToolCall(toolCall);
            }
        }
    }

    private class OpenAiStreamToolCallBuilder {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();

        void append(JsonNode item) {
            if (item.has("id") && !item.path("id").isNull() && !item.path("id").asText().isEmpty()) {
                id = item.path("id").asText();
            }
            JsonNode function = item.path("function");
            if (function.has("name") && !function.path("name").isNull() && !function.path("name").asText().isEmpty()) {
                name = function.path("name").asText();
            }
            if (function.has("arguments") && !function.path("arguments").isNull()) {
                arguments.append(function.path("arguments").asText());
            }
        }

        boolean hasToolCall() {
            return name != null && !name.trim().isEmpty();
        }

        ToolCall build() {
            return new ToolCall(id, name, jsonSupport.parseArguments(arguments.toString()));
        }
    }
}
