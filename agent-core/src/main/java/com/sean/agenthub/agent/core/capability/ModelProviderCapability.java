package com.sean.agenthub.agent.core.capability;

/**
 * 模型供应商适配器能力声明。
 *
 * <p>用于比较 HTTP 直连、Spring AI、LangChain4j、Hutool AI 等不同实现的覆盖范围。</p>
 *
 * @author Sean
 */
public enum ModelProviderCapability {
    /**
     * 非流式文本对话。
     */
    TEXT_CHAT,

    /**
     * 流式文本输出。
     */
    TEXT_STREAM,

    /**
     * 流式响应中可返回 Tool 调用请求。
     */
    STREAM_TOOL_CALL,

    /**
     * 模型可返回 Tool 调用请求。
     */
    TOOL_CALL,

    /**
     * 模型可在同一轮返回多个 Tool 调用请求。
     */
    MULTI_TOOL_CALL,

    /**
     * Tool 执行结果可按模型协议格式回传给模型。
     */
    TOOL_RESULT_MESSAGES,

    /**
     * 模型支持强约束结构化输出。
     */
    STRUCTURED_OUTPUT,

    /**
     * 适配器支持 MCP Tool / Resource / Prompt 生态互通。
     */
    MCP_INTEROP
}
