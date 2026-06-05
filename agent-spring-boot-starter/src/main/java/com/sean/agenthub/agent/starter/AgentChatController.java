package com.sean.agenthub.agent.starter;

import com.sean.agenthub.agent.core.api.AgentService;
import com.sean.agenthub.agent.core.api.AgentStreamListener;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;
import com.sean.agenthub.agent.core.model.ToolCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Starter 暴露的 HTTP 对话入口。
 *
 * @author Sean
 */
@RestController
public class AgentChatController {
    private final AgentService agentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/agent/chat")
    public AgentResponse chat(@RequestBody AgentRequest request) {
        return agentService.chat(request);
    }

    @PostMapping(value = "/agent/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody streamChat(@RequestBody final AgentRequest request) {
        return new StreamingResponseBody() {
            @Override
            public void writeTo(final OutputStream outputStream) {
                agentService.streamChat(request, new AgentStreamListener() {
                    @Override
                    public void onDelta(String delta) {
                        writeEvent(outputStream, "delta", "text", delta);
                    }

                    @Override
                    public void onToolCall(ToolCallResult result) {
                        writeEvent(outputStream, "tool", "toolCall", result);
                    }

                    @Override
                    public void onComplete() {
                        writeEvent(outputStream, "complete", "ok", true);
                    }

                    @Override
                    public void onError(String error) {
                        writeEvent(outputStream, "error", "message", error);
                    }
                });
            }
        };
    }

    private void writeEvent(OutputStream outputStream, String event, String key, Object value) {
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("type", event);
            payload.put(key, value);
            String frame = "event: " + event + "\n"
                    + "data: " + objectMapper.writeValueAsString(payload) + "\n\n";
            outputStream.write(frame.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("Write stream response failed: " + ex.getMessage(), ex);
        }
    }
}
