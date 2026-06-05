package com.sean.agenthub.agent.core.runtime;

import com.sean.agenthub.agent.core.api.AgentMemory;
import com.sean.agenthub.agent.core.api.AgentStreamListener;
import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.AuditService;
import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.api.PermissionEngine;
import com.sean.agenthub.agent.core.api.ToolRegistry;
import com.sean.agenthub.agent.core.memory.InMemoryAgentMemory;
import com.sean.agenthub.agent.core.model.AgentContext;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;
import com.sean.agenthub.agent.core.model.AuditEvent;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.PermissionResult;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.core.model.ToolExecutionResult;
import com.sean.agenthub.agent.core.model.UserContext;
import com.sean.agenthub.agent.core.permission.NoopPermissionEngine;
import com.sean.agenthub.agent.core.tool.InMemoryToolRegistry;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * DefaultAgentRuntime 单元测试。
 *
 * @author Sean
 */
public class DefaultAgentRuntimeTest {

    @Test
    public void shouldReturnDirectAnswerWhenModelDoesNotRequestTool() {
        InMemoryAgentMemory memory = new InMemoryAgentMemory();
        DefaultAgentRuntime runtime = newRuntime(
                ModelResponse.answer("直接回答"),
                new InMemoryToolRegistry(),
                memory,
                new NoopPermissionEngine(),
                new RecordingAuditService()
        );

        AgentResponse response = runtime.run(request("s001", "u001", "你好"), context("u001"));

        Assert.assertTrue(response.isOk());
        Assert.assertEquals("直接回答", response.getAnswer());
        Assert.assertTrue(response.getToolCalls().isEmpty());
        Assert.assertEquals(2, memory.load("s001").size());
    }

