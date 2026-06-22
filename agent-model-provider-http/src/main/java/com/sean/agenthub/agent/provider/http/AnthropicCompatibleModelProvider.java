package com.sean.agenthub.agent.provider.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.api.ModelStreamListener;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonSupport;
import com.sean.agenthub.agent.provider.http.transport.HttpJsonClient;
import com.sean.agenthub.agent.provider.http.transport.HttpRequestSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ANTHROPIC_VERSION;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ANY;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.AUTO;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.CONTENT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.CONTENT_BLOCK;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.CONTENT_BLOCK_DELTA;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.CONTENT_BLOCK_START;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.DELTA;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ERROR;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.ID;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.INDEX;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.INPUT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.INPUT_JSON_DELTA;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.MAX_TOKENS;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.MESSAGE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.MESSAGES;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.MODEL;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.NAME;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.NONE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.PARTIAL_JSON;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.REQUIRED_CHOICE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.STREAM;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.SYSTEM;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TEXT;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TEXT_DELTA;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_CHOICE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOLS;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TOOL_USE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.TYPE;
import static com.sean.agenthub.agent.provider.http.protocol.ModelProviderJsonFields.X_API_KEY;

/**
 * Anthropic-compatible Messages 协议适配。
 *
 * <p>Anthropic 的 messages 协议与 OpenAI Chat Completions 在 Tool 表达上不同：ToolCall 以
 * {@code tool_use} content block 表达，Tool 结果再作为用户侧的 {@code tool_result} block 传回。
 * 本类把这些协议差异限制在 provider 内部，Runtime 仍然只面对统一的 ModelRequest / ModelResponse。</p>
 *
 * @author Sean
 */
public class AnthropicCompatibleModelProvider implements ModelProvider {
    private static final String DEFAULT_MESSAGES_PATH = "/v1/messages";
    private static final String MESSAGES_PATH_SUFFIX = "/messages";
    private static final String SLASH = "/";
    private static final String ANTHROPIC_VERSION_VALUE = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 1024;

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

    /**
     * 执行一次非流式 Anthropic Messages 调用。
     *
     * <p>实现思路：
     * <ol>
     *   <li>把 ModelRequest 转换成 Anthropic 请求体（messages、tools、system、max_tokens）</li>
     *   <li>添加 x-api-key 和 anthropic-version 鉴权头</li>
     *   <li>发送 POST 请求到 /v1/messages</li>
     *   <li>解析响应：如果 content 包含 tool_use block，返回 ModelResponse.toolCalls；否则返回 ModelResponse.answer</li>
     * </ol>
     *
     * @param request 模型请求
     * @return 模型响应（文本或 ToolCall）
     * @throws IllegalStateException 如果模型返回错误
     */
    @Override
    public ModelResponse chat(ModelRequest request) {
        Map<String, Object> body = buildRequestBody(request);

        Map<String, String> headers = buildHeaders();
        JsonNode response = httpJsonClient.postJson(endpoint(DEFAULT_MESSAGES_PATH), headers, body);
        return parseResponse(response);
    }

