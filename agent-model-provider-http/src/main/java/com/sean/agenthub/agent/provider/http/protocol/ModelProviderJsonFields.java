package com.sean.agenthub.agent.provider.http.protocol;

/**
 * HTTP 模型协议 JSON 字段和值常量。
 *
 * <p>包内实现细节，仅用于避免 OpenAI / Anthropic 载荷转换逻辑散落魔法值；不作为模块公开 API 暴露。</p>
 *
 * @author Sean
 */
public final class ModelProviderJsonFields {
    public static final String ANTHROPIC_VERSION = "anthropic-version";
    public static final String ARGUMENTS = "arguments";
    public static final String ASSISTANT = "assistant";
    public static final String AUTHORIZATION = "Authorization";
    public static final String AUTO = "auto";
    public static final String ANY = "any";
    public static final String CALL_ID_PREFIX = "call_";
    public static final String CHOICES = "choices";
    public static final String CONTENT = "content";
    public static final String CONTENT_BLOCK = "content_block";
    public static final String CONTENT_BLOCK_DELTA = "content_block_delta";
    public static final String CONTENT_BLOCK_START = "content_block_start";
    public static final String DELTA = "delta";
    public static final String DESCRIPTION = "description";
    public static final String ENUM = "enum";
    public static final String ERROR = "error";
    public static final String FUNCTION = "function";
    public static final String ID = "id";
    public static final String INDEX = "index";
    public static final String INPUT = "input";
    public static final String INPUT_JSON_DELTA = "input_json_delta";
    public static final String INPUT_SCHEMA = "input_schema";
    public static final String JSON_SCHEMA = "json_schema";
    public static final String MAX_TOKENS = "max_tokens";
    public static final String MESSAGE = "message";
    public static final String MESSAGES = "messages";
    public static final String MODEL = "model";
    public static final String NAME = "name";
    public static final String NONE = "none";
    public static final String OBJECT = "object";
    public static final String PARAMETERS = "parameters";
    public static final String PARTIAL_JSON = "partial_json";
    public static final String PROPERTIES = "properties";
    public static final String REQUIRED = "required";
    public static final String REQUIRED_CHOICE = "required";
    public static final String RESPONSE_FORMAT = "response_format";
    public static final String ROLE = "role";
    public static final String SCHEMA = "schema";
    public static final String STRICT = "strict";
    public static final String STREAM = "stream";
    public static final String SYSTEM = "system";
    public static final String TEXT = "text";
    public static final String TEXT_DELTA = "text_delta";
    public static final String TOOL = "tool";
    public static final String TOOL_CALL_ID = "tool_call_id";
    public static final String TOOL_CALLS = "tool_calls";
    public static final String TOOL_CHOICE = "tool_choice";
    public static final String TOOL_RESULT = "tool_result";
    public static final String TOOLS = "tools";
    public static final String TOOL_USE = "tool_use";
    public static final String TOOL_USE_ID = "tool_use_id";
    public static final String TYPE = "type";
    public static final String USER = "user";
    public static final String X_API_KEY = "x-api-key";

    private ModelProviderJsonFields() {
    }
}
