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
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " must not be empty");
        }
    }

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

    private URL toUrl(String value) throws IOException {
        try {
            return new URI(value).toURL();
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid MiMo endpoint: " + value, ex);
        }
    }
}
