package com.sean.agenthub.agent.provider.http;

import com.fasterxml.jackson.databind.JsonNode;
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
 * 基于 JDK HttpURLConnection 的轻量 JSON HTTP 客户端。
 *
 * <p>包内实现细节，仅供 HTTP ModelProvider 适配器复用；不作为模块公开 API 暴露。</p>
 *
 * @author Sean
 */
class HttpJsonClient {
    private final ObjectMapper objectMapper;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    HttpJsonClient(ObjectMapper objectMapper, int connectTimeoutMs, int readTimeoutMs) {
        this.objectMapper = objectMapper;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    JsonNode postJson(String url, Map<String, String> headers, Map<String, Object> body) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            byte[] payload = objectMapper.writeValueAsBytes(body);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(payload);
            outputStream.close();

            int status = connection.getResponseCode();
            InputStream inputStream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = readBody(inputStream);
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Model provider HTTP error " + status + ": " + responseBody);
            }
            return objectMapper.readTree(responseBody);
        } catch (IOException ex) {
            throw new IllegalStateException("Model provider HTTP request failed: " + ex.getMessage(), ex);
        }
    }

    void postJsonStream(String url, Map<String, String> headers, Map<String, Object> body, LineHandler lineHandler) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            byte[] payload = objectMapper.writeValueAsBytes(body);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(payload);
            outputStream.close();

            int status = connection.getResponseCode();
            InputStream inputStream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Model provider HTTP error " + status + ": " + readBody(inputStream));
            }
            readLines(inputStream, lineHandler);
        } catch (IOException ex) {
            throw new IllegalStateException("Model provider HTTP stream request failed: " + ex.getMessage(), ex);
        }
    }

    private String readBody(InputStream inputStream) throws IOException {
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

    private void readLines(InputStream inputStream, LineHandler lineHandler) throws IOException {
        if (inputStream == null) {
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            lineHandler.onLine(line);
        }
    }

    interface LineHandler {
        void onLine(String line);
    }
}
