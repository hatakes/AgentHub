package com.sean.agenthub.agent.provider.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.api.ModelStreamListener;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Anthropic-compatible Messages 协议适配。
 *
 * @author Sean
 */
public class AnthropicCompatibleModelProvider implements ModelProvider {
    private final HttpModelProviderProperties properties;
    private final HttpJsonClient httpJsonClient;
    private final ModelProviderJsonSupport jsonSupport;

    public AnthropicCompatibleModelProvider(HttpModelProviderProperties properties) {
        this(properties, new ObjectMapper());
    }

    AnthropicCompatibleModelProvider(HttpModelProviderProperties properties, ObjectMapper objectMapper) {
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
                ModelProviderCapability.TOOL_RESULT_MESSAGES
        );
    }

    @Override
    public ModelResponse chat(ModelRequest request) {
        Map<String, Object> body = buildRequestBody(request);

        Map<String, String> headers = buildHeaders();
        JsonNode response = httpJsonClient.postJson(endpoint("/v1/messages"), headers, body);
        return parseResponse(response);
    }

    @Override
    public void streamChat(ModelRequest request, final ModelStreamListener listener) {
        Map<String, Object> body = buildRequestBody(request);
        body.put("stream", true);

        final AnthropicStreamToolCallAccumulator toolCallAccumulator = new AnthropicStreamToolCallAccumulator();
        try {
            httpJsonClient.postJsonStream(endpoint("/v1/messages"), buildHeaders(), body, new HttpJsonClient.LineHandler() {
                @Override
                public void onLine(String line) {
                    handleAnthropicStreamLine(line, listener, toolCallAccumulator);
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
        body.put("max_tokens", 1024);
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            body.put("system", request.getSystemPrompt());
        }
        body.put("messages", jsonSupport.toAnthropicMessages(request));
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", jsonSupport.toAnthropicTools(request.getTools()));
            if (request.getToolChoice() != null) {
                body.put("tool_choice", toAnthropicToolChoice(request.getToolChoice()));
            }
        }
        return body;
    }

    private Map<String, Object> toAnthropicToolChoice(String toolChoice) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if ("none".equals(toolChoice)) {
            result.put("type", "none");
        } else if ("required".equals(toolChoice) || "any".equals(toolChoice)) {
            result.put("type", "any");
        } else if ("auto".equals(toolChoice)) {
            result.put("type", "auto");
        } else {
            result.put("type", "auto");
        }
        return result;
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("x-api-key", properties.getApiKey());
        headers.put("anthropic-version", "2023-06-01");
        return headers;
    }

    private ModelResponse parseResponse(JsonNode response) {
        if ("error".equals(response.path("type").asText())) {
            JsonNode error = response.path("error");
            String errorMsg = error.path("message").asText(error.toString());
            throw new IllegalStateException("Model provider error: " + errorMsg);
        }
        JsonNode content = response.path("content");
        if (content.isArray()) {
            java.util.List<ToolCall> toolCalls = new java.util.ArrayList<ToolCall>();
            for (JsonNode item : content) {
                if ("tool_use".equals(item.path("type").asText())) {
                    Map<String, Object> arguments = new LinkedHashMap<String, Object>();
                    JsonNode input = item.path("input");
                    if (input.isObject()) {
                        arguments = jsonSupport.toMap(input);
                    }
                    toolCalls.add(new ToolCall(item.path("id").asText(null), item.path("name").asText(), arguments));
                }
            }
            if (!toolCalls.isEmpty()) {
                return ModelResponse.toolCalls(toolCalls);
            }
            for (JsonNode item : content) {
                if ("text".equals(item.path("type").asText())) {
                    return ModelResponse.answer(item.path("text").asText(""));
                }
            }
        }
        return ModelResponse.answer("");
    }

    private void handleAnthropicStreamLine(String line,
                                           ModelStreamListener listener,
                                           AnthropicStreamToolCallAccumulator toolCallAccumulator) {
        if (line == null || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring("data:".length()).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return;
        }
        try {
            JsonNode node = jsonSupport.parseJson(data);
            String type = node.path("type").asText();
            if ("content_block_start".equals(type)) {
                toolCallAccumulator.start(node);
                return;
            }
            if ("content_block_delta".equals(type)) {
                JsonNode delta = node.path("delta");
                if ("text_delta".equals(delta.path("type").asText()) && delta.has("text")) {
                    listener.onDelta(delta.path("text").asText());
                }
                if ("input_json_delta".equals(delta.path("type").asText()) && delta.has("partial_json")) {
                    toolCallAccumulator.append(node.path("index").asInt(0), delta.path("partial_json").asText());
                }
            }
        } catch (RuntimeException ex) {
            // Skip malformed stream lines.
        }
    }

    private String endpoint(String defaultPath) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/messages")) {
            return baseUrl;
        }
        return trimTrailingSlash(baseUrl) + defaultPath;
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private class AnthropicStreamToolCallAccumulator {
        private final Map<Integer, AnthropicStreamToolCallBuilder> builders =
                new LinkedHashMap<Integer, AnthropicStreamToolCallBuilder>();

        void start(JsonNode node) {
            JsonNode contentBlock = node.path("content_block");
            if (!"tool_use".equals(contentBlock.path("type").asText())) {
                return;
            }
            int index = node.path("index").asInt(0);
            AnthropicStreamToolCallBuilder builder = builder(index);
            builder.id = contentBlock.path("id").asText(null);
            builder.name = contentBlock.path("name").asText(null);
            JsonNode input = contentBlock.path("input");
            if (input.isObject() && input.size() > 0) {
                builder.arguments.append(input.toString());
            }
        }

        void append(int index, String partialJson) {
            if (partialJson != null) {
                builder(index).arguments.append(partialJson);
            }
        }

        void emit(ModelStreamListener listener) {
            List<ToolCall> toolCalls = new java.util.ArrayList<ToolCall>();
            for (AnthropicStreamToolCallBuilder builder : builders.values()) {
                if (builder.hasToolCall()) {
                    toolCalls.add(builder.build());
                }
            }
            for (ToolCall toolCall : toolCalls) {
                listener.onToolCall(toolCall);
            }
        }

        private AnthropicStreamToolCallBuilder builder(int index) {
            AnthropicStreamToolCallBuilder builder = builders.get(index);
            if (builder == null) {
                builder = new AnthropicStreamToolCallBuilder();
                builders.put(index, builder);
            }
            return builder;
        }
    }

    private class AnthropicStreamToolCallBuilder {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();

        boolean hasToolCall() {
            return name != null && !name.trim().isEmpty();
        }

        ToolCall build() {
            return new ToolCall(id, name, jsonSupport.parseArguments(arguments.toString()));
        }
    }
}
