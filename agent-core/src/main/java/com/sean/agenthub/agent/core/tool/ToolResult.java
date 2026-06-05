package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * Tool 执行结果，表达成功数据或错误原因。
 *
 * @author Sean
 */
public class ToolResult {
    private boolean success;
    private Object data;
    private String errorMessage;

    public static ToolResult success(Object data) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static ToolResult error(String errorMessage) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