    @Test
    public void shouldExecuteReadToolAndSummarizeResult() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        CountingTool tool = new CountingTool("query_user", ToolRiskLevel.READ);
        registry.register(tool);
        RecordingAuditService auditService = new RecordingAuditService();
        ModelProvider modelProvider = new ToolThenSummaryModelProvider(toolCall("query_user", argument("userId", "u001")));
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                modelProvider,
                registry,
                new InMemoryAgentMemory(),
                new NoopPermissionEngine(),
                auditService
        );

        AgentResponse response = runtime.run(request("s001", "u001", "查询用户"), context("u001"));

        Assert.assertTrue(response.isOk());
        Assert.assertEquals("总结：tool-data", response.getAnswer());
        Assert.assertEquals(1, response.getToolCalls().size());
        Assert.assertEquals("query_user", response.getToolCalls().get(0).getTool());
        Assert.assertEquals(1, tool.getExecuteCount());
        Assert.assertEquals(1, auditService.events.size());
        Assert.assertTrue(auditService.events.get(0).isSuccess());
    }

    @Test
    public void shouldExecuteMultipleToolCallsAndSummarizeResults() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        CountingTool userTool = new CountingTool("query_user", ToolRiskLevel.READ);
        CountingTool fileTool = new CountingTool("query_file", ToolRiskLevel.READ);
        registry.register(userTool);
        registry.register(fileTool);
        RecordingAuditService auditService = new RecordingAuditService();
        ModelProvider modelProvider = new MultiToolThenSummaryModelProvider(Arrays.asList(
                toolCall("query_user", argument("userId", "u001")),
                toolCall("query_file", argument("userId", "u001"))
        ));
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                modelProvider,
                registry,
                new InMemoryAgentMemory(),
                new NoopPermissionEngine(),
                auditService
        );

        AgentResponse response = runtime.run(request("s001", "u001", "查询用户和文件"), context("u001"));

        Assert.assertTrue(response.isOk());
        Assert.assertEquals("总结：2", response.getAnswer());
        Assert.assertEquals(2, response.getToolCalls().size());
        Assert.assertEquals(1, userTool.getExecuteCount());
        Assert.assertEquals(1, fileTool.getExecuteCount());
        Assert.assertEquals(2, auditService.events.size());
        Assert.assertTrue(auditService.events.get(0).isSuccess());
        Assert.assertTrue(auditService.events.get(1).isSuccess());
    }

    @Test
    public void shouldStreamDirectAnswerWhenNoToolsAreRegistered() {
        InMemoryAgentMemory memory = new InMemoryAgentMemory();
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                new StreamingModelProvider("你", "好"),
                new InMemoryToolRegistry(),
                memory,
                new NoopPermissionEngine(),
                new RecordingAuditService()
        );
        RecordingStreamListener listener = new RecordingStreamListener();

        runtime.runStream(request("s001", "u001", "你好"), context("u001"), listener);

        Assert.assertEquals("你好", listener.answer.toString());
        Assert.assertTrue(listener.completed);
        Assert.assertNull(listener.error);
        Assert.assertEquals(2, memory.load("s001").size());
        Assert.assertEquals("你好", memory.load("s001").get(1).getContent());
    }

    @Test
    public void shouldExecuteStreamToolCallAndStreamSummary() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        CountingTool tool = new CountingTool("query_user", ToolRiskLevel.READ);
        registry.register(tool);
        InMemoryAgentMemory memory = new InMemoryAgentMemory();
        RecordingAuditService auditService = new RecordingAuditService();
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                new StreamingToolThenSummaryModelProvider(toolCall("query_user", argument("userId", "u001"))),
                registry,
                memory,
                new NoopPermissionEngine(),
                auditService
        );
        RecordingStreamListener listener = new RecordingStreamListener();

        runtime.runStream(request("s001", "u001", "查询用户"), context("u001"), listener);

        Assert.assertEquals("总结：tool-data", listener.answer.toString());
        Assert.assertEquals(1, listener.toolCalls.size());
        Assert.assertEquals("query_user", listener.toolCalls.get(0).getTool());
        Assert.assertTrue(listener.toolCalls.get(0).isSuccess());
        Assert.assertTrue(listener.completed);
        Assert.assertNull(listener.error);
        Assert.assertEquals(1, tool.getExecuteCount());
        Assert.assertEquals(1, auditService.events.size());
        Assert.assertEquals(2, memory.load("s001").size());
        Assert.assertEquals("总结：tool-data", memory.load("s001").get(1).getContent());
    }

    @Test
    public void shouldRejectToolCallWhenRequiredArgumentIsMissing() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        CountingTool tool = new CountingTool("query_user", ToolRiskLevel.READ);
        registry.register(tool);
        RecordingAuditService auditService = new RecordingAuditService();
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                new ToolThenSummaryModelProvider(new ToolCall("query_user", new HashMap<String, Object>())),
                registry,
                new InMemoryAgentMemory(),
                new NoopPermissionEngine(),
                auditService
        );

        AgentResponse response = runtime.run(request("s001", "u001", "查询用户"), context("u001"));

        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("Missing required tool argument"));
        Assert.assertEquals(0, tool.getExecuteCount());
        Assert.assertEquals(1, auditService.events.size());
        Assert.assertFalse(auditService.events.get(0).isSuccess());
    }

    @Test
    public void shouldIncludeSystemPromptWhenToolsAreRegistered() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        CountingTool tool = new CountingTool("query_user", ToolRiskLevel.READ);
        registry.register(tool);
        CapturingModelProvider modelProvider = new CapturingModelProvider(ModelResponse.answer("ok"));
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                modelProvider,
                registry,
                new InMemoryAgentMemory(),
                new NoopPermissionEngine(),
                new RecordingAuditService()
        );

        runtime.run(request("s001", "u001", "你好"), context("u001"));

        Assert.assertNotNull(modelProvider.capturedRequest.getSystemPrompt());
        Assert.assertTrue(modelProvider.capturedRequest.getSystemPrompt().contains("只有当用户的请求明确需要查询数据时才使用工具"));
    }

    @Test
    public void shouldNotIncludeSystemPromptWhenNoToolsRegistered() {
        CapturingModelProvider modelProvider = new CapturingModelProvider(ModelResponse.answer("ok"));
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                modelProvider,
                new InMemoryToolRegistry(),
                new InMemoryAgentMemory(),
                new NoopPermissionEngine(),
                new RecordingAuditService()
        );

        runtime.run(request("s001", "u001", "你好"), context("u001"));

        Assert.assertNull(modelProvider.capturedRequest.getSystemPrompt());
    }

    @Test
    public void shouldSetLastToolResultToLastExecutionWhenMultipleToolsExecute() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        CountingTool userTool = new CountingTool("query_user", ToolRiskLevel.READ);
        CountingTool fileTool = new CountingTool("query_file", ToolRiskLevel.READ);
        registry.register(userTool);
        registry.register(fileTool);
        RecordingAuditService auditService = new RecordingAuditService();
        CapturingModelProvider modelProvider = new CapturingModelProvider(ModelResponse.answer("ok"));
        modelProvider.setFirstResponse(ModelResponse.toolCalls(Arrays.asList(
                toolCall("query_user", argument("userId", "u001")),
                toolCall("query_file", argument("userId", "u001"))
        )));
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                modelProvider,
                registry,
                new InMemoryAgentMemory(),
                new NoopPermissionEngine(),
                auditService
        );

        runtime.run(request("s001", "u001", "查询用户和文件"), context("u001"));

        // lastToolResult should contain the LAST tool's result, not the first
        Assert.assertNotNull(modelProvider.capturedRequest);
        // The summarize request should have lastToolResult from the last tool (query_file)
        // Both tools return "tool-data", but we verify the field is populated correctly
        Assert.assertNotNull(modelProvider.capturedRequest.getLastToolResult());
        Assert.assertNotNull(modelProvider.capturedRequest.getLastToolCall());
        Assert.assertEquals("query_file", modelProvider.capturedRequest.getLastToolCall().getName());
    }

    @Test
    public void shouldCheckPermissionBeforeExecutingTool() {
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        CountingTool tool = new CountingTool("query_user", ToolRiskLevel.READ);
        registry.register(tool);
        RecordingAuditService auditService = new RecordingAuditService();
        PermissionEngine deniedPermission = new PermissionEngine() {
            @Override
            public PermissionResult check(UserContext user, AgentTool tool, ToolContext context) {
                return PermissionResult.denied("无权限");
            }
        };
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(
                new ToolThenSummaryModelProvider(toolCall("query_user", argument("userId", "u001"))),
                registry,
                new InMemoryAgentMemory(),
                deniedPermission,
                auditService
        );

        AgentResponse response = runtime.run(request("s001", "u001", "查询用户"), context("u001"));

        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("Tool permission denied"));
        Assert.assertEquals(0, tool.getExecuteCount());
        Assert.assertEquals(1, auditService.events.size());
        Assert.assertFalse(auditService.events.get(0).isSuccess());
    }

    private DefaultAgentRuntime newRuntime(ModelResponse response,
                                           ToolRegistry registry,
                                           AgentMemory memory,
                                           PermissionEngine permissionEngine,
                                           AuditService auditService) {
        return new DefaultAgentRuntime(new FixedModelProvider(response), registry, memory, permissionEngine, auditService);
    }

    private AgentRequest request(String sessionId, String userId, String message) {
        AgentRequest request = new AgentRequest();
        request.setSessionId(sessionId);
        request.setUserId(userId);
        request.setMessage(message);
        return request;
    }

    private AgentContext context(String userId) {
        return new AgentContext(new UserContext(userId));
    }

    private ToolCall toolCall(String name, Map<String, Object> arguments) {
        return new ToolCall(name, arguments);
    }

    private Map<String, Object> argument(String key, Object value) {
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put(key, value);
        return arguments;
    }

    private static class FixedModelProvider implements ModelProvider {
        private final ModelResponse response;

        private FixedModelProvider(ModelResponse response) {
            this.response = response;
        }

        @Override
        public ModelResponse chat(ModelRequest request) {
            return response;
        }
    }

    private static class CapturingModelProvider implements ModelProvider {
        private final ModelResponse defaultResponse;
        private ModelResponse firstResponse;
        private ModelRequest capturedRequest;
        private int callCount;

        private CapturingModelProvider(ModelResponse response) {
            this.defaultResponse = response;
        }

        private void setFirstResponse(ModelResponse firstResponse) {
            this.firstResponse = firstResponse;
        }

        @Override
        public ModelResponse chat(ModelRequest request) {
            this.capturedRequest = request;
            callCount++;
            if (firstResponse != null && callCount == 1) {
                return firstResponse;
            }
            return defaultResponse;
        }
    }

    private static class ToolThenSummaryModelProvider implements ModelProvider {
        private final ToolCall toolCall;

        private ToolThenSummaryModelProvider(ToolCall toolCall) {
            this.toolCall = toolCall;
        }

        @Override
        public ModelResponse chat(ModelRequest request) {
            if (request.getLastToolResult() != null) {
                return ModelResponse.answer("总结：" + request.getLastToolResult().getData());
            }
            return ModelResponse.toolCall(toolCall);
        }
    }

    private static class MultiToolThenSummaryModelProvider implements ModelProvider {
        private final List<ToolCall> toolCalls;

        private MultiToolThenSummaryModelProvider(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }

        @Override
        public ModelResponse chat(ModelRequest request) {
            if (request.getLastToolExecutions() != null && !request.getLastToolExecutions().isEmpty()) {
                for (ToolExecutionResult execution : request.getLastToolExecutions()) {
                    Assert.assertTrue(execution.getToolResult().isSuccess());
                }
                return ModelResponse.answer("总结：" + request.getLastToolExecutions().size());
            }
            return ModelResponse.toolCalls(toolCalls);
        }
    }

    private static class StreamingModelProvider implements ModelProvider {
        private final List<String> chunks;

        private StreamingModelProvider(String... chunks) {
            this.chunks = Arrays.asList(chunks);
        }

        @Override
        public ModelResponse chat(ModelRequest request) {
            return ModelResponse.answer("");
        }

        @Override
        public void streamChat(ModelRequest request, com.sean.agenthub.agent.core.api.ModelStreamListener listener) {
            for (String chunk : chunks) {
                listener.onDelta(chunk);
            }
            listener.onComplete();
        }
    }

    private static class StreamingToolThenSummaryModelProvider implements ModelProvider {
        private final ToolCall toolCall;

        private StreamingToolThenSummaryModelProvider(ToolCall toolCall) {
            this.toolCall = toolCall;
        }

        @Override
        public ModelResponse chat(ModelRequest request) {
            return ModelResponse.answer("");
        }

        @Override
        public void streamChat(ModelRequest request, com.sean.agenthub.agent.core.api.ModelStreamListener listener) {
            if (request.getLastToolExecutions() == null || request.getLastToolExecutions().isEmpty()) {
                listener.onToolCall(toolCall);
                listener.onComplete();
                return;
            }
            listener.onDelta("总结：");
            listener.onDelta(String.valueOf(request.getLastToolExecutions().get(0).getToolResult().getData()));
            listener.onComplete();
        }
    }

    private static class RecordingStreamListener implements AgentStreamListener {
        private final StringBuilder answer = new StringBuilder();
        private final List<com.sean.agenthub.agent.core.model.ToolCallResult> toolCalls =
                new ArrayList<com.sean.agenthub.agent.core.model.ToolCallResult>();
        private boolean completed;
        private String error;

        @Override
        public void onDelta(String delta) {
            answer.append(delta);
        }

        @Override
        public void onToolCall(com.sean.agenthub.agent.core.model.ToolCallResult result) {
            toolCalls.add(result);
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        @Override
        public void onError(String error) {
            this.error = error;
        }
    }

    private static class CountingTool implements AgentTool {
        private final String name;
        private final ToolRiskLevel riskLevel;
        private int executeCount;

        private CountingTool(String name, ToolRiskLevel riskLevel) {
            this.name = name;
            this.riskLevel = riskLevel;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "测试 Tool";
        }

        @Override
        public ToolSchema schema() {
            ToolSchema schema = new ToolSchema();
            schema.setRequired(Arrays.asList("userId"));
            return schema;
        }

        @Override
        public ToolRiskLevel riskLevel() {
            return riskLevel;
        }

        @Override
        public ToolResult execute(ToolContext context) {
            executeCount++;
            return ToolResult.success("tool-data");
        }

        private int getExecuteCount() {
            return executeCount;
        }
    }

    private static class RecordingAuditService implements AuditService {
        private final List<AuditEvent> events = new ArrayList<AuditEvent>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }
    }
}
