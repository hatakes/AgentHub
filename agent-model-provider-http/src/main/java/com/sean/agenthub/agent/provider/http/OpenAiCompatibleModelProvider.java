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
import com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonSupport;
import com.sean.agenthub.agent.provider.http.transport.HttpJsonClient;
import com.sean.agenthub.agent.provider.http.transport.HttpRequestSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ARGUMENTS;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.AUTHORIZATION;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.AUTO;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.CHOICES;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.CONTENT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.DELTA;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ERROR;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.FUNCTION;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ID;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.INDEX;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.MESSAGE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.MESSAGES;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.MODEL;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.NAME;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.RESPONSE_FORMAT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.STREAM;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_CALLS;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_CHOICE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOLS;

/**
 * OpenAI-compatible Chat Completions 协议适配。
 *
 * <p>该 provider 只负责把 AgentHub 的 ModelRequest / ModelResponse 转换成 OpenAI Chat Completions
 * 兼容协议。Tool 是否允许执行、参数是否完整、审计如何记录，仍然由 agent-core 的 Runtime 处理。</p>
 *
 * <p>兼容目标包括 OpenAI 官方接口以及暴露 {@code /v1/chat/completions} 的国产或私有化模型网关。</p>
 *
 * @author Sean
 */
public class OpenAiCompatibleModelProvider implements ModelProvider {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEFAULT_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String CHAT_COMPLETIONS_PATH_SUFFIX = "/chat/completions";
    private static final String V1_PATH_SUFFIX = "/v1";
    private static final String SLASH = "/";

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

    /**
     * 执行一次非流式 OpenAI Chat Completions 调用。
     *
     * <p>实现思路：
     * <ol>
     *   <li>把 ModelRequest 转换成 OpenAI 请求体（messages、tools、response_format）</li>
     *   <li>添加 Bearer Token 鉴权头（apiKey 允许为空，便于接本地无鉴权网关）</li>
     *   <li>发送 POST 请求到 /v1/chat/completions</li>
     *   <li>解析响应：如果包含 tool_calls，返回 ModelResponse.toolCalls；否则返回 ModelResponse.answer</li>
     * </ol>
     *
     * @param request 模型请求
     * @return 模型响应（文本或 ToolCall）
     * @throws IllegalStateException 如果模型返回错误
     */
    @Override
    public ModelResponse chat(ModelRequest request) {
        Map<String, Object> body = buildRequestBody(request);

        // OpenAI 兼容协议通常使用 Bearer Token；apiKey 允许为空，便于接本地无鉴权网关。
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(AUTHORIZATION, properties.getApiKey() == null ? null : BEARER_PREFIX + properties.getApiKey());
        JsonNode response = httpJsonClient.postJson(endpoint(DEFAULT_CHAT_COMPLETIONS_PATH), headers, body);
        return parseResponse(response);
    }

