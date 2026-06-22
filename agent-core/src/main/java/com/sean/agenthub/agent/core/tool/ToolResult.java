package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * Tool 执行结果，表达成功数据或错误原因。
 *
 * <p>通过静态工厂方法 {@link #success(Object)} 和 {@link #error(String)} 创建。</p>
 *
 * @author Sean
 */
public class ToolResult {
    /** 执行是否成功。 */
    private boolean success;
    /** 成功时的返回数据。 */
    private Object data;
    /** 错误信息，仅在 success=false 时有值。 */
    private String errorMessage;

    /**
     * 创建成功的 Tool 结果。
     *
     * @param data 返回数据
     * @return 成功结果
     */
    public static ToolResult success(Object data) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    /**
     * 创建失败的 Tool 结果。
     *
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static ToolResult error(String errorMessage) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * 判断是否成功。
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
     * 获取返回数据。
     *
     * @return 返回数据
     */
    public Object getData() {
        return data;
    }

    /**
     * 设置返回数据。
     *
     * @param data 返回数据
     */
    public void setData(Object data) {
        this.data = data;
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
}
