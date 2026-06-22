package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 发送给模型供应商的请求对象。
 *
 * <p>由 Runtime 构建，包含消息历史、可用 Tool 列表、系统提示词等。
 * provider 负责把它转换为具体协议格式（OpenAI / Anthropic 等）。</p>
 *
 * @author Sean
 */
public class ModelRequest {
    /** 会话 ID，用于 Memory 加载历史消息。 */
    private String sessionId;
    /** 当前用户输入消息。 */
    private String userMessage;
    /** 系统提示词，用于约束模型行为。 */
    private String systemPrompt;
    /** Tool 选择策略（auto/none/required/指定 Tool 名称）。 */
    private String toolChoice;
    /** 响应格式约束，用于 Structured Output。 */
    private ResponseFormat responseFormat;
    /** 从 Memory 加载的历史消息列表。 */
    private List<AgentMessage> messages = new ArrayList<AgentMessage>();
    /** 可用 Tool 列表，来自 ToolRegistry 快照。 */
    private List<AgentTool> tools = new ArrayList<AgentTool>();
    /** 最近一次 Tool 执行结果（兼容字段）。 */
    private ToolResult lastToolResult;
    /** 最近一次 Tool 调用描述（兼容字段）。 */
    private ToolCall lastToolCall;
    /** 本次所有 Tool 执行结果，用于模型总结阶段回传上下文。 */
    private List<ToolExecutionResult> lastToolExecutions = new ArrayList<ToolExecutionResult>();

    /**
     * 获取会话 ID。
     *
     * @return 会话 ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话 ID。
     *
     * @param sessionId 会话 ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取当前用户输入。
     *
     * @return 用户消息
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * 设置当前用户输入。
     *
     * @param userMessage 用户消息
     */
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    /**
     * 获取系统提示词。
     *
     * @return 系统提示词
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 设置系统提示词。
     *
     * @param systemPrompt 系统提示词
     */
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    /**
     * 获取 Tool 选择策略。
     *
     * @return Tool 选择策略
     */
    public String getToolChoice() {
        return toolChoice;
    }

    /**
     * 设置 Tool 选择策略。
     *
     * @param toolChoice Tool 选择策略
     */
    public void setToolChoice(String toolChoice) {
        this.toolChoice = toolChoice;
    }

    /**
     * 获取响应格式约束。
     *
     * @return 响应格式
     */
    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    /**
     * 设置响应格式约束。
     *
     * @param responseFormat 响应格式
     */
    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    /**
     * 获取历史消息列表。
     *
     * @return 历史消息
     */
    public List<AgentMessage> getMessages() {
        return messages;
    }

    /**
     * 设置历史消息列表，null 会被替换为空列表。
     *
     * @param messages 历史消息
     */
    public void setMessages(List<AgentMessage> messages) {
        this.messages = messages == null ? new ArrayList<AgentMessage>() : messages;
    }

    /**
     * 获取可用 Tool 列表。
     *
     * @return Tool 列表
     */
    public List<AgentTool> getTools() {
        return tools;
    }

    /**
     * 设置可用 Tool 列表，null 会被替换为空列表。
     *
     * @param tools Tool 列表
     */
    public void setTools(List<AgentTool> tools) {
        this.tools = tools == null ? new ArrayList<AgentTool>() : tools;
    }

    /**
     * 获取最近一次 Tool 执行结果（兼容字段）。
     *
     * @return Tool 执行结果
     */
    public ToolResult getLastToolResult() {
        return lastToolResult;
    }

    /**
     * 设置最近一次 Tool 执行结果。
     *
     * @param lastToolResult Tool 执行结果
     */
    public void setLastToolResult(ToolResult lastToolResult) {
        this.lastToolResult = lastToolResult;
    }

    /**
     * 获取最近一次 Tool 调用描述（兼容字段）。
     *
     * @return Tool 调用描述
     */
    public ToolCall getLastToolCall() {
        return lastToolCall;
    }

    /**
     * 设置最近一次 Tool 调用描述。
     *
     * @param lastToolCall Tool 调用描述
     */
    public void setLastToolCall(ToolCall lastToolCall) {
        this.lastToolCall = lastToolCall;
    }

    /**
     * 获取本次所有 Tool 执行结果。
     *
     * @return Tool 执行结果列表
     */
    public List<ToolExecutionResult> getLastToolExecutions() {
        return lastToolExecutions;
    }

    /**
     * 设置本次所有 Tool 执行结果，null 会被替换为空列表。
     *
     * @param lastToolExecutions Tool 执行结果列表
     */
    public void setLastToolExecutions(List<ToolExecutionResult> lastToolExecutions) {
        this.lastToolExecutions = lastToolExecutions == null
                ? new ArrayList<ToolExecutionResult>()
                : lastToolExecutions;
    }
}