    /**
     * 执行一次流式 Anthropic Messages 调用。
     *
     * <p>实现思路：
     * <ol>
     *   <li>构建请求体并设置 stream=true</li>
     *   <li>逐行读取 SSE 流，解析每个 event</li>
     *   <li>content_block_start 事件：识别 tool_use block，初始化 ToolCall 构建器</li>
     *   <li>content_block_delta 事件：文本 delta 输出，input_json_delta 追加到构建器</li>
     *   <li>流结束后，把聚合好的 ToolCall 通过 listener.onToolCall 输出</li>
     * </ol>
     *
     * <p>Anthropic stream 会先发 content_block_start，再用 input_json_delta 逐段补齐参数。
     * 与 OpenAI 不同，Anthropic 的 ToolCall 以 content block 形式表达。</p>
     *
     * @param request  模型请求
     * @param listener 流式输出回调
     */
    @Override
    public void streamChat(ModelRequest request, final ModelStreamListener listener) {
        Map<String, Object> body = buildRequestBody(request);
        body.put(STREAM, true);

        final AnthropicStreamToolCallAccumulator toolCallAccumulator = new AnthropicStreamToolCallAccumulator();
        try {
            // Anthropic stream 会先发 content_block_start，再用 input_json_delta 逐段补齐参数。
            // 这里先聚合完整 ToolCall，等流结束后再交给 Runtime。
            httpJsonClient.postJsonStream(endpoint(DEFAULT_MESSAGES_PATH), buildHeaders(), body, new HttpJsonClient.LineHandler() {
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

    /**
     * 构建 Anthropic Messages 请求体。
     *
     * <p>与 OpenAI 不同，Anthropic 的请求体结构：
     * <ul>
     *   <li>model 和 max_tokens 是顶层字段</li>
     *   <li>system prompt 是顶层字段（不在 messages 里）</li>
     *   <li>Tool schema 直接放在 input_schema 下（没有 function wrapper）</li>
     *   <li>tool_choice 使用 type 字段（auto/any/none）</li>
     * </ul>
     *
     * @param request 模型请求
     * @return Anthropic 请求体
     */
    private Map<String, Object> buildRequestBody(ModelRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put(MODEL, properties.getModel());
        body.put(MAX_TOKENS, DEFAULT_MAX_TOKENS);
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            body.put(SYSTEM, request.getSystemPrompt());
        }
        body.put(MESSAGES, jsonSupport.toAnthropicMessages(request));
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            // Anthropic 没有 OpenAI 的 function wrapper，Tool schema 直接放在 input_schema 下。
            body.put(TOOLS, jsonSupport.toAnthropicTools(request.getTools()));
            if (request.getToolChoice() != null) {
                body.put(TOOL_CHOICE, toAnthropicToolChoice(request.getToolChoice()));
            }
        }
        return body;
    }

    private Map<String, Object> toAnthropicToolChoice(String toolChoice) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (NONE.equals(toolChoice)) {
            result.put(TYPE, NONE);
        } else if (REQUIRED_CHOICE.equals(toolChoice) || ANY.equals(toolChoice)) {
            result.put(TYPE, ANY);
        } else if (AUTO.equals(toolChoice)) {
            result.put(TYPE, AUTO);
        } else {
            result.put(TYPE, AUTO);
        }
        return result;
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(X_API_KEY, properties.getApiKey());
        headers.put(ANTHROPIC_VERSION, ANTHROPIC_VERSION_VALUE);
        return headers;
    }

    /**
     * 解析 Anthropic Messages 响应。
     *
     * <p>与 OpenAI 不同，Anthropic 的响应结构：
     * <ul>
     *   <li>content 是一个数组，包含 text 和 tool_use 两种 block</li>
     *   <li>tool_use block 的 input 已经是 JSON object，不需要再解析字符串</li>
     *   <li>优先返回 ToolCall，没有 ToolCall 时返回 text block 的内容</li>
     * </ul>
     *
     * @param response Anthropic 响应 JSON
     * @return AgentHub 模型响应
     * @throws IllegalStateException 如果模型返回错误
     */
    private ModelResponse parseResponse(JsonNode response) {
        if (ERROR.equals(response.path(TYPE).asText())) {
            JsonNode error = response.path(ERROR);
            String errorMsg = error.path(MESSAGE).asText(error.toString());
            throw new IllegalStateException("Model provider error: " + errorMsg);
        }
        JsonNode content = response.path(CONTENT);
        if (content.isArray()) {
            java.util.List<ToolCall> toolCalls = new java.util.ArrayList<ToolCall>();
            for (JsonNode item : content) {
                if (TOOL_USE.equals(item.path(TYPE).asText())) {
                    // input 已经是 JSON object，不需要像 OpenAI arguments 字符串那样再解析一次。
                    Map<String, Object> arguments = new LinkedHashMap<String, Object>();
                    JsonNode input = item.path(INPUT);
                    if (input.isObject()) {
                        arguments = jsonSupport.toMap(input);
                    }
                    toolCalls.add(new ToolCall(item.path(ID).asText(null), item.path(NAME).asText(), arguments));
                }
            }
            if (!toolCalls.isEmpty()) {
                return ModelResponse.toolCalls(toolCalls);
            }
            for (JsonNode item : content) {
                if (TEXT.equals(item.path(TYPE).asText())) {
                    return ModelResponse.answer(item.path(TEXT).asText(""));
                }
            }
        }
        return ModelResponse.answer("");
    }

    /**
     * 处理 Anthropic SSE 流中的单行数据。
     *
     * <p>Anthropic 的流式事件类型：
     * <ul>
     *   <li>content_block_start：开始一个新的 content block（可能是 text 或 tool_use）</li>
     *   <li>content_block_delta：文本增量或 input_json_delta</li>
     *   <li>content_block_stop：block 结束</li>
     * </ul>
     *
     * <p>与 OpenAI 不同，Anthropic 的 ToolCall 以 content block 形式表达，
     * 需要先通过 content_block_start 识别 tool_use block，再通过 input_json_delta 聚合参数。</p>
     *
     * @param line               SSE 原始行
     * @param listener           流式输出回调
     * @param toolCallAccumulator ToolCall 聚合器
     */
    private void handleAnthropicStreamLine(String line,
                                           ModelStreamListener listener,
                                           AnthropicStreamToolCallAccumulator toolCallAccumulator) {
        String data = HttpRequestSupport.sseData(line);
        if (data == null) {
            return;
        }
        try {
            JsonNode node = jsonSupport.parseJson(data);
            String type = node.path(TYPE).asText();
            if (CONTENT_BLOCK_START.equals(type)) {
                toolCallAccumulator.start(node);
                return;
            }
            if (CONTENT_BLOCK_DELTA.equals(type)) {
                JsonNode delta = node.path(DELTA);
                if (TEXT_DELTA.equals(delta.path(TYPE).asText()) && delta.has(TEXT)) {
                    listener.onDelta(delta.path(TEXT).asText());
                }
                if (INPUT_JSON_DELTA.equals(delta.path(TYPE).asText()) && delta.has(PARTIAL_JSON)) {
                    toolCallAccumulator.append(node.path(INDEX).asInt(0), delta.path(PARTIAL_JSON).asText());
                }
            }
        } catch (RuntimeException ex) {
            // Skip malformed stream lines.
        }
    }

    private String endpoint(String defaultPath) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith(MESSAGES_PATH_SUFFIX)) {
            return baseUrl;
        }
        return trimTrailingSlash(baseUrl) + defaultPath;
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith(SLASH) ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * Anthropic 流式 ToolCall 聚合器。
     *
     * <p>与 OpenAI 不同，Anthropic 的 ToolCall 以 content block 形式表达：
     * <ul>
     *   <li>content_block_start：开始一个 tool_use block，包含 id 和 name</li>
     *   <li>input_json_delta：逐段补齐 input JSON</li>
     * </ul>
     *
     * <p>这个类负责按 index 聚合每个 ToolCall 的分片，等流结束后一次性发射。</p>
     */
    private class AnthropicStreamToolCallAccumulator {
        private final Map<Integer, AnthropicStreamToolCallBuilder> builders =
                new LinkedHashMap<Integer, AnthropicStreamToolCallBuilder>();

