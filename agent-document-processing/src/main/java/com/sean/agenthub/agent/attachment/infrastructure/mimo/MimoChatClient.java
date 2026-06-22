package com.sean.agenthub.agent.attachment.infrastructure.mimo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * MiMo OpenAI-compatible chat completions client.
 *
 * @author Sean
 */
@Component
public class MimoChatClient {

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 MiMo chat completions 接口。
     *
     * @param baseUrl         API 基础地址
     * @param apiKey          API 密钥
     * @param body            请求体，包含 model、messages 等字段
     * @param connectTimeoutMs 连接超时时间，单位毫秒
     * @param readTimeoutMs    读取超时时间，单位毫秒
     * @return API 返回的 JSON 响应
     * @throws IOException 网络或序列化异常
     */
    public JsonNode chatCompletions(String baseUrl,
                                    String apiKey,
                                    Map<String, Object> body,
                                    int connectTimeoutMs,
                                    int readTimeoutMs) throws IOException {
        requireText(baseUrl, "mimo.base-url");
        requireText(apiKey, "mimo.api-key");

        HttpURLConnection connection = (HttpURLConnection) toUrl(endpoint(baseUrl)).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        byte[] bytes = objectMapper.writeValueAsBytes(body);
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(bytes);
        } finally {
            outputStream.close();
        }

        int status = connection.getResponseCode();
        InputStream inputStream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String responseBody = readAll(inputStream, StandardCharsets.UTF_8);
        return objectMapper.readTree(responseBody);
    }

    /**
     * 根据基础地址构建完整的 chat completions 端点 URL。
     * <p>自动处理末尾斜杠、是否已包含 /v1 或 /chat/completions 路径的情况。</p>
     *
     * @param baseUrl API 基础地址
     * @return 完整的 chat completions 端点 URL
     */
    public String endpoint(String baseUrl) {
        requireText(baseUrl, "mimo.base-url");
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        return normalized + "/v1/chat/completions";
    }

    /**
     * 校验字符串不为空，为空时抛出异常。
     *
     * @param value 待校验的字符串
     * @param name  参数名称，用于错误信息
     * @throws IllegalStateException 字符串为空时抛出
     */
    public void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " must not be empty");
        }
    }

    /**
     * 读取输入流的全部内容。
     *
     * @param inputStream 输入流
     * @param charset     字符编码
     * @return 流内容字符串
     * @throws IOException 读取异常
     */
    private String readAll(InputStream inputStream, Charset charset) throws IOException {
        if (inputStream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    /**
     * 将字符串转换为 URL 对象。
     *
     * @param value URL 字符串
     * @return URL 对象
     * @throws IOException URL 格式无效时抛出
     */
    private URL toUrl(String value) throws IOException {
        try {
            return new URI(value).toURL();
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid MiMo endpoint: " + value, ex);
        }
    }
}
