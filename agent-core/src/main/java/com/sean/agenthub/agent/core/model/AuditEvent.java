package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.Date;

/**
 * 审计事件模型，记录一次 Tool 调用的关键上下文和结果。
 *
 * <p>每次 Tool 执行（无论成功或失败）都会生成一个 AuditEvent，由 AuditService 持久化。
 * traceId 用于关联同一次 Agent 请求中的多次 Tool 调用。</p>
 *
 * @author Sean
 */
public class AuditEvent {
    /** 调用链追踪 ID，同一次 Agent 请求共享。 */
    private String traceId;
    /** 会话 ID，关联 AgentRequest.sessionId。 */
    private String sessionId;
    /** 发起请求的用户 ID。 */
    private String userId;
    /** 被调用的 Tool 名称。 */
    private String toolName;
    /** 用户原始请求摘要。 */
    private String requestSummary;
    /** Tool 执行结果摘要（截断后的 data.toString）。 */
    private String toolResultSummary;
    /** Tool 执行耗时（毫秒）。 */
    private long latencyMs;
    /** Tool 执行是否成功。 */
    private boolean success;
    /** 错误信息，仅在 success=false 时有值。 */
    private String errorMessage;
    /** 审计事件创建时间。 */
    private Date createdAt = new Date();

    /**
     * 获取追踪 ID。
     *
     * @return 追踪 ID
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * 设置追踪 ID。
     *
     * @param traceId 追踪 ID
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

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
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取 Tool 名称。
     *
     * @return Tool 名称
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * 设置 Tool 名称。
     *
     * @param toolName Tool 名称
     */
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    /**
     * 获取请求摘要。
     *
     * @return 请求摘要
     */
    public String getRequestSummary() {
        return requestSummary;
    }

    /**
     * 设置请求摘要。
     *
     * @param requestSummary 请求摘要
     */
    public void setRequestSummary(String requestSummary) {
        this.requestSummary = requestSummary;
    }

    /**
     * 获取 Tool 执行结果摘要。
     *
     * @return 结果摘要
     */
    public String getToolResultSummary() {
        return toolResultSummary;
    }

    /**
     * 设置 Tool 执行结果摘要。
     *
     * @param toolResultSummary 结果摘要
     */
    public void setToolResultSummary(String toolResultSummary) {
        this.toolResultSummary = toolResultSummary;
    }

    /**
     * 获取执行耗时（毫秒）。
     *
     * @return 耗时毫秒数
     */
    public long getLatencyMs() {
        return latencyMs;
    }

    /**
     * 设置执行耗时。
     *
     * @param latencyMs 耗时毫秒数
     */
    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    /**
     * 判断执行是否成功。
     *
     * @return 成功返回 true
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置执行状态。
     *
     * @param success 是否成功
     */
    public void setSuccess(boolean success) {
        this.success = success;
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
     * 获取事件创建时间。
     *
     * @return 创建时间
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置事件创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
