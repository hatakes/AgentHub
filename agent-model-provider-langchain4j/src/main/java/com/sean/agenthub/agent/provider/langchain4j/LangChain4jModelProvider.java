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
 * @author Sean
 */
public class LangChain4jModelProvider implements ModelProvider {
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LangChain4jModelProvider(ChatModel chatModel) {
        this(chatModel, null);
    }

    public LangChain4jModelProvider(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel must not be null");
        }
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    @Override
    public Set<ModelProviderCapability> capabilities() {
        EnumSet<ModelProviderCapability> capabilities = EnumSet.of(ModelProviderCapability.TEXT_CHAT);
        if (streamingChatModel != null) {
            capabilities.add(ModelProviderCapability.TEXT_STREAM);
        }
        return capabilities;
    }

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

    @Override
    public void streamChat(ModelRequest request, final ModelStreamListener listener) {
        if (streamingChatModel == null) {
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

    private ChatRequest toChatRequest(ModelRequest request) {
        ChatRequest.Builder builder = ChatRequest.builder()
                .messages(toMessages(request));
        List<ToolSpecification> toolSpecifications = toToolSpecifications(request);
        if (!toolSpecifications.isEmpty()) {
            builder.toolSpecifications(toolSpecifications);
        }
        return builder.build();
    }

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

    private List<ToolSpecification> toToolSpecifications(ModelRequest request) {
        List<ToolSpecification> toolSpecifications = new ArrayList<ToolSpecification>();
        if (request == null || request.getTools() == null) {
            return toolSpecifications;
        }
        for (AgentTool tool : request.getTools()) {
            if (tool == null || !hasText(tool.name())) {
                continue;
            }
            toolSpecifications.add(ToolSpecification.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .parameters(toJsonObjectSchema(tool.schema()))
                    .build());
        }
        return toolSpecifications;
    }

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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

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
