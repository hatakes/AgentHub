package com.sean.agenthub.agent.provider.langchain4j;

import com.fasterxml.jackson.core.type.TypeReference;
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
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LangChain4j ChatModel backed ModelProvider.
 *
 * <p>这个适配器让已经使用 LangChain4j 的业务系统可以复用现有 ChatModel，同时仍然把 Tool 执行权留在
 * AgentHub Runtime 中。LangChain4j 负责模型协议封装，AgentHub 负责权限、审计和 Tool 生命周期。</p>
 *
 * @author Sean
 */
public class LangChain4jModelProvider implements ModelProvider {
    /** LangChain4j 非流式聊天模型。 */
    private final ChatModel chatModel;
    /** LangChain4j 流式聊天模型，可选。 */
    private final StreamingChatModel streamingChatModel;
    /** JSON 序列化器，用于解析 Tool 参数。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建非流式 LangChain4j provider。
     *
     * @param chatModel LangChain4j 聊天模型
     */
    public LangChain4jModelProvider(ChatModel chatModel) {
        this(chatModel, null);
    }

    /**
     * 创建 LangChain4j provider，可选支持流式。
     *
     * @param chatModel         非流式聊天模型
     * @param streamingChatModel 流式聊天模型，null 表示不支持流式
     */
    public LangChain4jModelProvider(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel must not be null");
        }
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    /**
     * 返回 provider 支持的能力集合。
     *
     * @return 能力集合，包含 TEXT_CHAT，有 streamingChatModel 时额外包含 TEXT_STREAM
     */
    @Override
    public Set<ModelProviderCapability> capabilities() {
        EnumSet<ModelProviderCapability> capabilities = EnumSet.of(ModelProviderCapability.TEXT_CHAT);
        if (streamingChatModel != null) {
            capabilities.add(ModelProviderCapability.TEXT_STREAM);
        }
        return capabilities;
    }

    /**
     * 执行非流式聊天调用。
     *
     * <p>将 ModelRequest 转换为 LangChain4j ChatRequest，调用 chatModel，
     * 再将响应转换为 ModelResponse（文本或 ToolCall）。</p>
     *
     * @param request 模型请求
     * @return 模型响应
     */
    @Override
    public ModelResponse chat(ModelRequest request) {
        ChatResponse response = chatModel.chat(toChatRequest(request));
        if (response == null || response.aiMessage() == null) {
            return ModelResponse.answer("");
        }
        AiMessage aiMessage = response.aiMessage();
        if (aiMessage.hasToolExecutionRequests()) {
            return ModelResponse.toolCalls(toToolCalls(aiMessage.toolExecutionRequests()));
        }
        String text = aiMessage.text();
        return ModelResponse.answer(text == null ? "" : text);
    }

