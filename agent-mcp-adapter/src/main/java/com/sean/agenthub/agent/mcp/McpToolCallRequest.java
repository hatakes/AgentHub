package com.sean.agenthub.agent.mcp;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP tools/call 最小请求模型。
 *
 * @author Sean
 */
public class McpToolCallRequest {
    private String name;
    private Map<String, Object> arguments = new HashMap<String, Object>();
    private String sessionId;
    private String userId;
    private Map<String, Object> userAttributes = new HashMap<String, Object>();

    public McpToolCallRequest() {
    }

    public McpToolCallRequest(String name, Map<String, Object> arguments, String sessionId, String userId) {
        this.name = name;
        setArguments(arguments);
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments == null ? new HashMap<String, Object>() : arguments;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(Map<String, Object> userAttributes) {
        this.userAttributes = userAttributes == null ? new HashMap<String, Object>() : userAttributes;
    }
}
