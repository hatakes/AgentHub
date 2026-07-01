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
 * <p>这个 Controller 是 starter 提供的最小 HTTP 面。业务系统如果已有自己的网关、鉴权或统一返回结构，
 * 可以不使用它，直接注入 AgentService 组合到自己的 Controller 中。</p>
 *
 * @author Sean
 */
@RestController
public class AgentChatController {
    /** Agent 服务，处理对话请求。 */
    private final AgentService agentService;
    /** JSON 序列化器，用于 SSE 事件序列化。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建对话控制器。
     *
     * @param agentService Agent 服务
     */
    public AgentChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 非流式对话接口。
     *
     * @param request 用户请求
     * @return Agent 响应
     */
    @PostMapping("/agent/chat")
    public AgentResponse chat(@RequestBody AgentRequest request) {
        String validationError = validateRequest(request);
        if (validationError != null) {
            return AgentResponse.error(validationError);
        }
        return agentService.chat(request);
    }

    /**
     * 流式对话接口，返回 SSE 事件流。
     *
     * <p>SSE 事件类型：delta（文本增量）、tool（Tool 执行结果）、complete（完成）、error（错误）。</p>
     *
     * @param request 用户请求
     * @return SSE 流式响应体
     */
    @PostMapping(value = "/agent/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody streamChat(@RequestBody final AgentRequest request) {
        // SSE 事件保持简单稳定：delta 传文本片段，tool 传受控 Tool 执行结果，complete / error 表示终态。
        // 前端只需要按 event 类型分流，不需要理解具体模型供应商的流式协议。
        return new StreamingResponseBody() {
            @Override
            public void writeTo(final OutputStream outputStream) {
                String validationError = validateRequest(request);
                if (validationError != null) {
                    writeEvent(outputStream, "error", "message", validationError);
                    return;
                }
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

    /**
     * 校验 starter HTTP 入口的最小必填字段。
     *
     * @param request 用户请求
     * @return 错误信息，合法时返回 null
     */
    private String validateRequest(AgentRequest request) {
        if (request == null) {
            return "Agent request must not be null";
        }
        if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            return "Agent request sessionId must not be blank";
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return "Agent request message must not be blank";
        }
        return null;
    }

    /**
     * 写入一个 SSE 事件帧。
     *
     * @param outputStream 输出流
     * @param event        事件类型
     * @param key          数据字段名
     * @param value        数据字段值
     */
    private void writeEvent(OutputStream outputStream, String event, String key, Object value) {
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("type", event);
            payload.put(key, value);
            // 每个事件都单独 flush，保证调用方能尽快收到模型增量和 Tool 状态。
            String frame = "event: " + event + "\n"
                    + "data: " + objectMapper.writeValueAsString(payload) + "\n\n";
            outputStream.write(frame.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("Write stream response failed: " + ex.getMessage(), ex);
        }
    }
}
