package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 当前用户上下文，后续可扩展角色、组织、数据权限等信息。
 *
 * <p>由业务系统在请求入口处构建，传入 AgentContext，供 PermissionEngine 和 Tool 使用。</p>
 *
 * @author Sean
 */
public class UserContext {
    /** 用户 ID。 */
    private String userId;
    /** 用户扩展属性，如角色、部门、数据权限范围等。 */
    private Map<String, Object> attributes = new HashMap<String, Object>();

    /** 创建空的用户上下文。 */
    public UserContext() {
    }

    /**
     * 创建指定用户 ID 的上下文。
     *
     * @param userId 用户 ID
     */
    public UserContext(String userId) {
        this.userId = userId;
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
     * 获取用户扩展属性。
     *
     * @return 扩展属性映射
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 设置用户扩展属性，null 会被替换为空 Map。
     *
     * @param attributes 扩展属性映射
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes == null ? new HashMap<String, Object>() : attributes;
    }
}
