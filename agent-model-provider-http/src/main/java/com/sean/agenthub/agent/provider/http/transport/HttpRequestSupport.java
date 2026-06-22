package com.sean.agenthub.agent.provider.http.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP 模型适配器使用的请求和响应辅助方法。
 *
 * <p>这个类只放 HTTP 传输层的通用逻辑：创建 POST JSON 连接、写请求体、读取响应体、
 * 逐行消费流式响应，以及提取 SSE data 行。模型协议字段仍由具体 provider 解析。</p>
 *
 * @author Sean
 */
public final class HttpRequestSupport {
    /** HTTP GET 方法名。 */
    private static final String METHOD_GET = "GET";
    /** HTTP POST 方法名。 */
    private static final String METHOD_POST = "POST";
    /** Content-Type 请求头名称。 */
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    /** JSON 内容类型值。 */
    private static final String CONTENT_TYPE_JSON = "application/json";
    /** SSE data 行前缀。 */
    private static final String SSE_DATA_PREFIX = "data:";
    /** SSE 流结束标记。 */
    private static final String SSE_DONE = "[DONE]";
    /** HTTP 成功状态码下限（含）。 */
    private static final int HTTP_SUCCESS_MIN = 200;
    /** HTTP 成功状态码上限（不含）。 */
    private static final int HTTP_SUCCESS_MAX_EXCLUSIVE = 300;

    private HttpRequestSupport() {
    }

    /**
     * 创建 POST JSON 连接。
     *
     * @param url             请求 URL
     * @param headers         额外请求头
     * @param connectTimeoutMs 连接超时
     * @param readTimeoutMs    读取超时
     * @return 已配置的 HTTP 连接
     * @throws IOException 如果连接创建失败
     */
    static HttpURLConnection openPostJsonConnection(String url,
                                                    Map<String, String> headers,
                                                    int connectTimeoutMs,
                                                    int readTimeoutMs) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(METHOD_POST);
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setDoOutput(true);
        connection.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        return connection;
    }

    /**
     * 将请求体序列化为 JSON 并写入连接输出流。
     *
     * @param connection   HTTP 连接
     * @param objectMapper JSON 序列化器
     * @param body         请求体
     * @throws IOException 如果写入失败
     */
    static void writeJsonBody(HttpURLConnection connection,
                              ObjectMapper objectMapper,
                              Map<String, Object> body) throws IOException {
        byte[] payload = objectMapper.writeValueAsBytes(body);
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(payload);
        } finally {
            outputStream.close();
        }
    }

    /**
     * 根据状态码返回输入流（成功用 getInputStream，失败用 getErrorStream）。
     *
     * @param connection HTTP 连接
     * @param status     HTTP 状态码
     * @return 输入流
     * @throws IOException 如果获取流失败
     */
    static InputStream responseStream(HttpURLConnection connection, int status) throws IOException {
        return isSuccessStatus(status) ? connection.getInputStream() : connection.getErrorStream();
    }

    /**
     * 从输入流中读取全部内容为字符串。
     *
     * @param inputStream 输入流
     * @return 响应体文本
     * @throws IOException 如果读取失败
     */
    static String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    /**
     * 逐行读取输入流并回调。
     *
     * @param inputStream  输入流
     * @param lineConsumer 行消费者
     * @throws IOException 如果读取失败
     */
    static void readLines(InputStream inputStream, LineConsumer lineConsumer) throws IOException {
        if (inputStream == null) {
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            lineConsumer.onLine(line);
        }
    }

    /**
     * 从 SSE 行中提取 data 字段内容。
     *
     * <p>跳过非 data 行、空 data 和 [DONE] 标记。</p>
     *
     * @param line SSE 原始行
     * @return data 内容，非 data 行或结束标记返回 null
     */
    public static String sseData(String line) {
        if (line == null || !line.startsWith(SSE_DATA_PREFIX)) {
            return null;
        }
        String data = line.substring(SSE_DATA_PREFIX.length()).trim();
        if (data.isEmpty() || SSE_DONE.equals(data)) {
            return null;
        }
        return data;
    }

    /**
     * 判断 HTTP 状态码是否为成功（2xx）。
     *
     * @param status 状态码
     * @return 2xx 返回 true
     */
    static boolean isSuccessStatus(int status) {
        return status >= HTTP_SUCCESS_MIN && status < HTTP_SUCCESS_MAX_EXCLUSIVE;
    }

    /**
     * 流式响应行消费者接口。
     */
    interface LineConsumer {
        /**
         * 消费一行数据。
         *
         * @param line 原始行内容
         */
        void onLine(String line);
    }
}
