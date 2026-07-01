package com.sean.agenthub.agent.starter;

import com.sean.agenthub.agent.core.api.AgentService;
import com.sean.agenthub.agent.core.api.AgentStreamListener;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;
import com.sean.agenthub.agent.core.model.ToolCallResult;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * AgentChatController 单元测试。
 *
 * @author Sean
 */
public class AgentChatControllerTest {

    @Test
    public void shouldRejectNullChatRequest() {
        RecordingAgentService service = new RecordingAgentService();
        AgentChatController controller = new AgentChatController(service);

        AgentResponse response = controller.chat(null);

        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("must not be null"));
        Assert.assertEquals(0, service.chatCount);
    }

    @Test
    public void shouldRejectBlankSessionId() {
        RecordingAgentService service = new RecordingAgentService();
        AgentChatController controller = new AgentChatController(service);
        AgentRequest request = request("", "hello");

        AgentResponse response = controller.chat(request);

        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("sessionId"));
        Assert.assertEquals(0, service.chatCount);
    }

    @Test
    public void shouldRejectBlankMessage() {
        RecordingAgentService service = new RecordingAgentService();
        AgentChatController controller = new AgentChatController(service);
        AgentRequest request = request("s001", " ");

        AgentResponse response = controller.chat(request);

        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("message"));
        Assert.assertEquals(0, service.chatCount);
    }

    @Test
    public void shouldWriteStreamErrorWhenRequestInvalid() throws Exception {
        RecordingAgentService service = new RecordingAgentService();
        AgentChatController controller = new AgentChatController(service);
        StreamingResponseBody responseBody = controller.streamChat(request("s001", ""));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        responseBody.writeTo(outputStream);

        String body = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        Assert.assertTrue(body.contains("event: error"));
        Assert.assertTrue(body.contains("message"));
        Assert.assertEquals(0, service.streamCount);
    }

    @Test
    public void shouldDelegateValidChatRequest() {
        RecordingAgentService service = new RecordingAgentService();
        AgentChatController controller = new AgentChatController(service);

        AgentResponse response = controller.chat(request("s001", "hello"));

        Assert.assertTrue(response.isOk());
        Assert.assertEquals("ok", response.getAnswer());
        Assert.assertEquals(1, service.chatCount);
    }

    private AgentRequest request(String sessionId, String message) {
        AgentRequest request = new AgentRequest();
        request.setSessionId(sessionId);
        request.setUserId("u001");
        request.setMessage(message);
        return request;
    }

    private static class RecordingAgentService implements AgentService {
        private int chatCount;
        private int streamCount;

        @Override
        public AgentResponse chat(AgentRequest request) {
            chatCount++;
            return AgentResponse.ok("ok", new ArrayList<ToolCallResult>());
        }

        @Override
        public void streamChat(AgentRequest request, AgentStreamListener listener) {
            streamCount++;
            listener.onComplete();
        }
    }
}
