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
 * @author Sean
 */
public class SpringAiModelProvider implements ModelProvider {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpringAiModelProvider(ChatModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel must not be null");
        }
        this.chatModel = chatModel;
    }

    @Override
    public Set<ModelProviderCapability> capabilities() {
        return EnumSet.of(ModelProviderCapability.TEXT_CHAT, ModelProviderCapability.TEXT_STREAM);
    }

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

    private void disableInternalToolExecutionIfSupported(ToolCallingChatOptions.Builder builder) {
        try {
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

    private List<ToolCallback> toToolCallbacks(ModelRequest request) {
        List<ToolCallback> callbacks = new ArrayList<ToolCallback>();
        if (request == null || request.getTools() == null) {
            return callbacks;
        }
        for (AgentTool tool : request.getTools()) {
            if (tool == null || !hasText(tool.name())) {
                continue;
            }
            callbacks.add(new AgentHubToolCallback(tool));
        }
        return callbacks;
    }

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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

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

    private class AgentHubToolCallback implements ToolCallback {
        private final ToolDefinition toolDefinition;

        private AgentHubToolCallback(AgentTool tool) {
            this.toolDefinition = DefaultToolDefinition.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .inputSchema(toInputSchema(tool.schema()))
                    .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        public String call(String toolInput) {
            throw new UnsupportedOperationException("AgentHub executes tools after permission and audit checks");
        }
    }
}