        /**
         * 处理 content_block_start 事件。
         *
         * <p>如果是 tool_use block，初始化 ToolCall 构建器，设置 id 和 name。</p>
         *
         * @param node content_block_start 事件 JSON
         */
        void start(JsonNode node) {
            JsonNode contentBlock = node.path(CONTENT_BLOCK);
            if (!TOOL_USE.equals(contentBlock.path(TYPE).asText())) {
                return;
            }
            int index = node.path(INDEX).asInt(0);
            AnthropicStreamToolCallBuilder builder = builder(index);
            builder.id = contentBlock.path(ID).asText(null);
            builder.name = contentBlock.path(NAME).asText(null);
            JsonNode input = contentBlock.path(INPUT);
            if (input.isObject() && input.size() > 0) {
                builder.arguments.append(input.toString());
            }
        }

        /**
         * 追加 input_json_delta 分片。
         *
         * @param index       ToolCall 索引
         * @param partialJson 部分 JSON 字符串
         */
        void append(int index, String partialJson) {
            if (partialJson != null) {
                builder(index).arguments.append(partialJson);
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

    /**
     * 单个 Anthropic 流式 ToolCall 构建器。
     *
     * <p>负责聚合单个 ToolCall 的多个分片：
     * <ul>
     *   <li>id 和 name：从 content_block_start 事件获取</li>
     *   <li>arguments：逐段拼接 input_json_delta</li>
     * </ul>
     */
    private class AnthropicStreamToolCallBuilder {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();

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
