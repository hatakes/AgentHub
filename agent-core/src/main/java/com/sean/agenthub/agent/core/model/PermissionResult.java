package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 权限检查结果，表达允许或拒绝及原因。
 *
 * @author Sean
 */
public class PermissionResult {
    private boolean allowed;
    private String reason;

    public static PermissionResult allowed() {
        PermissionResult result = new PermissionResult();
        result.setAllowed(true);
        return result;
    }

    public static PermissionResult denied(String reason) {
        PermissionResult result = new PermissionResult();
        result.setAllowed(false);
        result.setReason(reason);
        return result;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
