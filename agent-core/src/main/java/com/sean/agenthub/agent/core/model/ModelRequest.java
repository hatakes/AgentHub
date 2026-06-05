package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 发送给模型供应商的请求对象。
 *
 * @author Sean
 */
public class ModelRequest {
    private String sessionId;
    private String userMessage;
    private String systemPrompt;
    private String toolChoice;
    private ResponseFormat responseFormat;
    private List<AgentMessage> messages = new ArrayList<AgentMessage>();
    private List<AgentTool> tools = new ArrayList<AgentTool>();
    private ToolResult lastToolResult;
    private ToolCall lastToolCall;
    private List<ToolExecutionResult> lastToolExecutions = new ArrayList<ToolExecutionResult>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getToolChoice() {
        return toolChoice;
    }

    public void setToolChoice(String toolChoice) {
        this.toolChoice = toolChoice;
    }

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    public List<AgentMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AgentMessage> messages) {
        this.messages = messages == null ? new ArrayList<AgentMessage>() : messages;
    }

    public List<AgentTool> getTools() {
        return tools;
    }

    public void setTools(List<AgentTool> tools) {
        this.tools = tools == null ? new ArrayList<AgentTool>() : tools;
    }

    public ToolResult getLastToolResult() {
        return lastToolResult;
    }

    public void setLastToolResult(ToolResult lastToolResult) {
        this.lastToolResult = lastToolResult;
    }

    public ToolCall getLastToolCall() {
        return lastToolCall;
    }

    public void setLastToolCall(ToolCall lastToolCall) {
        this.lastToolCall = lastToolCall;
    }

    public List<ToolExecutionResult> getLastToolExecutions() {
        return lastToolExecutions;
    }

    public void setLastToolExecutions(List<ToolExecutionResult> lastToolExecutions) {
        this.lastToolExecutions = lastToolExecutions == null
                ? new ArrayList<ToolExecutionResult>()
                : lastToolExecutions;
    }
}
