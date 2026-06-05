package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 对话响应，包含最终回答、错误信息和 Tool 调用摘要。
 *
 * @author Sean
 */
public class AgentResponse {
    private boolean ok;
    private String answer;
    private String errorMessage;
    private List<ToolCallResult> toolCalls = new ArrayList<ToolCallResult>();

    public static AgentResponse ok(String answer, List<ToolCallResult> toolCalls) {
        AgentResponse response = new AgentResponse();
        response.setOk(true);
        response.setAnswer(answer);
        response.setToolCalls(toolCalls);
        return response;
    }

    public static AgentResponse error(String errorMessage) {
        AgentResponse response = new AgentResponse();
        response.setOk(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<ToolCallResult> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallResult> toolCalls) {
        this.toolCalls = toolCalls == null ? new ArrayList<ToolCallResult>() : toolCalls;
    }
}
