package com.sean.agenthub.agent.core.runtime;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 默认 Agent 执行器，负责串起一次非流式对话。
 *
 * <p>MVP 阶段只支持一次模型发起的 Tool 调用，然后再让模型基于 Tool 结果生成最终回答。</p>
 *
 * @author Sean
 */
public class DefaultAgentRuntime implements AgentRuntime {
    private final ModelProvider modelProvider;
    private final ToolRegistry toolRegistry;
    private final AgentMemory memory;
    private final PermissionEngine permissionEngine;
    private final AuditService auditService;

    public DefaultAgentRuntime(ModelProvider modelProvider,
                               ToolRegistry toolRegistry,
                               AgentMemory memory,
                               PermissionEngine permissionEngine,
                               AuditService auditService) {
        this.modelProvider = modelProvider;
        this.toolRegistry = toolRegistry;
        this.memory = memory;
        this.permissionEngine = permissionEngine;
        this.auditService = auditService;
    }

    @Override
    public AgentResponse run(AgentRequest request, AgentContext context) {
        List<ToolCallResult> toolCallResults = new ArrayList<ToolCallResult>();
        try {
            // 先保存用户输入，后续轮次可以从 Memory 中加载上下文。
            memory.save(request.getSessionId(), new AgentMessage("user", request.getMessage()));
            ModelRequest modelRequest = buildModelRequest(request);
            ModelResponse modelResponse = modelProvider.chat(modelRequest);

            if (!modelResponse.hasToolCalls()) {
                String answer = modelResponse.getAnswer();
                memory.save(request.getSessionId(), new AgentMessage("assistant", answer));
                return AgentResponse.ok(answer, toolCallResults);
            }

            List<ToolExecutionResult> toolExecutionResults = new ArrayList<ToolExecutionResult>();
            for (ToolCall toolCall : modelResponse.getToolCalls()) {
                ToolResult toolResult = executeTool(request, context, toolCall, toolCallResults);
                if (!toolResult.isSuccess()) {
                    return AgentResponse.error(toolResult.getErrorMessage());
                }
                toolExecutionResults.add(new ToolExecutionResult(toolCall, toolResult));
            }

            // Tool 执行成功后，把结果交回模型，由模型生成面向用户的最终回答。
            ModelRequest summarizeRequest = buildModelRequest(request);
            summarizeRequest.setLastToolExecutions(toolExecutionResults);
            ToolExecutionResult lastExecution = toolExecutionResults.get(toolExecutionResults.size() - 1);
            summarizeRequest.setLastToolCall(lastExecution.getToolCall());
            summarizeRequest.setLastToolResult(lastExecution.getToolResult());
            ModelResponse summarizeResponse = modelProvider.chat(summarizeRequest);
            String answer = summarizeResponse.getAnswer();
            memory.save(request.getSessionId(), new AgentMessage("assistant", answer));
            return AgentResponse.ok(answer, toolCallResults);
        } catch (RuntimeException ex) {
            return AgentResponse.error(ex.getMessage());
        }
    }

    @Override
    public void runStream(AgentRequest request, AgentContext context, AgentStreamListener listener) {
        List<ToolCallResult> toolCallResults = new ArrayList<ToolCallResult>();
        try {
            memory.save(request.getSessionId(), new AgentMessage("user", request.getMessage()));
            ModelRequest modelRequest = buildModelRequest(request);

            if (modelRequest.getTools().isEmpty()) {
                streamModelAnswer(request, modelRequest, listener);
                return;
            }

            StreamModelDecision decision = streamModelDecision(modelRequest);
            if (!decision.getToolCalls().isEmpty()) {
                List<ToolExecutionResult> toolExecutionResults = new ArrayList<ToolExecutionResult>();
                for (ToolCall toolCall : decision.getToolCalls()) {
                    ToolResult toolResult = executeTool(request, context, toolCall, toolCallResults);
                    ToolCallResult toolCallResult = toolCallResults.get(toolCallResults.size() - 1);
                    listener.onToolCall(toolCallResult);
                    if (!toolResult.isSuccess()) {
                        listener.onError(toolResult.getErrorMessage());
                        return;
                    }
                    toolExecutionResults.add(new ToolExecutionResult(toolCall, toolResult));
                }

                ModelRequest summarizeRequest = buildModelRequest(request);
                summarizeRequest.setLastToolExecutions(toolExecutionResults);
                ToolExecutionResult lastExecution = toolExecutionResults.get(toolExecutionResults.size() - 1);
                summarizeRequest.setLastToolCall(lastExecution.getToolCall());
                summarizeRequest.setLastToolResult(lastExecution.getToolResult());
                streamModelAnswer(request, summarizeRequest, listener);
                return;
            }

            if (decision.getError() != null) {
                listener.onError(decision.getError());
                return;
            }

            String answer = decision.getAnswer();
            if (answer != null) {
                memory.save(request.getSessionId(), new AgentMessage("assistant", answer));
                if (!answer.isEmpty()) {
                    listener.onDelta(answer);
                }
            }
            listener.onComplete();
        } catch (RuntimeException ex) {
            listener.onError(ex.getMessage());
        }
    }

