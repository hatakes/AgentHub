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
 * 默认 Agent 执行器，负责串起一次 Agent 对话。
 *
 * <p>这个类是 agent-core 的主编排点。它只依赖 ModelProvider、ToolRegistry、AgentMemory、
 * PermissionEngine 和 AuditService 这些抽象，不直接依赖 Spring、HTTP Client、数据库或具体模型厂商。
 * 这样 core 可以保持稳定，具体模型和业务能力都通过接口注入。</p>
 *
 * <p>当前链路采用“模型决策 -> Runtime 受控执行 Tool -> 模型总结”的模式。模型只负责判断是否需要
 * ToolCall 和生成最终自然语言，真正的 Tool 查找、参数校验、权限校验、执行和审计都在 Runtime 中完成。</p>
 *
 * @author Sean
 */
public class DefaultAgentRuntime implements AgentRuntime {
    private final ModelProvider modelProvider;
    private final ToolRegistry toolRegistry;
    private final AgentMemory memory;
    private final PermissionEngine permissionEngine;
    private final AuditService auditService;

    /**
     * 创建默认 Agent 执行器。
     *
     * <p>所有依赖通过构造函数注入，不依赖 Spring 或其他 DI 框架。
     * 这样 core 可以保持纯 Java，上层通过 Starter 或手动组装接入。</p>
     *
     * @param modelProvider 模型供应商适配器
     * @param toolRegistry  Tool 注册中心
     * @param memory        会话记忆
     * @param permissionEngine 权限检查引擎
     * @param auditService  审计记录服务
     */
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

