package com.sean.agenthub.agent.provider.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ResponseFormat;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.core.model.ToolExecutionResult;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.test.ModelProviderContract;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * OpenAI 兼容协议适配器测试。
 *
 * @author Sean
 */
public class OpenAiCompatibleModelProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String requestPath;
    private String authorization;
    private String requestBody;

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void shouldDeclareOpenAiCapabilities() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");

        OpenAiCompatibleModelProvider provider = provider();

        ModelProviderContract.assertCapabilities(
                provider,
                new ModelProviderCapability[] {
                        ModelProviderCapability.TEXT_CHAT,
                        ModelProviderCapability.TEXT_STREAM,
                        ModelProviderCapability.STREAM_TOOL_CALL,
                        ModelProviderCapability.TOOL_CALL,
                        ModelProviderCapability.MULTI_TOOL_CALL,
                        ModelProviderCapability.TOOL_RESULT_MESSAGES,
                        ModelProviderCapability.STRUCTURED_OUTPUT
                },
                new ModelProviderCapability[] {
                        ModelProviderCapability.MCP_INTEROP
                }
        );
    }

    @Test
    public void shouldParseOpenAiTextResponse() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"openai ok\"}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        ModelProviderContract.assertTextChat(provider(), request, "openai ok");

        Assert.assertEquals("/v1/chat/completions", requestPath);
        Assert.assertEquals("Bearer test-key", authorization);
        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertEquals("test-model", body.path("model").asText());
        Assert.assertEquals("hello", body.path("messages").path(0).path("content").asText());
    }

    @Test
    public void shouldAppendChatCompletionsWhenBaseUrlEndsWithV1() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"openai ok\"}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        ModelProviderContract.assertTextChat(providerWithV1BaseUrl(), request, "openai ok");

        Assert.assertEquals("/v1/chat/completions", requestPath);
    }

    @Test
    public void shouldParseOpenAiToolCallResponse() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"tool_calls\":[{\"id\":\"call_abc\",\"function\":{\"name\":\"query_user\",\"arguments\":\"{\\\"userId\\\":\\\"u001\\\"}\"}}]}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("query user");
        ModelProviderContract.assertToolCall(provider(), request, "call_abc", "query_user", "userId", "u001");
    }

    @Test
    public void shouldSendOpenAiToolResultMessages() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"summary ok\"}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("query user");
        ToolCall toolCall = new ToolCall("call_abc", "query_user", argument("userId", "u001"));
        request.setLastToolExecutions(Arrays.asList(new ToolExecutionResult(toolCall, ToolResult.success("user-data"))));
        ModelResponse response = provider().chat(request);

        Assert.assertEquals("summary ok", response.getAnswer());
        JsonNode messages = objectMapper.readTree(requestBody).path("messages");
        Assert.assertEquals("assistant", messages.path(1).path("role").asText());
        Assert.assertEquals("call_abc", messages.path(1).path("tool_calls").path(0).path("id").asText());
        Assert.assertEquals("query_user", messages.path(1).path("tool_calls").path(0).path("function").path("name").asText());
        Assert.assertEquals("tool", messages.path(2).path("role").asText());
        Assert.assertEquals("call_abc", messages.path(2).path("tool_call_id").asText());
        Assert.assertEquals("user-data", messages.path(2).path("content").asText());
    }

    @Test
    public void shouldStreamOpenAiTextResponse() throws Exception {
        startServer("data: {\"choices\":[{\"delta\":{\"content\":\"你\"}}]}\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"好\"}}]}\n"
                + "data: [DONE]\n");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        ModelProviderContract.assertStreamText(provider(), request, "你好");

        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertTrue(body.path("stream").asBoolean());
    }

    @Test
    public void shouldStreamOpenAiToolCallResponse() throws Exception {
        startServer("data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_abc\",\"function\":{\"name\":\"query_user\",\"arguments\":\"{\\\"user\"}}]}}]}\n"
                + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"Id\\\":\\\"u001\\\"}\"}}]}}]}\n"
                + "data: [DONE]\n");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("query user");
        ModelProviderContract.assertStreamToolCall(provider(), request, "call_abc", "query_user", "userId", "u001");

        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertTrue(body.path("stream").asBoolean());
    }

    @Test
    public void shouldIncludeSystemPromptInMessages() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        request.setSystemPrompt("你是一个智能助手");
        provider().chat(request);

        JsonNode messages = objectMapper.readTree(requestBody).path("messages");
        Assert.assertEquals("system", messages.path(0).path("role").asText());
        Assert.assertEquals("你是一个智能助手", messages.path(0).path("content").asText());
        Assert.assertEquals("user", messages.path(1).path("role").asText());
        Assert.assertEquals("hello", messages.path(1).path("content").asText());
    }

    @Test
    public void shouldUseToolChoiceFromRequest() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        request.setToolChoice("none");
        request.getTools().add(dummyTool());
        provider().chat(request);

        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertEquals("none", body.path("tool_choice").asText());
    }

    @Test
    public void shouldDefaultToolChoiceToAuto() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        request.getTools().add(dummyTool());
        provider().chat(request);

        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertEquals("auto", body.path("tool_choice").asText());
    }

    @Test
    public void shouldSendResponseFormatWhenSet() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"{\\\"name\\\":\\\"AgentHub\\\"}\"}}]}");

        java.util.Map<String, Object> schema = new java.util.LinkedHashMap<String, Object>();
        schema.put("type", "object");
        java.util.Map<String, Object> properties = new java.util.LinkedHashMap<String, Object>();
        java.util.Map<String, Object> nameProp = new java.util.LinkedHashMap<String, Object>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schema.put("properties", properties);
        schema.put("required", java.util.Arrays.asList("name"));
        schema.put("additionalProperties", false);

        ResponseFormat responseFormat = ResponseFormat.jsonSchema("project_info", schema);

        ModelRequest request = new ModelRequest();
        request.setUserMessage("describe the project");
        request.setResponseFormat(responseFormat);
        provider().chat(request);

        JsonNode body = objectMapper.readTree(requestBody);
        JsonNode rf = body.path("response_format");
        Assert.assertEquals("json_schema", rf.path("type").asText());
        Assert.assertEquals("project_info", rf.path("json_schema").path("name").asText());
        Assert.assertTrue(rf.path("json_schema").path("strict").asBoolean());
        Assert.assertEquals("object", rf.path("json_schema").path("schema").path("type").asText());
        Assert.assertEquals("name", rf.path("json_schema").path("schema").path("required").path(0).asText());
    }

    @Test
    public void shouldNotSendResponseFormatWhenNull() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        provider().chat(request);

        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertTrue(body.path("response_format").isMissingNode());
    }

    private com.sean.agenthub.agent.core.api.AgentTool dummyTool() {
        return new com.sean.agenthub.agent.core.api.AgentTool() {
            @Override public String name() { return "dummy"; }
            @Override public String description() { return "dummy tool"; }
            @Override public ToolSchema schema() { return new ToolSchema(); }
            @Override public ToolRiskLevel riskLevel() { return ToolRiskLevel.READ; }
            @Override public ToolResult execute(ToolContext ctx) { return ToolResult.success("ok"); }
        };
    }

    @Test
    public void shouldThrowOnNon2xxHttpStatus() throws Exception {
        startServerWithStatus(401, "{\"error\":{\"message\":\"Invalid API key\"}}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        try {
            provider().chat(request);
            Assert.fail("Should throw on 401");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("401"));
            Assert.assertTrue(ex.getMessage().contains("Invalid API key"));
        }
    }

    @Test
    public void shouldThrowOnProviderErrorResponse() throws Exception {
        startServer("{\"error\":{\"message\":\"Rate limit exceeded\",\"type\":\"rate_limit_error\"}}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        try {
            provider().chat(request);
            Assert.fail("Should throw on error response");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("Rate limit exceeded"));
        }
    }

    @Test
    public void shouldReturnEmptyAnswerOnEmptyChoices() throws Exception {
        startServer("{\"choices\":[]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        ModelResponse response = provider().chat(request);

        Assert.assertEquals("", response.getAnswer());
    }

    @Test
    public void shouldRecoverFromMalformedToolCallArguments() throws Exception {
        startServer("{\"choices\":[{\"message\":{\"tool_calls\":[{\"id\":\"call_abc\",\"function\":{\"name\":\"query_user\",\"arguments\":\"not-valid-json\"}}]}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("query user");
        ModelResponse response = provider().chat(request);

        Assert.assertTrue(response.hasToolCalls());
        Assert.assertEquals("query_user", response.getToolCalls().get(0).getName());
        Assert.assertTrue(response.getToolCalls().get(0).getArguments().isEmpty());
    }

    @Test
    public void shouldSkipMalformedStreamLine() throws Exception {
        startServer("data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n"
                + "data: {this is not valid json\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"世界\"}}]}\n"
                + "data: [DONE]\n");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        ModelProviderContract.assertStreamText(provider(), request, "你好世界");
    }

    private OpenAiCompatibleModelProvider provider() {
        HttpModelProviderProperties properties = new HttpModelProviderProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        return new OpenAiCompatibleModelProvider(properties);
    }

    private OpenAiCompatibleModelProvider providerWithV1BaseUrl() {
        HttpModelProviderProperties properties = new HttpModelProviderProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        return new OpenAiCompatibleModelProvider(properties);
    }

    private void startServer(final String responseBody) throws IOException {
        startServerWithStatus(200, responseBody);
    }

    private void startServerWithStatus(final int statusCode, final String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requestPath = exchange.getRequestURI().getPath();
                authorization = exchange.getRequestHeaders().getFirst("Authorization");
                requestBody = readBody(exchange.getRequestBody());
                byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(statusCode, response.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response);
                outputStream.close();
            }
        });
        server.start();
    }

    private String readBody(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder builder = new StringBuilder();
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private Map<String, Object> argument(String key, Object value) {
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put(key, value);
        return arguments;
    }
}
