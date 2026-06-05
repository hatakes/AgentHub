package com.sean.agenthub.agent.provider.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.core.model.ToolExecutionResult;
import com.sean.agenthub.agent.core.tool.ToolResult;
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
 * Anthropic 兼容协议适配器测试。
 *
 * @author Sean
 */
public class AnthropicCompatibleModelProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String requestPath;
    private String apiKey;
    private String anthropicVersion;
    private String requestBody;

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void shouldDeclareAnthropicCapabilities() throws Exception {
        startServer("{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}");

        AnthropicCompatibleModelProvider provider = provider();

        ModelProviderContract.assertCapabilities(
                provider,
                new ModelProviderCapability[] {
                        ModelProviderCapability.TEXT_CHAT,
                        ModelProviderCapability.TEXT_STREAM,
                        ModelProviderCapability.STREAM_TOOL_CALL,
                        ModelProviderCapability.TOOL_CALL,
                        ModelProviderCapability.MULTI_TOOL_CALL,
                        ModelProviderCapability.TOOL_RESULT_MESSAGES
                },
                new ModelProviderCapability[] {
                        ModelProviderCapability.STRUCTURED_OUTPUT,
                        ModelProviderCapability.MCP_INTEROP
                }
        );
    }

    @Test
    public void shouldParseAnthropicTextResponse() throws Exception {
        startServer("{\"content\":[{\"type\":\"text\",\"text\":\"anthropic ok\"}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        ModelProviderContract.assertTextChat(provider(), request, "anthropic ok");

        Assert.assertEquals("/v1/messages", requestPath);
        Assert.assertEquals("test-key", apiKey);
        Assert.assertEquals("2023-06-01", anthropicVersion);
        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertEquals("test-model", body.path("model").asText());
        Assert.assertEquals("hello", body.path("messages").path(0).path("content").asText());
    }

    @Test
    public void shouldParseAnthropicToolUseResponse() throws Exception {
        startServer("{\"content\":[{\"type\":\"tool_use\",\"id\":\"toolu_abc\",\"name\":\"query_user\",\"input\":{\"userId\":\"u001\"}}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("query user");
        ModelProviderContract.assertToolCall(provider(), request, "toolu_abc", "query_user", "userId", "u001");
    }

    @Test
    public void shouldSendAnthropicToolResultMessages() throws Exception {
        startServer("{\"content\":[{\"type\":\"text\",\"text\":\"summary ok\"}]}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("query user");
        ToolCall toolCall = new ToolCall("toolu_abc", "query_user", argument("userId", "u001"));
        request.setLastToolExecutions(Arrays.asList(new ToolExecutionResult(toolCall, ToolResult.success("user-data"))));
        ModelResponse response = provider().chat(request);

        Assert.assertEquals("summary ok", response.getAnswer());
        JsonNode messages = objectMapper.readTree(requestBody).path("messages");
        Assert.assertEquals("assistant", messages.path(1).path("role").asText());
        Assert.assertEquals("tool_use", messages.path(1).path("content").path(0).path("type").asText());
        Assert.assertEquals("toolu_abc", messages.path(1).path("content").path(0).path("id").asText());
        Assert.assertEquals("user", messages.path(2).path("role").asText());
        Assert.assertEquals("tool_result", messages.path(2).path("content").path(0).path("type").asText());
        Assert.assertEquals("toolu_abc", messages.path(2).path("content").path(0).path("tool_use_id").asText());
        Assert.assertEquals("user-data", messages.path(2).path("content").path(0).path("content").asText());
    }

    @Test
    public void shouldStreamAnthropicTextResponse() throws Exception {
        startServer("data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"你\"}}\n"
                + "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"好\"}}\n"
                + "data: [DONE]\n");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        ModelProviderContract.assertStreamText(provider(), request, "你好");

        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertTrue(body.path("stream").asBoolean());
    }

    @Test
    public void shouldStreamAnthropicToolUseResponse() throws Exception {
        startServer("data: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"tool_use\",\"id\":\"toolu_abc\",\"name\":\"query_user\",\"input\":{}}}\n"
                + "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"user\"}}\n"
                + "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"Id\\\":\\\"u001\\\"}\"}}\n"
                + "data: [DONE]\n");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("query user");
        ModelProviderContract.assertStreamToolCall(provider(), request, "toolu_abc", "query_user", "userId", "u001");

        JsonNode body = objectMapper.readTree(requestBody);
        Assert.assertTrue(body.path("stream").asBoolean());
    }

    @Test
    public void shouldThrowOnNon2xxHttpStatus() throws Exception {
        startServerWithStatus(500, "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"Internal server error\"}}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        try {
            provider().chat(request);
            Assert.fail("Should throw on 500");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("500"));
        }
    }

    @Test
    public void shouldThrowOnProviderErrorResponse() throws Exception {
        startServer("{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"Invalid API key\"}}");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        try {
            provider().chat(request);
            Assert.fail("Should throw on error response");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("Invalid API key"));
        }
    }

    @Test
    public void shouldSkipMalformedStreamLine() throws Exception {
        startServer("data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"你好\"}}\n"
                + "data: {this is not valid json\n"
                + "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"世界\"}}\n"
                + "data: [DONE]\n");

        ModelRequest request = new ModelRequest();
        request.setUserMessage("hello");
        ModelProviderContract.assertStreamText(provider(), request, "你好世界");
    }

    private AnthropicCompatibleModelProvider provider() {
        HttpModelProviderProperties properties = new HttpModelProviderProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        return new AnthropicCompatibleModelProvider(properties);
    }

    private void startServer(final String responseBody) throws IOException {
        startServerWithStatus(200, responseBody);
    }

    private void startServerWithStatus(final int statusCode, final String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requestPath = exchange.getRequestURI().getPath();
                apiKey = exchange.getRequestHeaders().getFirst("x-api-key");
                anthropicVersion = exchange.getRequestHeaders().getFirst("anthropic-version");
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
