package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.Date;

/**
 * 会话消息模型，用于 Memory 保存用户和助手历史消息。
 *
 * <p>每条消息包含角色（user/assistant/tool）和内容文本，createdAt 用于消息排序。</p>
 *
 * @author Sean
 */
public class AgentMessage {
    /** 消息角色，取值为 "user"、"assistant" 或 "tool"。 */
    private String role;
    /** 消息文本内容。 */
    private String content;
    /** 消息创建时间戳，用于 Memory 排序。 */
    private Date createdAt = new Date();

    /** 创建空的消息对象。 */
    public AgentMessage() {
    }

    /**
     * 创建指定角色和内容的消息。
     *
     * @param role    消息角色
     * @param content 消息内容
     */
    public AgentMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * 获取消息角色。
     *
     * @return 消息角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置消息角色。
     *
     * @param role 消息角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取消息内容。
     *
     * @return 消息文本
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容。
     *
     * @param content 消息文本
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取消息创建时间。
     *
     * @return 创建时间
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置消息创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
