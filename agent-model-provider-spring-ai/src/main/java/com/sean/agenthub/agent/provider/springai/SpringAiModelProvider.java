package com.sean.agenthub.agent.provider.springai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.api.ModelStreamListener;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.AgentMessage;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.core.tool.ToolSchemaProperty;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Spring AI ChatModel backed ModelProvider.
 *
 * <p>Spring AI 原生支持 ToolCallback，但 AgentHub 需要在 Tool 执行前插入参数校验、权限校验和审计。
 * 因此本 provider 只把 Tool 定义传给模型，让模型返回 ToolCall；真正的 ToolCallback.call 不会被用于
 * 执行业务逻辑。</p>
 *
 * @author Sean
 */
public class SpringAiModelProvider implements ModelProvider {
    /** Spring AI 聊天模型。 */
    private final ChatModel chatModel;
    /** JSON 序列化器，用于解析 Tool 参数和 Schema。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 Spring AI provider。
     *
     * @param chatModel Spring AI 聊天模型
     * @throws IllegalArgumentException 如果 chatModel 为 null
     */
    public SpringAiModelProvider(ChatModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel must not be null");
        }
        this.chatModel = chatModel;
    }

    /**
     * 返回 provider 支持的能力集合。
     *
     * @return 包含 TEXT_CHAT 和 TEXT_STREAM 的能力集合
     */
    @Override
    public Set<ModelProviderCapability> capabilities() {
        return EnumSet.of(ModelProviderCapability.TEXT_CHAT, ModelProviderCapability.TEXT_STREAM);
    }

    /**
     * 执行非流式聊天调用。
     *
     * <p>将 ModelRequest 转换为 Spring AI Prompt，调用 chatModel，
     * 再将响应转换为 ModelResponse（文本或 ToolCall）。</p>
     *
     * @param request 模型请求
     * @return 模型响应
     */
    @Override
    public ModelResponse chat(ModelRequest request) {
        ChatResponse response = chatModel.call(toPrompt(request));
        if (response == null || response.getResult() == null) {
            return ModelResponse.answer("");
        }
        Generation generation = response.getResult();
        if (generation.getOutput() == null) {
            return ModelResponse.answer("");
        }
        if (generation.getOutput().hasToolCalls()) {
            return ModelResponse.toolCalls(toToolCalls(generation.getOutput().getToolCalls()));
        }
        if (generation.getOutput().getText() == null) {
            return ModelResponse.answer("");
        }
        return ModelResponse.answer(generation.getOutput().getText());
    }

    /**
     * 执行流式聊天调用。
     *
     * <p>使用 Spring AI 的 Reactive stream API 逐段输出文本增量。</p>
     *
     * @param request  模型请求
     * @param listener 流式输出回调
     */
    @Override
    public void streamChat(ModelRequest request, final ModelStreamListener listener) {
        final AtomicBoolean errorNotified = new AtomicBoolean(false);
        try {
            chatModel.stream(toPrompt(request))
                    .doOnNext(response -> {
                        String text = extractText(response);
                        if (text != null) {
                            listener.onDelta(text);
                        }
                    })
                    .doOnError(error -> {
                        errorNotified.set(true);
                        listener.onError(error.getMessage());
                    })
                    .doOnComplete(listener::onComplete)
                    .blockLast();
        } catch (RuntimeException ex) {
            if (!errorNotified.get()) {
                listener.onError(ex.getMessage());
            }
        }
    }

    /**
     * 从 ChatResponse 中提取文本。
     *
     * @param response Spring AI 响应
     * @return 文本内容，无内容返回 null
     */
    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return null;
        }
        Generation generation = response.getResult();
        if (generation.getOutput() == null) {
            return null;
        }
        return generation.getOutput().getText();
    }

    /**
     * 将 AgentHub ModelRequest 转换为 Spring AI Prompt。
     *
     * <p>如果有 Tool，会创建 ToolCallingChatOptions 并尝试关闭内部 Tool 执行，
     * 确保 Tool 执行权留在 AgentHub Runtime。</p>
     *
     * @param request AgentHub 请求
     * @return Spring AI Prompt
     */
    private Prompt toPrompt(ModelRequest request) {
        List<ToolCallback> toolCallbacks = toToolCallbacks(request);
        if (toolCallbacks.isEmpty()) {
            return new Prompt(toMessages(request));
        }
        ToolCallingChatOptions.Builder builder = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks);
        disableInternalToolExecutionIfSupported(builder);
        ToolCallingChatOptions options = builder.build();
        return new Prompt(toMessages(request), options);
    }

    /**
     * 尝试关闭 Spring AI 内部 Tool 执行。
     *
     * <p>Spring AI 部分版本支持关闭内部 Tool 执行。关闭后模型只返回 ToolCall，
     * Runtime 才能继续保持统一安全边界。不支持的版本会静默跳过。</p>
     *
     * @param builder ToolCallingChatOptions 构建器
     */
    private void disableInternalToolExecutionIfSupported(ToolCallingChatOptions.Builder builder) {
        try {
            // Spring AI 部分版本支持关闭内部 Tool 执行。关闭后模型只返回 ToolCall，
            // Runtime 才能继续保持统一安全边界。
            Method method = builder.getClass().getMethod("internalToolExecutionEnabled", Boolean.class);
            method.invoke(builder, Boolean.FALSE);
        } catch (NoSuchMethodException ex) {
            // Spring AI 2.0 RC removed this option; AgentHub still executes returned tool calls itself.
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Failed to configure Spring AI tool execution", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new IllegalStateException("Failed to configure Spring AI tool execution", cause);
        }
    }

    /**
     * 将 AgentHub 消息列表转换为 Spring AI Message 列表。
     *
     * @param request AgentHub 请求
     * @return Spring AI 消息列表
     */
    private List<Message> toMessages(ModelRequest request) {
        List<Message> messages = new ArrayList<Message>();
        if (request == null) {
            return messages;
        }
        if (hasText(request.getSystemPrompt())) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        for (AgentMessage message : request.getMessages()) {
            if (message == null || !hasText(message.getContent())) {
                continue;
            }
            if ("assistant".equalsIgnoreCase(message.getRole())) {
                messages.add(new AssistantMessage(message.getContent()));
            } else if ("system".equalsIgnoreCase(message.getRole())) {
                messages.add(new SystemMessage(message.getContent()));
            } else {
                messages.add(new UserMessage(message.getContent()));
            }
        }
        if (hasText(request.getUserMessage())) {
            messages.add(new UserMessage(request.getUserMessage()));
        }
        return messages;
    }

    /**
     * 将 AgentHub Tool 列表转换为 Spring AI ToolCallback 列表。
     *
     * <p>ToolCallback 在这里作为 schema carrier 使用，不作为实际业务执行入口。</p>
     *
     * @param request AgentHub 请求
     * @return ToolCallback 列表
     */
    private List<ToolCallback> toToolCallbacks(ModelRequest request) {
        List<ToolCallback> callbacks = new ArrayList<ToolCallback>();
        if (request == null || request.getTools() == null) {
            return callbacks;
        }
        for (AgentTool tool : request.getTools()) {
            if (tool == null || !hasText(tool.name())) {
                continue;
            }
            // ToolCallback 在这里作为 schema carrier 使用，不作为实际业务执行入口。
            callbacks.add(new AgentHubToolCallback(tool));
        }
        return callbacks;
    }

    /**
     * 将 AgentHub ToolSchema 转换为 JSON Schema 字符串。
     *
     * @param schema AgentHub ToolSchema
     * @return JSON Schema 字符串
     */
    private String toInputSchema(ToolSchema schema) {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("type", schema == null || !hasText(schema.getType()) ? "object" : schema.getType());
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        if (schema != null) {
            for (Map.Entry<String, ToolSchemaProperty> entry : schema.getProperties().entrySet()) {
                if (entry == null || !hasText(entry.getKey())) {
                    continue;
                }
                properties.put(entry.getKey(), toPropertySchema(entry.getValue()));
            }
            json.put("required", schema.getRequired());
        } else {
            json.put("required", new ArrayList<String>());
        }
        json.put("properties", properties);
        json.put("additionalProperties", false);
        try {
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tool input schema", ex);
        }
    }

    /**
     * 将 AgentHub ToolSchemaProperty 转换为 JSON Schema 属性 Map。
     *
     * @param property AgentHub 属性定义
     * @return JSON Schema 属性 Map
     */
    private Map<String, Object> toPropertySchema(ToolSchemaProperty property) {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("type", property == null || !hasText(property.getType()) ? "string" : property.getType());
        if (property != null && hasText(property.getDescription())) {
            json.put("description", property.getDescription());
        }
        if (property != null && property.getEnumValues() != null && !property.getEnumValues().isEmpty()) {
            json.put("enum", property.getEnumValues());
        }
        return json;
    }

    /**
     * 判断字符串非空。
     *
     * @param value 待检查字符串
     * @return 非空非空白返回 true
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 将 Spring AI ToolCall 列表转换为 AgentHub ToolCall 列表。
     *
     * @param toolCalls Spring AI ToolCall 列表
     * @return AgentHub ToolCall 列表
     */
    private List<ToolCall> toToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        List<ToolCall> result = new ArrayList<ToolCall>();
        if (toolCalls == null) {
            return result;
        }
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            if (toolCall == null) {
                continue;
            }
            result.add(new ToolCall(toolCall.id(), toolCall.name(), parseArguments(toolCall.arguments())));
        }
        return result;
    }

    /**
     * 解析 Tool 参数 JSON 字符串为 Map。
     *
     * @param argumentsJson 参数 JSON 字符串
     * @return 参数 Map
     */
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (!hasText(argumentsJson)) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException ex) {
            return new LinkedHashMap<String, Object>();
        }
    }

    /**
     * AgentHub 自定义 ToolCallback，只作为 schema carrier，不执行实际业务逻辑。
     *
     * <p>如果某个 Spring AI 版本仍尝试内部执行 Tool，call() 会立即抛出异常，
     * 避免绕过 Runtime 的权限和审计。</p>
     */
    private class AgentHubToolCallback implements ToolCallback {
        /** Tool 定义，包含名称、描述和输入 Schema。 */
        private final ToolDefinition toolDefinition;

        /**
         * 创建 AgentHub ToolCallback。
         *
         * @param tool AgentTool 实例
         */
        private AgentHubToolCallback(AgentTool tool) {
            this.toolDefinition = DefaultToolDefinition.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .inputSchema(toInputSchema(tool.schema()))
                    .build();
        }

        /**
         * 获取 Tool 定义。
         *
         * @return Tool 定义
         */
        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        /**
         * 禁止直接执行，必须通过 AgentHub Runtime 的权限和审计流程。
         *
         * @param toolInput Tool 输入
         * @return 永不返回
         * @throws UnsupportedOperationException 始终抛出
         */
        @Override
        public String call(String toolInput) {
            // 如果某个 Spring AI 版本仍尝试内部执行 Tool，立即失败，避免绕过 Runtime 的权限和审计。
            throw new UnsupportedOperationException("AgentHub executes tools after permission and audit checks");
        }
    }
}
