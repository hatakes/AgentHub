package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 对话请求，承载会话 ID、用户 ID、用户输入和扩展属性。
 *
 * <p>sessionId 用于 Memory 加载历史消息；userId 用于权限检查和审计记录；
 * attributes 用于传递业务扩展字段。</p>
 *
 * @author Sean
 */
public class AgentRequest {
    /** 会话 ID，用于 Memory 加载历史消息和审计关联。 */
    private String sessionId;
    /** 用户 ID，用于权限检查和审计记录。 */
    private String userId;
    /** 用户输入的自然语言消息。 */
    private String message;
    /** 扩展属性，用于传递业务自定义字段。 */
    private Map<String, Object> attributes = new HashMap<String, Object>();

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
     * 获取用户输入消息。
     *
     * @return 用户消息文本
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置用户输入消息。
     *
     * @param message 用户消息文本
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取扩展属性。
     *
     * @return 扩展属性映射
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 设置扩展属性，null 会被替换为空 Map。
     *
     * @param attributes 扩展属性映射
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes == null ? new HashMap<String, Object>() : attributes;
    }
}
