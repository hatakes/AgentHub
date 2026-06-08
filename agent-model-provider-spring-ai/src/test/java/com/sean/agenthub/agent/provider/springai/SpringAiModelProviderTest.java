package com.sean.agenthub.agent.provider.springai;

import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.ModelStreamListener;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.AgentMessage;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.core.tool.ToolSchemaProperty;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Spring AI ModelProvider adapter tests.
 *
 * @author Sean
 */
public class SpringAiModelProviderTest {
    @Test
    public void shouldMapTextChat() {
        RecordingChatModel chatModel = new RecordingChatModel("spring ai answer");
        SpringAiModelProvider provider = new SpringAiModelProvider(chatModel);
        ModelRequest request = new ModelRequest();
        request.setSystemPrompt("Be concise");
        request.getMessages().add(new AgentMessage("assistant", "previous answer"));
        request.setUserMessage("hello");

        ModelResponse response = provider.chat(request);

        Assert.assertEquals("spring ai answer", response.getAnswer());
        Assert.assertTrue(provider.capabilities().contains(ModelProviderCapability.TEXT_CHAT));
        Assert.assertFalse(provider.capabilities().contains(ModelProviderCapability.TOOL_CALL));
        Assert.assertEquals(3, chatModel.prompt.getInstructions().size());
    }

    @Test
    public void shouldMapTextStream() {
        RecordingChatModel chatModel = new RecordingChatModel("spring ai answer");
        SpringAiModelProvider provider = new SpringAiModelProvider(chatModel);
        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        RecordingListener listener = new RecordingListener();

        provider.streamChat(request, listener);

        Assert.assertTrue(provider.capabilities().contains(ModelProviderCapability.TEXT_STREAM));
        Assert.assertEquals(1, chatModel.streamPrompt.getInstructions().size());
        Assert.assertEquals("part-1part-2", listener.text());
        Assert.assertTrue(listener.complete);
        Assert.assertNull(listener.error);
    }

    @Test
    public void shouldMapToolCallResponse() {
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call-1",
                "function",
                "query_order_status",
                "{\"orderNo\":\"A001\"}"
        );
        RecordingChatModel chatModel = new RecordingChatModel(
                AssistantMessage.builder()
                        .content("")
                        .toolCalls(Collections.singletonList(toolCall))
                        .build()
        );
        SpringAiModelProvider provider = new SpringAiModelProvider(chatModel);
        ModelRequest request = new ModelRequest();
        request.setUserMessage("query order");

        ModelResponse response = provider.chat(request);

        Assert.assertTrue(response.hasToolCalls());
        Assert.assertEquals(1, response.getToolCalls().size());
        Assert.assertEquals("call-1", response.getToolCalls().get(0).getId());
        Assert.assertEquals("query_order_status", response.getToolCalls().get(0).getName());
        Assert.assertEquals("A001", response.getToolCalls().get(0).getArguments().get("orderNo"));
        Assert.assertFalse(provider.capabilities().contains(ModelProviderCapability.TOOL_CALL));
    }

    @Test
    public void shouldSendToolCallbacksWithoutDeclaringToolCallCapability() {
        RecordingChatModel chatModel = new RecordingChatModel("spring ai answer");
        SpringAiModelProvider provider = new SpringAiModelProvider(chatModel);
        ModelRequest request = new ModelRequest();
        request.setUserMessage("query order");
        request.getTools().add(new OrderStatusTool());

        ModelResponse response = provider.chat(request);

        Assert.assertEquals("spring ai answer", response.getAnswer());
        Assert.assertTrue(chatModel.prompt.getOptions() instanceof ToolCallingChatOptions);
        ToolCallingChatOptions options = (ToolCallingChatOptions) chatModel.prompt.getOptions();
        Assert.assertEquals(Boolean.FALSE, options.getInternalToolExecutionEnabled());
        Assert.assertEquals(1, options.getToolCallbacks().size());
        Assert.assertEquals("query_order_status", options.getToolCallbacks().get(0).getToolDefinition().name());
        Assert.assertEquals("查询订单状态", options.getToolCallbacks().get(0).getToolDefinition().description());
        Assert.assertTrue(options.getToolCallbacks().get(0).getToolDefinition().inputSchema().contains("\"orderNo\""));
        Assert.assertTrue(options.getToolCallbacks().get(0).getToolDefinition().inputSchema().contains("\"required\":[\"orderNo\"]"));
        Assert.assertFalse(provider.capabilities().contains(ModelProviderCapability.TOOL_CALL));
    }

    private static class RecordingChatModel implements org.springframework.ai.chat.model.ChatModel {
        private final String answer;
        private final AssistantMessage assistantMessage;
        private Prompt prompt;
        private Prompt streamPrompt;

        private RecordingChatModel(String answer) {
            this.answer = answer;
            this.assistantMessage = null;
        }

        private RecordingChatModel(AssistantMessage assistantMessage) {
            this.answer = null;
            this.assistantMessage = assistantMessage;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            this.prompt = prompt;
            return new ChatResponse(Arrays.asList(new Generation(
                    assistantMessage == null ? new AssistantMessage(answer) : assistantMessage
            )));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            this.streamPrompt = prompt;
            return Flux.just(
                    new ChatResponse(Arrays.asList(new Generation(new AssistantMessage("part-1")))),
                    new ChatResponse(Arrays.asList(new Generation(new AssistantMessage("part-2"))))
            );
        }
    }

    private static class RecordingListener implements ModelStreamListener {
        private final StringBuilder text = new StringBuilder();
        private boolean complete;
        private String error;

        @Override
        public void onDelta(String delta) {
            text.append(delta);
        }

        @Override
        public void onToolCall(ToolCall toolCall) {
        }

        @Override
        public void onComplete() {
            complete = true;
        }

        @Override
        public void onError(String error) {
            this.error = error;
        }

        private String text() {
            return text.toString();
        }
    }

    private static class OrderStatusTool implements AgentTool {
        @Override
        public String name() {
            return "query_order_status";
        }

        @Override
        public String description() {
            return "查询订单状态";
        }

        @Override
        public ToolSchema schema() {
            ToolSchema schema = new ToolSchema();
            Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
            properties.put("orderNo", new ToolSchemaProperty("string", "订单号"));
            schema.setProperties(properties);
            schema.setRequired(Arrays.asList("orderNo"));
            return schema;
        }

        @Override
        public ToolRiskLevel riskLevel() {
            return ToolRiskLevel.READ;
        }

        @Override
        public ToolResult execute(ToolContext context) {
            return ToolResult.success("unused");
        }
    }
}
