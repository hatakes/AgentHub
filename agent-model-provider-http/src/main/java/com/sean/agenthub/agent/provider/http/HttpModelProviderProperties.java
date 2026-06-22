package com.sean.agenthub.agent.provider.http;

/**
 * HTTP 模型供应商公共配置。
 *
 * <p>OpenAI 和 Anthropic 兼容 provider 共用的连接和鉴权参数。
 * 由 Starter 从 application.yml 中读取后构建。</p>
 *
 * @author Sean
 */
public class HttpModelProviderProperties {
    /** 模型服务基地址（如 https://api.deepseek.com）。 */
    private String baseUrl;
    /** API 密钥，用于请求鉴权。 */
    private String apiKey;
    /** 模型名称（如 deepseek-v4-flash）。 */
    private String model;
    /** HTTP 连接超时（毫秒），默认 10 秒。 */
    private int connectTimeoutMs = 10000;
    /** HTTP 读取超时（毫秒），默认 60 秒。 */
    private int readTimeoutMs = 60000;

    /**
     * 获取基地址。
     *
     * @return 基地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置基地址。
     *
     * @param baseUrl 基地址
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 获取 API 密钥。
     *
     * @return API 密钥
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置 API 密钥。
     *
     * @param apiKey API 密钥
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 获取模型名称。
     *
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置模型名称。
     *
     * @param model 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取连接超时（毫秒）。
     *
     * @return 连接超时
     */
    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    /**
     * 设置连接超时（毫秒）。
     *
     * @param connectTimeoutMs 连接超时
     */
    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /**
     * 获取读取超时（毫秒）。
     *
     * @return 读取超时
     */
    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    /**
     * 设置读取超时（毫秒）。
     *
     * @param readTimeoutMs 读取超时
     */
    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
