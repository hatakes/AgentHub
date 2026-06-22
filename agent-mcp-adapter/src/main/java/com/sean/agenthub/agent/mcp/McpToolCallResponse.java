package com.sean.agenthub.agent.mcp;

/**
 * MCP tools/call 最小响应模型。
 *
 * <p>通过静态工厂方法 {@link #success(Object)} 和 {@link #error(String)} 创建。</p>
 *
 * @author Sean
 */
public class McpToolCallResponse {
    /** 执行是否成功。 */
    private boolean success;
    /** 成功时的返回数据。 */
    private Object result;
    /** 错误信息，仅在 success=false 时有值。 */
    private String errorMessage;

    /**
     * 创建成功的响应。
     *
     * @param result 返回数据
     * @return 成功响应
     */
    public static McpToolCallResponse success(Object result) {
        McpToolCallResponse response = new McpToolCallResponse();
        response.setSuccess(true);
        response.setResult(result);
        return response;
    }

    /**
     * 创建失败的响应。
     *
     * @param errorMessage 错误信息
     * @return 失败响应
     */
    public static McpToolCallResponse error(String errorMessage) {
        McpToolCallResponse response = new McpToolCallResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
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
    public Object getResult() {
        return result;
    }

    /**
     * 设置返回数据。
     *
     * @param result 返回数据
     */
    public void setResult(Object result) {
        this.result = result;
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
