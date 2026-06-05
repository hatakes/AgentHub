package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 当前用户上下文，后续可扩展角色、组织、数据权限等信息。
 *
 * @author Sean
 */
public class UserContext {
    private String userId;
    private Map<String, Object> attributes = new HashMap<String, Object>();

    public UserContext() {
    }

    public UserContext(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes == null ? new HashMap<String, Object>() : attributes;
    }
}
