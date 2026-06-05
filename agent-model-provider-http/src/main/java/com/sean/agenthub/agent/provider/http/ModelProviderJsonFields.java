package com.sean.agenthub.agent.provider.http;

/**
 * HTTP 模型协议 JSON 字段和值常量。
 *
 * <p>包内实现细节，仅用于避免 OpenAI / Anthropic 载荷转换逻辑散落魔法值；不作为模块公开 API 暴露。</p>
 *
 * @author Sean
 */
final class ModelProviderJsonFields {
    static final String ARGUMENTS = "arguments";
    static final String ASSISTANT = "assistant";
    static final String CALL_ID_PREFIX = "call_";
    static final String CONTENT = "content";
    static final String DESCRIPTION = "description";
    static final String ENUM = "enum";
    static final String FUNCTION = "function";
    static final String ID = "id";
    static final String INPUT = "input";
    static final String INPUT_SCHEMA = "input_schema";
    static final String JSON_SCHEMA = "json_schema";
    static final String NAME = "name";
    static final String OBJECT = "object";
    static final String PARAMETERS = "parameters";
    static final String PROPERTIES = "properties";
    static final String REQUIRED = "required";
    static final String ROLE = "role";
    static final String SCHEMA = "schema";
    static final String STRICT = "strict";
    static final String SYSTEM = "system";
    static final String TOOL = "tool";
    static final String TOOL_CALL_ID = "tool_call_id";
    static final String TOOL_CALLS = "tool_calls";
    static final String TOOL_RESULT = "tool_result";
    static final String TOOL_USE = "tool_use";
    static final String TOOL_USE_ID = "tool_use_id";
    static final String TYPE = "type";
    static final String USER = "user";

    private ModelProviderJsonFields() {
    }
}
