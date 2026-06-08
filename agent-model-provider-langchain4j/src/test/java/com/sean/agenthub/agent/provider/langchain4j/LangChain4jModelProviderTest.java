package com.sean.agenthub.agent.provider.langchain4j;

import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.api.ModelStreamListener;
import com.sean.agenthub.agent.core.model.AgentMessage;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.core.tool.ToolSchemaProperty;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * LangChain4j ModelProvider adapter tests.
 *
 * @author Sean
 */
public class LangChain4jModelProviderTest {
    @Test
    public void shouldMapTextChat() {
        RecordingChatModel chatModel = new RecordingChatModel("langchain4j answer");
        LangChain4jModelProvider provider = new LangChain4jModelProvider(chatModel);
        ModelRequest request = new ModelRequest();
        request.setSystemPrompt("Be concise");
        request.getMessages().add(new AgentMessage("assistant", "previous answer"));
        request.setUserMessage("hello");

        ModelResponse response = provider.chat(request);

        Assert.assertEquals("langchain4j answer", response.getAnswer());
        Assert.assertTrue(provider.capabilities().contains(ModelProviderCapability.TEXT_CHAT));
        Assert.assertFalse(provider.capabilities().contains(ModelProviderCapability.TOOL_CALL));
        Assert.assertEquals(3, chatModel.messages.size());
    }

    @Test
    public void shouldMapTextStream() {
        RecordingChatModel chatModel = new RecordingChatModel("langchain4j answer");
        RecordingStreamingChatModel streamingChatModel = new RecordingStreamingChatModel();
        LangChain4jModelProvider provider = new LangChain4jModelProvider(chatModel, streamingChatModel);
        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        RecordingListener listener = new RecordingListener();

        provider.streamChat(request, listener);

        Assert.assertTrue(provider.capabilities().contains(ModelProviderCapability.TEXT_STREAM));
        Assert.assertEquals(1, streamingChatModel.messages.size());
        Assert.assertEquals("part-1part-2", listener.text());
        Assert.assertTrue(listener.complete);
        Assert.assertNull(listener.error);
    }

    @Test
    public void shouldMapToolCallResponse() {
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .id("call-1")
                .name("query_order_status")
                .arguments("{\"orderNo\":\"A001\"}")
                .build();
        RecordingChatModel chatModel = new RecordingChatModel(AiMessage.from(toolExecutionRequest));
        LangChain4jModelProvider provider = new LangChain4jModelProvider(chatModel);
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
    public void shouldSendToolSpecificationsWithoutDeclaringToolCallCapability() {
        RecordingChatModel chatModel = new RecordingChatModel("langchain4j answer");
        LangChain4jModelProvider provider = new LangChain4jModelProvider(chatModel);
        ModelRequest request = new ModelRequest();
        request.setUserMessage("query order");
        request.getTools().add(new OrderStatusTool());

        ModelResponse response = provider.chat(request);

        Assert.assertEquals("langchain4j answer", response.getAnswer());
        Assert.assertNotNull(chatModel.chatRequest);
        Assert.assertEquals(1, chatModel.chatRequest.toolSpecifications().size());
        Assert.assertEquals("query_order_status", chatModel.chatRequest.toolSpecifications().get(0).name());
        Assert.assertEquals("查询订单状态", chatModel.chatRequest.toolSpecifications().get(0).description());
        Assert.assertEquals(Arrays.asList("orderNo"), chatModel.chatRequest.toolSpecifications().get(0).parameters().required());
        Assert.assertTrue(chatModel.chatRequest.toolSpecifications().get(0).parameters().properties().containsKey("orderNo"));
        Assert.assertFalse(provider.capabilities().contains(ModelProviderCapability.TOOL_CALL));
    }

    private static class RecordingChatModel implements ChatModel {
        private final String answer;
        private final AiMessage aiMessage;
        private List<ChatMessage> messages = new ArrayList<ChatMessage>();
        private ChatRequest chatRequest;

        private RecordingChatModel(String answer) {
            this.answer = answer;
            this.aiMessage = null;
        }

        private RecordingChatModel(AiMessage aiMessage) {
            this.answer = null;
            this.aiMessage = aiMessage;
        }

        @Override
        public ChatResponse chat(List<ChatMessage> messages) {
            this.messages = messages;
            return ChatResponse.builder()
                    .aiMessage(aiMessage == null ? AiMessage.from(answer) : aiMessage)
                    .build();
        }

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            this.chatRequest = chatRequest;
            this.messages = chatRequest.messages();
            return ChatResponse.builder()
                    .aiMessage(aiMessage == null ? AiMessage.from(answer) : aiMessage)
                    .build();
        }
    }

    private static class RecordingStreamingChatModel implements StreamingChatModel {
        private List<ChatMessage> messages = new ArrayList<ChatMessage>();
        private ChatRequest chatRequest;

        @Override
        public void chat(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
            this.messages = messages;
            handler.onPartialResponse("part-1");
            handler.onPartialResponse("part-2");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("part-1part-2"))
                    .build());
        }

        @Override
        public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
            this.chatRequest = chatRequest;
            this.messages = chatRequest.messages();
            handler.onPartialResponse("part-1");
            handler.onPartialResponse("part-2");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("part-1part-2"))
                    .build());
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