    /**
     * 执行一次流式 OpenAI Chat Completions 调用。
     *
     * <p>实现思路：
     * <ol>
     *   <li>构建请求体并设置 stream=true</li>
     *   <li>逐行读取 SSE 流，解析每个 data 行</li>
     *   <li>文本 delta 直接通过 listener.onDelta 输出</li>
     *   <li>ToolCall delta 先聚合到 OpenAiStreamToolCallAccumulator</li>
     *   <li>流结束后，把聚合好的 ToolCall 通过 listener.onToolCall 输出</li>
     * </ol>
     *
     * <p>OpenAI stream 会把 tool_calls 拆成多段 delta（按 index 并行分片返回），
     * 必须先聚合完整 name / arguments，再通知 Runtime 执行 Tool。</p>
     *
     * @param request  模型请求
     * @param listener 流式输出回调
     */
    @Override
    public void streamChat(ModelRequest request, final ModelStreamListener listener) {
        Map<String, Object> body = buildRequestBody(request);
        body.put(STREAM, true);

        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(AUTHORIZATION, properties.getApiKey() == null ? null : BEARER_PREFIX + properties.getApiKey());
        final OpenAiStreamToolCallAccumulator toolCallAccumulator = new OpenAiStreamToolCallAccumulator();
        try {
            // OpenAI stream 会把 tool_calls 拆成多段 delta；必须先聚合完整 name / arguments，
            // 再通知 Runtime 执行 Tool。
            httpJsonClient.postJsonStream(endpoint(DEFAULT_CHAT_COMPLETIONS_PATH), headers, body, new HttpJsonClient.LineHandler() {
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

    /**
     * 构建 OpenAI Chat Completions 请求体。
     *
     * <p>把 AgentHub 的 ModelRequest 转换成 OpenAI 协议格式，包括：
     * <ul>
     *   <li>model：从配置读取</li>
     *   <li>messages：通过 jsonSupport 转换，包含 system prompt、历史消息和 Tool 结果</li>
     *   <li>tools：只在有 Tool 时添加，工具清单来自 Runtime 下发的 ToolRegistry 快照</li>
     *   <li>tool_choice：默认 auto，可通过 request 自定义</li>
     *   <li>response_format：用于 Structured Output</li>
     * </ul>
     *
     * @param request 模型请求
     * @return OpenAI 请求体
     */
    private Map<String, Object> buildRequestBody(ModelRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put(MODEL, properties.getModel());
        body.put(MESSAGES, jsonSupport.toOpenAiMessages(request));
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            // 工具清单只来自 Runtime 下发的 ToolRegistry 快照，provider 不主动发现或执行任何业务能力。
            body.put(TOOLS, jsonSupport.toOpenAiTools(request.getTools()));
            body.put(TOOL_CHOICE, request.getToolChoice() != null ? request.getToolChoice() : AUTO);
        }
        if (request.getResponseFormat() != null) {
            body.put(RESPONSE_FORMAT, jsonSupport.toOpenAiResponseFormat(request.getResponseFormat()));
        }
        return body;
    }

    /**
     * 解析 OpenAI Chat Completions 响应。
     *
     * <p>实现思路：
     * <ol>
     *   <li>检查是否有 error 字段，有则抛出异常</li>
     *   <li>获取 choices[0].message</li>
     *   <li>如果包含 tool_calls，解析每个 ToolCall（id、name、arguments）并返回 ModelResponse.toolCalls</li>
     *   <li>否则提取 content 文本并返回 ModelResponse.answer</li>
     * </ol>
     *
     * <p>provider 只解析 ToolCall 意图，不在这里执行 Tool。这样权限、审计和只读限制不会被绕过。</p>
     *
     * @param response OpenAI 响应 JSON
     * @return AgentHub 模型响应
     * @throws IllegalStateException 如果模型返回错误
     */
    private ModelResponse parseResponse(JsonNode response) {
        JsonNode error = response.path(ERROR);
        if (error.isObject()) {
            String errorMsg = error.path(MESSAGE).asText(error.toString());
            throw new IllegalStateException("Model provider error: " + errorMsg);
        }
        JsonNode choices = response.path(CHOICES);
        if (!choices.isArray() || choices.size() == 0) {
            return ModelResponse.answer("");
        }
        JsonNode message = choices.path(0).path(MESSAGE);
        JsonNode toolCalls = message.path(TOOL_CALLS);
        if (toolCalls.isArray() && toolCalls.size() > 0) {
            // provider 只解析 ToolCall 意图，不在这里执行 Tool。这样权限、审计和只读限制不会被绕过。
            java.util.List<ToolCall> parsedToolCalls = new java.util.ArrayList<ToolCall>();
            for (JsonNode item : toolCalls) {
                JsonNode function = item.path(FUNCTION);
                parsedToolCalls.add(new ToolCall(
                        item.path(ID).asText(null),
                        function.path(NAME).asText(),
                        jsonSupport.parseArguments(function.path(ARGUMENTS).asText())
                ));
            }
            return ModelResponse.toolCalls(parsedToolCalls);
        }
        return ModelResponse.answer(message.path(CONTENT).asText(""));
    }

    /**
     * 处理 OpenAI SSE 流中的单行数据。
     *
     * <p>实现思路：
     * <ol>
     *   <li>提取 SSE data 字段（跳过非 data 行和 [DONE] 标记）</li>
     *   <li>解析 JSON，获取 choices[0].delta</li>
     *   <li>如果有 content，通过 listener.onDelta 输出文本增量</li>
     *   <li>如果有 tool_calls，追加到 toolCallAccumulator 聚合</li>
     * </ol>
     *
     * <p>单行解析失败会被静默跳过（如代理/网络问题导致的截断 JSON），
     * 避免一个坏行终止整个流。</p>
     *
     * @param line               SSE 原始行
     * @param listener           流式输出回调
     * @param toolCallAccumulator ToolCall 聚合器
     */
    private void handleOpenAiStreamLine(String line,
                                        ModelStreamListener listener,
                                        OpenAiStreamToolCallAccumulator toolCallAccumulator) {
        String data = HttpRequestSupport.sseData(line);
        if (data == null) {
            return;
        }
        try {
            JsonNode node = jsonSupport.parseJson(data);
            JsonNode delta = node.path(CHOICES).path(0).path(DELTA);
            if (delta.has(CONTENT) && !delta.path(CONTENT).isNull()) {
                listener.onDelta(delta.path(CONTENT).asText());
            }
            JsonNode toolCalls = delta.path(TOOL_CALLS);
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                toolCallAccumulator.append(toolCalls);
            }
        } catch (RuntimeException ex) {
            // Skip malformed stream lines (e.g. truncated JSON from proxy/network issues).
            // A single bad line should not terminate the entire stream.
        }
    }

    /**
     * 计算 Chat Completions 端点 URL。
     *
     * <p>支持多种 baseUrl 格式：
     * <ul>
     *   <li>已包含 /chat/completions：直接使用</li>
     *   <li>已包含 /v1：拼接 /chat/completions</li>
     *   <li>其他：拼接 /v1/chat/completions</li>
     * </ul>
     *
     * @param defaultPath 默认路径
     * @return 完整端点 URL
     */
    private String endpoint(String defaultPath) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith(CHAT_COMPLETIONS_PATH_SUFFIX)) {
            return baseUrl;
        }
        String normalizedBaseUrl = trimTrailingSlash(baseUrl);
        if (normalizedBaseUrl.endsWith(V1_PATH_SUFFIX)) {
            return normalizedBaseUrl + CHAT_COMPLETIONS_PATH_SUFFIX;
        }
        return normalizedBaseUrl + defaultPath;
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith(SLASH) ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * OpenAI 流式 ToolCall 聚合器。
     *
     * <p>OpenAI stream 会把多个 ToolCall 按 index 并行分片返回。这个类负责：
     * <ul>
     *   <li>按 index 聚合每个 ToolCall 的分片</li>
     *   <li>等流结束后一次性发射完整的 ToolCall</li>
     * </ul>
     *
     * <p>使用 index 作为 key 可以保持模型返回的 ToolCall 顺序和边界。</p>
     */
    private class OpenAiStreamToolCallAccumulator {
        private final Map<Integer, OpenAiStreamToolCallBuilder> builders =
                new LinkedHashMap<Integer, OpenAiStreamToolCallBuilder>();

        /**
         * 追加 ToolCall 分片。
         *
         * @param toolCalls 包含 index 和部分数据的 ToolCall 数组
         */
        void append(JsonNode toolCalls) {
            // 多个 ToolCall 会按 index 并行分片返回；用 index 聚合可以保持模型返回的顺序和边界。
            for (int i = 0; i < toolCalls.size(); i++) {
                JsonNode item = toolCalls.get(i);
                int index = item.has(INDEX) ? item.path(INDEX).asInt() : i;
                OpenAiStreamToolCallBuilder builder = builders.get(index);
                if (builder == null) {
                    builder = new OpenAiStreamToolCallBuilder();
                    builders.put(index, builder);
                }
                builder.append(item);
            }
        }

        /**
         * 发射所有已聚合的 ToolCall。
         *
         * <p>等流结束后调用，把聚合好的 ToolCall 通过 listener.onToolCall 输出。</p>
         *
         * @param listener 流式输出回调
         */
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

    /**
     * 单个 OpenAI 流式 ToolCall 构建器。
     *
     * <p>负责聚合单个 ToolCall 的多个分片：
     * <ul>
     *   <li>id：从第一个包含 id 的分片获取</li>
     *   <li>name：从第一个包含 name 的分片获取</li>
     *   <li>arguments：逐段拼接 JSON 字符串</li>
     * </ul>
     */
    private class OpenAiStreamToolCallBuilder {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();

        /**
         * 追加单个 ToolCall 分片。
         *
         * @param item 包含部分数据的 ToolCall JSON 节点
         */
        void append(JsonNode item) {
            if (item.has(ID) && !item.path(ID).isNull() && !item.path(ID).asText().isEmpty()) {
                id = item.path(ID).asText();
            }
            JsonNode function = item.path(FUNCTION);
            if (function.has(NAME) && !function.path(NAME).isNull() && !function.path(NAME).asText().isEmpty()) {
                name = function.path(NAME).asText();
            }
            if (function.has(ARGUMENTS) && !function.path(ARGUMENTS).isNull()) {
                arguments.append(function.path(ARGUMENTS).asText());
            }
        }

        /**
         * 判断是否已聚合到有效的 ToolCall。
         *
         * @return 如果 name 不为空则返回 true
         */
        boolean hasToolCall() {
            return name != null && !name.trim().isEmpty();
        }

        /**
         * 构建最终的 ToolCall 对象。
         *
         * @return 包含 id、name 和解析后 arguments 的 ToolCall
         */
        ToolCall build() {
            return new ToolCall(id, name, jsonSupport.parseArguments(arguments.toString()));
        }
    }
}