    /**
     * 执行一次非流式 Agent 对话。
     *
     * <p>实现思路：
     * <ol>
     *   <li>保存用户输入到 Memory，构建包含消息历史和可用 Tool 的 ModelRequest</li>
     *   <li>调用模型：让模型在"直接回答"和"请求 ToolCall"之间做决策</li>
     *   <li>如果模型返回文本，直接保存并返回</li>
     *   <li>如果模型返回 ToolCall，顺序执行每个 Tool（经过权限和审计）</li>
     *   <li>把 Tool 执行结果交给模型，由模型生成最终回答</li>
     *   <li>保存助手回答到 Memory，返回 AgentResponse</li>
     * </ol>
     *
     * <p>异常处理：任何 RuntimeException 都会被捕获并转为 AgentResponse.error，
     * 避免未预期的异常直接抛给调用方。</p>
     *
     * @param request 用户请求，包含 sessionId、userId、message
     * @param context 执行上下文，包含用户信息
     * @return 执行结果，包含回答文本、错误信息和 Tool 调用摘要
     */
    @Override
    public AgentResponse run(AgentRequest request, AgentContext context) {
        List<ToolCallResult> toolCallResults = new ArrayList<ToolCallResult>();
        try {
            // 先保存用户输入，后续轮次可以从 Memory 中加载上下文。
            memory.save(request.getSessionId(), new AgentMessage("user", request.getMessage()));
            ModelRequest modelRequest = buildModelRequest(request);

            // 第一次调用模型：让模型在“直接回答”和“请求 ToolCall”之间做决策。
            // modelProvider 是接口，实际实现由 Spring Bean 或测试代码注入，可能是 Echo、OpenAI-compatible、
            // Anthropic-compatible，也可能是业务样板里的规则型 ModelProvider。
            ModelResponse modelResponse = modelProvider.chat(modelRequest);

            // 如果模型没有返回 ToolCall，说明本轮无需访问业务能力，直接把答案落 Memory 后返回。
            if (!modelResponse.hasToolCalls()) {
                String answer = modelResponse.getAnswer();
                memory.save(request.getSessionId(), new AgentMessage("assistant", answer));
                return AgentResponse.ok(answer, toolCallResults);
            }

            // 模型可以一次返回多个 ToolCall。Runtime 顺序执行它们，并把每个执行结果收集起来。
            // 这里不让模型直接调用业务接口，而是通过 ToolRegistry 匹配已注册 Tool，再走统一安全边界。
            List<ToolExecutionResult> toolExecutionResults = new ArrayList<ToolExecutionResult>();
            for (ToolCall toolCall : modelResponse.getToolCalls()) {
                ToolResult toolResult = executeTool(request, context, toolCall, toolCallResults);
                if (!toolResult.isSuccess()) {
                    return AgentResponse.error(toolResult.getErrorMessage());
                }
                toolExecutionResults.add(new ToolExecutionResult(toolCall, toolResult));
            }

            // Tool 执行成功后，把结果交回模型，由模型生成面向用户的最终回答。
            // Runtime 同时保留 lastToolExecutions 和兼容字段 lastToolCall / lastToolResult，便于不同 provider
            // 或旧测试按自己的能力读取 Tool 结果。
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

    /**
     * 执行一次流式 Agent 对话。
     *
     * <p>实现思路：
     * <ol>
     *   <li>保存用户输入到 Memory</li>
     *   <li>如果没有注册 Tool，直接流式透传模型输出</li>
     *   <li>如果有 Tool，先收集模型的流式决策（可能包含 ToolCall 分片）</li>
     *   <li>聚合完整 ToolCall 后执行 Tool，通过 listener 推送 Tool 状态</li>
     *   <li>Tool 执行完成后，发起第二次流式调用让模型总结</li>
     *   <li>如果流式决策阶段没有 ToolCall，直接把已收集文本作为回答</li>
     * </ol>
     *
     * <p>与非流式版本共享同一套 Tool、权限和审计规则，只是通过 listener 逐步输出。</p>
     *
     * @param request  用户请求
     * @param context  执行上下文
     * @param listener 流式输出回调，接收 delta、toolCall、complete、error 事件
     */
    @Override
    public void runStream(AgentRequest request, AgentContext context, AgentStreamListener listener) {
        List<ToolCallResult> toolCallResults = new ArrayList<ToolCallResult>();
        try {
            memory.save(request.getSessionId(), new AgentMessage("user", request.getMessage()));
            ModelRequest modelRequest = buildModelRequest(request);

            // 没有任何 Tool 注册时，不需要先等待完整模型决策，直接把模型输出流式透传给调用方。
            if (modelRequest.getTools().isEmpty()) {
                streamModelAnswer(request, modelRequest, listener);
                return;
            }

            // 有 Tool 时先收集模型的流式决策。部分 provider 会在流中返回 ToolCall 分片，
            // Runtime 需要先聚合出完整 ToolCall，执行 Tool 后再发起第二次流式总结。
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

            // 如果流式决策阶段没有 ToolCall，就把已收集的文本作为普通回答处理。
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

    /**
     * 收集模型的流式决策。
     *
     * <p>部分模型会边输出文本边输出 ToolCall 分片。Runtime 先把这些事件聚合到 StreamModelDecision 里，
     * 再决定是直接返回文本，还是执行 Tool 后进入第二次总结。</p>
     *
     * <p>ModelProvider.streamChat 是统一流式接口。真实 provider 可以覆盖它；未覆盖时会退化为 chat，
     * 仍然通过 listener 输出完整答案或 ToolCall，保证 Runtime 不需要区分 provider 新旧能力。</p>
     *
     * @param modelRequest 模型请求
     * @return 聚合后的流式决策结果
     */
    private StreamModelDecision streamModelDecision(ModelRequest modelRequest) {
        final StreamModelDecision decision = new StreamModelDecision();
        // ModelProvider.streamChat 是统一流式接口。真实 provider 可以覆盖它；未覆盖时会退化为 chat，
        // 仍然通过 listener 输出完整答案或 ToolCall，保证 Runtime 不需要区分 provider 新旧能力。
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

    /**
     * 流式输出模型最终回答。
     *
     * <p>这个方法只处理"最终回答"的流式输出。Tool 执行阶段已经完成后，模型输出的每个 delta
     * 可以安全转发给调用方，并在 complete 时保存完整 assistant 消息到 Memory。</p>
     *
     * @param request      原始用户请求（用于获取 sessionId）
     * @param modelRequest 模型请求（包含 Tool 执行结果）
     * @param listener     流式输出回调
     */
    private void streamModelAnswer(final AgentRequest request,
                                   ModelRequest modelRequest,
                                   final AgentStreamListener listener) {
        final StringBuilder answer = new StringBuilder();
        // 这个方法只处理“最终回答”的流式输出。Tool 执行阶段已经完成后，模型输出的每个 delta
        // 可以安全转发给调用方，并在 complete 时保存完整 assistant 消息。
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

    /**
     * 构建模型请求。
     *
     * <p>把用户请求转换为 ModelProvider 需要的 ModelRequest，包括：
     * <ul>
     *   <li>sessionId：用于 Memory 加载历史消息</li>
     *   <li>userMessage：当前用户输入</li>
     *   <li>messages：从 Memory 加载的历史消息</li>
     *   <li>tools：从 ToolRegistry 获取的可用 Tool 列表</li>
     *   <li>systemPrompt：只在有 Tool 时注入工具选择约束，避免无 Tool 场景产生不必要的工具调用暗示</li>
     * </ul>
     *
     * @param request 用户请求
     * @return 模型请求
     */
    private ModelRequest buildModelRequest(AgentRequest request) {
        ModelRequest modelRequest = new ModelRequest();
        modelRequest.setSessionId(request.getSessionId());
        modelRequest.setUserMessage(request.getMessage());
        modelRequest.setMessages(memory.load(request.getSessionId()));
        modelRequest.setTools(toolRegistry.list());
        if (!toolRegistry.list().isEmpty()) {
            // 只在存在 Tool 时注入工具选择约束。无 Tool 场景保持普通对话请求尽量干净，
            // 避免对模型产生不必要的工具调用暗示。
            modelRequest.setSystemPrompt(buildToolSelectionPrompt());
        }
        return modelRequest;
    }

    /**
     * 构建工具选择约束提示词。
     *
     * <p>这段提示词是 MVP 阶段的最小防误触发约束。它告诉模型：
     * <ul>
     *   <li>只有当用户明确需要查询数据时才使用工具</li>
     *   <li>不要在闲聊、介绍、解释类问题中调用工具</li>
     * </ul>
     *
     * <p>真正的安全边界仍在 Runtime：即使模型误返回 ToolCall，也必须通过 ToolRegistry、
     * READ 限制、参数校验和权限校验。</p>
     *
     * @return 系统提示词
     */
    private String buildToolSelectionPrompt() {
        StringBuilder sb = new StringBuilder();
        // 这段提示词是 MVP 阶段的最小防误触发约束。真正的安全边界仍在 Runtime：
        // 即使模型误返回 ToolCall，也必须通过 ToolRegistry、READ 限制、参数校验和权限校验。
        sb.append("你是一个智能助手。");
        sb.append("只有当用户的请求明确需要查询数据时才使用工具，不要在闲聊、介绍、解释类问题中调用工具。");
        sb.append("如果用户没有明确要求查询某个具体数据，请直接回答，不要调用任何工具。");
        return sb.toString();
    }

    /**
     * 受控执行单个 Tool。
     *
     * <p>这是 agent-core 安全边界的核心方法。执行流程：
     * <ol>
     *   <li>通过 ToolRegistry 查找模型请求的 Tool（找不到说明模型越界）</li>
     *   <li>校验 Tool 风险等级（MVP 只允许 READ）</li>
     *   <li>校验必填参数（确保 PermissionEngine 拿到结构完整的 ToolContext）</li>
     *   <li>调用 PermissionEngine 检查权限</li>
     *   <li>执行 Tool.execute()</li>
     *   <li>记录审计事件（无论成功或失败）</li>
     * </ol>
     *
     * <p>异常处理：Tool 查找、参数校验、权限拒绝或业务执行异常都统一转成 ToolResult.error，
     * 调用方可以得到结构化失败结果，同时 finally 仍会写入审计。</p>
     *
     * @param request          原始用户请求
     * @param context          执行上下文
     * @param toolCall         模型返回的 Tool 调用请求
     * @param toolCallResults  用于收集 Tool 调用摘要的列表
     * @return Tool 执行结果
     */
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
            // 模型只能通过名称请求 Tool。Runtime 必须在本地注册中心中找到同名 Tool，才允许继续。
            // 找不到说明模型越界或配置不一致，直接拒绝执行。
            Optional<AgentTool> optionalTool = toolRegistry.get(toolCall.getName());
            if (!optionalTool.isPresent()) {
                throw new IllegalArgumentException("Tool not found: " + toolCall.getName());
            }

            AgentTool tool = optionalTool.get();
            // MVP 只允许 READ Tool。这个限制放在 Runtime，而不是只写在文档里，
            // 是为了防止业务侧误注册高风险 Tool 后被模型直接触发。
            validateReadOnly(tool);
            // 必填参数在权限检查前校验，确保 PermissionEngine 拿到的是结构完整的 ToolContext。
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
            // Tool 查找、参数校验、权限拒绝或业务执行异常都统一转成 ToolResult.error，
            // 调用方可以得到结构化失败结果，同时 finally 仍会写入审计。
            auditEvent.setSuccess(false);
            auditEvent.setErrorMessage(ex.getMessage());
            toolCallResults.add(new ToolCallResult(toolCall.getName(), false, ex.getMessage()));
            return ToolResult.error(ex.getMessage());
        } finally {
            auditEvent.setLatencyMs(System.currentTimeMillis() - startedAt);
            auditService.record(auditEvent);
        }
    }

    /**
     * 校验 Tool 风险等级。
     *
     * <p>MVP 阶段只允许 READ 级别的 Tool。这个限制放在 Runtime，而不是只写在文档里，
     * 是为了防止业务侧误注册高风险 Tool 后被模型直接触发。</p>
     *
     * @param tool 待校验的 Tool
     * @throws IllegalStateException 如果 Tool 不是 READ 级别
     */
    private void validateReadOnly(AgentTool tool) {
        if (tool.riskLevel() != ToolRiskLevel.READ) {
            throw new IllegalStateException("Only READ tools are enabled in MVP: " + tool.name());
        }
    }

    /**
     * 校验 Tool 必填参数。
     *
     * <p>必填参数在权限检查前校验，确保 PermissionEngine 拿到的是结构完整的 ToolContext。
     * 这样权限判断可以安全地基于参数内容做决策，不用担心缺失字段导致 NPE。</p>
     *
     * @param schema    Tool 参数 Schema
     * @param arguments 模型生成的参数
     * @throws IllegalArgumentException 如果缺少必填参数
     */
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

    /**
     * 流式模型决策的临时聚合对象。
     *
     * <p>部分模型会边输出文本边输出 ToolCall 分片。Runtime 先把这些事件聚合到本对象里，
     * 再决定是直接返回文本，还是执行 Tool 后进入第二次总结。</p>
     */
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
