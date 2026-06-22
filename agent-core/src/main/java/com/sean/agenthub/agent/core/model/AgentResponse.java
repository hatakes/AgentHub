package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 对话响应，包含最终回答、错误信息和 Tool 调用摘要。
 *
 * <p>通过静态工厂方法 {@link #ok} 和 {@link #error} 创建成功/失败响应。</p>
 *
 * @author Sean
 */
public class AgentResponse {
    /** 是否执行成功。 */
    private boolean ok;
    /** 模型生成的最终回答文本。 */
    private String answer;
    /** 错误信息，仅在 ok=false 时有值。 */
    private String errorMessage;
    /** 本次对话中所有 Tool 调用的摘要列表。 */
    private List<ToolCallResult> toolCalls = new ArrayList<ToolCallResult>();

    /**
     * 创建成功的响应。
     *
     * @param answer     模型回答文本
     * @param toolCalls  Tool 调用摘要列表
     * @return 成功响应
     */
    public static AgentResponse ok(String answer, List<ToolCallResult> toolCalls) {
        AgentResponse response = new AgentResponse();
        response.setOk(true);
        response.setAnswer(answer);
        response.setToolCalls(toolCalls);
        return response;
    }

    /**
     * 创建失败的响应。
     *
     * @param errorMessage 错误信息
     * @return 失败响应
     */
    public static AgentResponse error(String errorMessage) {
        AgentResponse response = new AgentResponse();
        response.setOk(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    /**
     * 判断是否执行成功。
     *
     * @return 成功返回 true
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * 设置执行状态。
     *
     * @param ok 是否成功
     */
    public void setOk(boolean ok) {
        this.ok = ok;
    }

    /**
     * 获取模型回答文本。
     *
     * @return 回答文本
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * 设置模型回答文本。
     *
     * @param answer 回答文本
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息，成功时为 null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置错误信息。
     *
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 获取 Tool 调用摘要列表。
     *
     * @return Tool 调用摘要
     */
    public List<ToolCallResult> getToolCalls() {
        return toolCalls;
    }

    /**
     * 设置 Tool 调用摘要列表，null 会被替换为空列表。
     *
     * @param toolCalls Tool 调用摘要
     */
    public void setToolCalls(List<ToolCallResult> toolCalls) {
        this.toolCalls = toolCalls == null ? new ArrayList<ToolCallResult>() : toolCalls;
    }
}
