package com.sean.agenthub.agent.mcp;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP tools/call 最小请求模型。
 *
 * <p>包含 Tool 名称、参数和调用方身份信息，用于 MCP 适配器执行 Tool。</p>
 *
 * @author Sean
 */
public class McpToolCallRequest {
    /** Tool 名称。 */
    private String name;
    /** Tool 调用参数。 */
    private Map<String, Object> arguments = new HashMap<String, Object>();
    /** 会话 ID，用于审计关联。 */
    private String sessionId;
    /** 用户 ID，用于权限检查和审计。 */
    private String userId;
    /** 用户扩展属性，用于权限检查。 */
    private Map<String, Object> userAttributes = new HashMap<String, Object>();

    /** 创建空的请求。 */
    public McpToolCallRequest() {
    }

    /**
     * 创建完整的请求。
     *
     * @param name      Tool 名称
     * @param arguments Tool 参数
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     */
    public McpToolCallRequest(String name, Map<String, Object> arguments, String sessionId, String userId) {
        this.name = name;
        setArguments(arguments);
        this.sessionId = sessionId;
        this.userId = userId;
    }

    /**
     * 获取 Tool 名称。
     *
     * @return Tool 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置 Tool 名称。
     *
     * @param name Tool 名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取 Tool 参数。
     *
     * @return 参数映射
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * 设置 Tool 参数，null 会被替换为空 Map。
     *
     * @param arguments 参数映射
     */
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments == null ? new HashMap<String, Object>() : arguments;
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
     * 获取用户扩展属性。
     *
     * @return 扩展属性映射
     */
    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    /**
     * 设置用户扩展属性，null 会被替换为空 Map。
     *
     * @param userAttributes 扩展属性映射
     */
    public void setUserAttributes(Map<String, Object> userAttributes) {
        this.userAttributes = userAttributes == null ? new HashMap<String, Object>() : userAttributes;
    }
}
