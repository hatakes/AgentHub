package com.sean.agenthub.agent.provider.http.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;

/**
 * 基于 JDK HttpURLConnection 的轻量 JSON HTTP 客户端。
 *
 * <p>包内实现细节，仅供 HTTP ModelProvider 适配器复用；不作为模块公开 API 暴露。</p>
 *
 * @author Sean
 */
public class HttpJsonClient {
    /** Jackson ObjectMapper，用于序列化请求体和解析响应 JSON。 */
    private final ObjectMapper objectMapper;
    /** HTTP 连接超时（毫秒）。 */
    private final int connectTimeoutMs;
    /** HTTP 读取超时（毫秒）。 */
    private final int readTimeoutMs;

    /**
     * 创建 HTTP JSON 客户端。
     *
     * @param objectMapper    JSON 序列化器
     * @param connectTimeoutMs 连接超时
     * @param readTimeoutMs    读取超时
     */
    public HttpJsonClient(ObjectMapper objectMapper, int connectTimeoutMs, int readTimeoutMs) {
        this.objectMapper = objectMapper;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * 发送 POST JSON 请求并返回解析后的响应。
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @param body    请求体
     * @return 解析后的 JSON 响应
     * @throws IllegalStateException 如果请求失败或返回非 2xx 状态
     */
    public JsonNode postJson(String url, Map<String, String> headers, Map<String, Object> body) {
        try {
            HttpURLConnection connection = HttpRequestSupport.openPostJsonConnection(
                    url, headers, connectTimeoutMs, readTimeoutMs);
            HttpRequestSupport.writeJsonBody(connection, objectMapper, body);

            int status = connection.getResponseCode();
            InputStream inputStream = HttpRequestSupport.responseStream(connection, status);
            String responseBody = HttpRequestSupport.readBody(inputStream);
            if (!HttpRequestSupport.isSuccessStatus(status)) {
                throw new IllegalStateException("Model provider HTTP error " + status + ": " + responseBody);
            }
            return objectMapper.readTree(responseBody);
        } catch (IOException ex) {
            throw new IllegalStateException("Model provider HTTP request failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 发送 POST JSON 流式请求，逐行回调处理。
     *
     * @param url         请求 URL
     * @param headers     请求头
     * @param body        请求体
     * @param lineHandler 行回调处理器
     * @throws IllegalStateException 如果请求失败或返回非 2xx 状态
     */
    public void postJsonStream(String url,
                               Map<String, String> headers,
                               Map<String, Object> body,
                               LineHandler lineHandler) {
        try {
            HttpURLConnection connection = HttpRequestSupport.openPostJsonConnection(
                    url, headers, connectTimeoutMs, readTimeoutMs);
            HttpRequestSupport.writeJsonBody(connection, objectMapper, body);

            int status = connection.getResponseCode();
            InputStream inputStream = HttpRequestSupport.responseStream(connection, status);
            if (!HttpRequestSupport.isSuccessStatus(status)) {
                throw new IllegalStateException("Model provider HTTP error " + status + ": "
                        + HttpRequestSupport.readBody(inputStream));
            }
            HttpRequestSupport.readLines(inputStream, new HttpRequestSupport.LineConsumer() {
                @Override
                public void onLine(String line) {
                    lineHandler.onLine(line);
                }
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Model provider HTTP stream request failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 流式响应行处理器。
     */
    public interface LineHandler {
        /**
         * 处理一行流式数据。
         *
         * @param line 原始行内容
         */
        void onLine(String line);
    }
}
