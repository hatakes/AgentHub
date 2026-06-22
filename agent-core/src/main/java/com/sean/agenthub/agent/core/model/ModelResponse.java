package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型供应商返回结果，可以是文本回答，也可以是 Tool 调用请求。
 *
 * <p>通过静态工厂方法创建：{@link #answer} 创建文本响应，
 * {@link #toolCall} / {@link #toolCalls} 创建 ToolCall 响应。</p>
 *
 * @author Sean
 */
public class ModelResponse {
    /** 模型生成的文本回答。 */
    private String answer;
    /** 模型请求的 Tool 调用列表。 */
    private List<ToolCall> toolCalls = new ArrayList<ToolCall>();

    /**
     * 创建文本回答响应。
     *
     * @param answer 模型回答文本
     * @return 文本响应
     */
    public static ModelResponse answer(String answer) {
        ModelResponse response = new ModelResponse();
        response.setAnswer(answer);
        return response;
    }

    /**
     * 创建单个 ToolCall 响应。
     *
     * @param toolCall Tool 调用描述
     * @return ToolCall 响应
     */
    public static ModelResponse toolCall(ToolCall toolCall) {
        ModelResponse response = new ModelResponse();
        response.getToolCalls().add(toolCall);
        return response;
    }

    /**
     * 创建多个 ToolCall 响应。
     *
     * @param toolCalls Tool 调用列表
     * @return ToolCall 响应
     */
    public static ModelResponse toolCalls(List<ToolCall> toolCalls) {
        ModelResponse response = new ModelResponse();
        response.setToolCalls(toolCalls);
        return response;
    }

    /**
     * 判断是否包含 Tool 调用请求。
     *
     * @return 有 ToolCall 返回 true
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * 获取文本回答。
     *
     * @return 回答文本
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * 设置文本回答。
     *
     * @param answer 回答文本
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * 获取 Tool 调用列表。
     *
     * @return ToolCall 列表
     */
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    /**
     * 设置 Tool 调用列表，null 会被替换为空列表。
     *
     * @param toolCalls ToolCall 列表
     */
    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls == null ? new ArrayList<ToolCall>() : toolCalls;
    }
}