    private StreamModelDecision streamModelDecision(ModelRequest modelRequest) {
        final StreamModelDecision decision = new StreamModelDecision();
        modelProvider.streamChat(modelRequest, new ModelStreamListener() {
            @Override
            public void onDelta(String delta) {
                if (delta != null) {
                    decision.answer.append(delta);
                }
            }

            @Override
            public void onToolCall(ToolCall toolCall) {
                decision.toolCalls.add(toolCall);
            }

            @Override
            public void onComplete() {
                decision.completed = true;
            }

            @Override
            public void onError(String error) {
                decision.error = error;
            }
        });
        return decision;
    }

    private void streamModelAnswer(final AgentRequest request,
                                   ModelRequest modelRequest,
                                   final AgentStreamListener listener) {
        final StringBuilder answer = new StringBuilder();
        modelProvider.streamChat(modelRequest, new ModelStreamListener() {
            @Override
            public void onDelta(String delta) {
                if (delta != null && !delta.isEmpty()) {
                    answer.append(delta);
                    listener.onDelta(delta);
                }
            }

            @Override
            public void onComplete() {
                memory.save(request.getSessionId(), new AgentMessage("assistant", answer.toString()));
                listener.onComplete();
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    private ModelRequest buildModelRequest(AgentRequest request) {
        ModelRequest modelRequest = new ModelRequest();
        modelRequest.setSessionId(request.getSessionId());
        modelRequest.setUserMessage(request.getMessage());
        modelRequest.setMessages(memory.load(request.getSessionId()));
        modelRequest.setTools(toolRegistry.list());
        if (!toolRegistry.list().isEmpty()) {
            modelRequest.setSystemPrompt(buildToolSelectionPrompt());
        }
        return modelRequest;
    }

    private String buildToolSelectionPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能助手。");
        sb.append("只有当用户的请求明确需要查询数据时才使用工具，不要在闲聊、介绍、解释类问题中调用工具。");
        sb.append("如果用户没有明确要求查询某个具体数据，请直接回答，不要调用任何工具。");
        return sb.toString();
    }

    private ToolResult executeTool(AgentRequest request,
                                   AgentContext context,
                                   ToolCall toolCall,
                                   List<ToolCallResult> toolCallResults) {
        long startedAt = System.currentTimeMillis();
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setTraceId(UUID.randomUUID().toString());
        auditEvent.setSessionId(request.getSessionId());
        auditEvent.setUserId(request.getUserId());
        auditEvent.setToolName(toolCall.getName());
        auditEvent.setRequestSummary(request.getMessage());

        try {
            Optional<AgentTool> optionalTool = toolRegistry.get(toolCall.getName());
            if (!optionalTool.isPresent()) {
                throw new IllegalArgumentException("Tool not found: " + toolCall.getName());
            }

            AgentTool tool = optionalTool.get();
            validateReadOnly(tool);
            validateRequiredArguments(tool.schema(), toolCall.getArguments());

            ToolContext toolContext = new ToolContext(context.getUser(), toolCall.getArguments());
            // 权限检查必须在参数校验之后、业务 Tool 代码执行之前完成。
            PermissionResult permission = permissionEngine.check(context.getUser(), tool, toolContext);
            if (!permission.isAllowed()) {
                throw new IllegalStateException("Tool permission denied: " + permission.getReason());
            }

            ToolResult toolResult = tool.execute(toolContext);
            auditEvent.setSuccess(toolResult.isSuccess());
            auditEvent.setToolResultSummary(String.valueOf(toolResult.getData()));
            auditEvent.setErrorMessage(toolResult.getErrorMessage());
            toolCallResults.add(new ToolCallResult(tool.name(), toolResult.isSuccess(), toolResult.getErrorMessage()));
            return toolResult;
        } catch (RuntimeException ex) {
            auditEvent.setSuccess(false);
            auditEvent.setErrorMessage(ex.getMessage());
            toolCallResults.add(new ToolCallResult(toolCall.getName(), false, ex.getMessage()));
            return ToolResult.error(ex.getMessage());
        } finally {
            auditEvent.setLatencyMs(System.currentTimeMillis() - startedAt);
            auditService.record(auditEvent);
        }
    }

    private void validateReadOnly(AgentTool tool) {
        if (tool.riskLevel() != ToolRiskLevel.READ) {
            throw new IllegalStateException("Only READ tools are enabled in MVP: " + tool.name());
        }
    }

    private void validateRequiredArguments(ToolSchema schema, Map<String, Object> arguments) {
        if (schema == null || schema.getRequired() == null) {
            return;
        }
        for (String key : schema.getRequired()) {
            if (arguments == null || !arguments.containsKey(key) || arguments.get(key) == null) {
                throw new IllegalArgumentException("Missing required tool argument: " + key);
            }
        }
    }

    private static class StreamModelDecision {
        private final StringBuilder answer = new StringBuilder();
        private final List<ToolCall> toolCalls = new ArrayList<ToolCall>();
        private boolean completed;
        private String error;

        String getAnswer() {
            return answer.toString();
        }

        List<ToolCall> getToolCalls() {
            return toolCalls;
        }

        String getError() {
            if (error != null) {
                return error;
            }
            return completed ? null : "Model stream did not complete.";
        }
    }
}