    /**
     * 执行流式聊天调用。
     *
     * <p>如果没有注入 StreamingChatModel，退化为非流式调用。</p>
     *
     * @param request  模型请求
     * @param listener 流式输出回调
     */
    @Override
    public void streamChat(ModelRequest request, final ModelStreamListener listener) {
        if (streamingChatModel == null) {
            // 没有注入 StreamingChatModel 时沿用 ModelProvider 默认退化逻辑，避免调用方必须区分 provider 能力。
            ModelProvider.super.streamChat(request, listener);
            return;
        }
        streamingChatModel.chat(toChatRequest(request), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null) {
                    listener.onDelta(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                listener.onComplete();
            }

            @Override
            public void onError(Throwable error) {
                listener.onError(error == null ? null : error.getMessage());
            }
        });
    }

    /**
     * 将 AgentHub ModelRequest 转换为 LangChain4j ChatRequest。
     *
     * @param request AgentHub 请求
     * @return LangChain4j 请求
     */
    private ChatRequest toChatRequest(ModelRequest request) {
        ChatRequest.Builder builder = ChatRequest.builder()
                .messages(toMessages(request));
        List<ToolSpecification> toolSpecifications = toToolSpecifications(request);
        if (!toolSpecifications.isEmpty()) {
            builder.toolSpecifications(toolSpecifications);
        }
        return builder.build();
    }

    /**
     * 将 AgentHub 消息列表转换为 LangChain4j 消息列表。
     *
     * @param request AgentHub 请求
     * @return LangChain4j 消息列表
     */
    private List<ChatMessage> toMessages(ModelRequest request) {
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        if (request == null) {
            return messages;
        }
        if (hasText(request.getSystemPrompt())) {
            messages.add(SystemMessage.from(request.getSystemPrompt()));
        }
        for (AgentMessage message : request.getMessages()) {
            if (message == null || !hasText(message.getContent())) {
                continue;
            }
            if ("assistant".equalsIgnoreCase(message.getRole())) {
                messages.add(AiMessage.from(message.getContent()));
            } else if ("system".equalsIgnoreCase(message.getRole())) {
                messages.add(SystemMessage.from(message.getContent()));
            } else {
                messages.add(UserMessage.from(message.getContent()));
            }
        }
        if (hasText(request.getUserMessage())) {
            messages.add(UserMessage.from(request.getUserMessage()));
        }
        return messages;
    }

    /**
     * 将 AgentHub Tool 列表转换为 LangChain4j ToolSpecification 列表。
     *
     * <p>ToolSpecification 只描述可选工具，不包含实际执行函数；
     * Runtime 会在模型返回 ToolCall 后统一执行。</p>
     *
     * @param request AgentHub 请求
     * @return ToolSpecification 列表
     */
    private List<ToolSpecification> toToolSpecifications(ModelRequest request) {
        List<ToolSpecification> toolSpecifications = new ArrayList<ToolSpecification>();
        if (request == null || request.getTools() == null) {
            return toolSpecifications;
        }
        for (AgentTool tool : request.getTools()) {
            if (tool == null || !hasText(tool.name())) {
                continue;
            }
            // ToolSpecification 只描述可选工具，不包含实际执行函数；Runtime 会在模型返回 ToolCall 后统一执行。
            toolSpecifications.add(ToolSpecification.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .parameters(toJsonObjectSchema(tool.schema()))
                    .build());
        }
        return toolSpecifications;
    }

    /**
     * 将 AgentHub ToolSchema 转换为 LangChain4j JsonObjectSchema。
     *
     * @param schema AgentHub ToolSchema
     * @return LangChain4j JsonObjectSchema
     */
    private JsonObjectSchema toJsonObjectSchema(ToolSchema schema) {
        JsonObjectSchema.Builder builder = JsonObjectSchema.builder()
                .additionalProperties(false);
        if (schema == null) {
            return builder.build();
        }
        for (Map.Entry<String, ToolSchemaProperty> entry : schema.getProperties().entrySet()) {
            if (entry == null || !hasText(entry.getKey())) {
                continue;
            }
            addProperty(builder, entry.getKey(), entry.getValue());
        }
        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            builder.required(schema.getRequired());
        }
        return builder.build();
    }

    /**
     * 向 JsonObjectSchema.Builder 添加单个属性。
     *
     * @param builder  Schema 构建器
     * @param name     属性名
     * @param property 属性定义
     */
    private void addProperty(JsonObjectSchema.Builder builder, String name, ToolSchemaProperty property) {
        String description = property == null ? null : property.getDescription();
        if (property != null && property.getEnumValues() != null && !property.getEnumValues().isEmpty()) {
            builder.addEnumProperty(name, property.getEnumValues(), description);
            return;
        }
        String type = property == null ? null : property.getType();
        if ("integer".equalsIgnoreCase(type)) {
            builder.addIntegerProperty(name, description);
        } else if ("number".equalsIgnoreCase(type)) {
            builder.addNumberProperty(name, description);
        } else if ("boolean".equalsIgnoreCase(type)) {
            builder.addBooleanProperty(name, description);
        } else {
            builder.addStringProperty(name, description);
        }
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
     * 将 LangChain4j ToolExecutionRequest 列表转换为 AgentHub ToolCall 列表。
     *
     * @param requests LangChain4j ToolExecutionRequest 列表
     * @return AgentHub ToolCall 列表
     */
    private List<ToolCall> toToolCalls(List<ToolExecutionRequest> requests) {
        List<ToolCall> toolCalls = new ArrayList<ToolCall>();
        if (requests == null) {
            return toolCalls;
        }
        for (ToolExecutionRequest request : requests) {
            if (request == null) {
                continue;
            }
            toolCalls.add(new ToolCall(request.id(), request.name(), parseArguments(request.arguments())));
        }
        return toolCalls;
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
}
