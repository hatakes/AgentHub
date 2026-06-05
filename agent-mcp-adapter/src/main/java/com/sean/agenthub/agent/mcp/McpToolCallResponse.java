package com.sean.agenthub.agent.mcp;

/**
 * MCP tools/call 最小响应模型。
 *
 * @author Sean
 */
public class McpToolCallResponse {
    private boolean success;
    private Object result;
    private String errorMessage;

    public static McpToolCallResponse success(Object result) {
        McpToolCallResponse response = new McpToolCallResponse();
        response.setSuccess(true);
        response.setResult(result);
        return response;
    }

    public static McpToolCallResponse error(String errorMessage) {
        McpToolCallResponse response = new McpToolCallResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
