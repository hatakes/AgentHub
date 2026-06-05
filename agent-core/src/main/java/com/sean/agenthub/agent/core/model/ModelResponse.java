package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型供应商返回结果，可以是文本回答，也可以是 Tool 调用请求。
 *
 * @author Sean
 */
public class ModelResponse {
    private String answer;
    private List<ToolCall> toolCalls = new ArrayList<ToolCall>();

    public static ModelResponse answer(String answer) {
        ModelResponse response = new ModelResponse();
        response.setAnswer(answer);
        return response;
    }

    public static ModelResponse toolCall(ToolCall toolCall) {
        ModelResponse response = new ModelResponse();
        response.getToolCalls().add(toolCall);
        return response;
    }

    public static ModelResponse toolCalls(List<ToolCall> toolCalls) {
        ModelResponse response = new ModelResponse();
        response.setToolCalls(toolCalls);
        return response;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls == null ? new ArrayList<ToolCall>() : toolCalls;
    }
}
