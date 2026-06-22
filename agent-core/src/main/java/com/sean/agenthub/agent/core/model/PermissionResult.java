package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 权限检查结果，表达允许或拒绝及原因。
 *
 * <p>通过静态工厂方法 {@link #allowed()} 和 {@link #denied(String)} 创建。</p>
 *
 * @author Sean
 */
public class PermissionResult {
    /** 是否允许执行。 */
    private boolean allowed;
    /** 拒绝原因，仅在 allowed=false 时有值。 */
    private String reason;

    /**
     * 创建允许的权限结果。
     *
     * @return 允许结果
     */
    public static PermissionResult allowed() {
        PermissionResult result = new PermissionResult();
        result.setAllowed(true);
        return result;
    }

    /**
     * 创建拒绝的权限结果。
     *
     * @param reason 拒绝原因
     * @return 拒绝结果
     */
    public static PermissionResult denied(String reason) {
        PermissionResult result = new PermissionResult();
        result.setAllowed(false);
        result.setReason(reason);
        return result;
    }

    /**
     * 判断是否允许执行。
     *
     * @return 允许返回 true
     */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * 设置是否允许。
     *
     * @param allowed 是否允许
     */
    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    /**
     * 获取拒绝原因。
     *
     * @return 拒绝原因，允许时为 null
     */
    public String getReason() {
        return reason;
    }

    /**
     * 设置拒绝原因。
     *
     * @param reason 拒绝原因
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
