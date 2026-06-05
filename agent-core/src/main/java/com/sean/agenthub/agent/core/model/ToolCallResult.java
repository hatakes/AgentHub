package com.sean.agenthub.agent.core.model;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 返回给调用方的 Tool 调用摘要。
 *
 * @author Sean
 */
public class ToolCallResult {
    private String tool;
    private boolean success;
    private String errorMessage;

    public ToolCallResult() {
    }

    public ToolCallResult(String tool, boolean success, String errorMessage) {
        this.tool = tool;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
