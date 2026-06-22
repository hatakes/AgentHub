package com.sean.agenthub.agent.starter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AgentHub Starter 配置项。
 *
 * <p>对应 application.yml 中的 {@code agent.*} 前缀配置。</p>
 *
 * @author Sean
 */
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    /** 是否启用 AgentHub，默认 true。 */
    private boolean enabled = true;
    /** 模型协议配置。 */
    private Model model = new Model();
    /** Tool 注册配置。 */
    private Tools tools = new Tools();

    /**
     * 判断是否启用。
     *
     * @return 启用返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取模型配置。
     *
     * @return 模型配置
     */
    public Model getModel() {
        return model;
    }

    /**
     * 设置模型配置，null 会被替换为默认值。
     *
     * @param model 模型配置
     */
    public void setModel(Model model) {
        this.model = model == null ? new Model() : model;
    }

    /**
     * 获取 Tool 配置。
     *
     * @return Tool 配置
     */
    public Tools getTools() {
        return tools;
    }

    /**
     * 设置 Tool 配置，null 会被替换为默认值。
     *
     * @param tools Tool 配置
     */
    public void setTools(Tools tools) {
        this.tools = tools == null ? new Tools() : tools;
    }

    /**
     * 模型协议配置。
     *
     * @author Sean
     */
    public static class Model {
        /** 协议类型：echo、openai、anthropic。默认 echo。 */
        private String protocol = "echo";
        /** 模型服务基地址。 */
        private String baseUrl;
        /** API 密钥。 */
        private String apiKey;
        /** 模型名称。 */
        private String model;
        /** 是否要求 API 密钥，默认 true。本地无鉴权网关可设为 false。 */
        private boolean apiKeyRequired = true;
        /** HTTP 连接超时（毫秒），默认 10 秒。 */
        private int connectTimeoutMs = 10000;
        /** HTTP 读取超时（毫秒），默认 60 秒。 */
        private int readTimeoutMs = 60000;

        /**
         * 获取协议类型。
         *
         * @return 协议类型
         */
        public String getProtocol() {
            return protocol;
        }

        /**
         * 设置协议类型。
         *
         * @param protocol 协议类型
         */
        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

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
         * 判断是否要求 API 密钥。
         *
         * @return 要求返回 true
         */
        public boolean isApiKeyRequired() {
            return apiKeyRequired;
        }

        /**
         * 设置是否要求 API 密钥。
         *
         * @param apiKeyRequired 是否要求
         */
        public void setApiKeyRequired(boolean apiKeyRequired) {
            this.apiKeyRequired = apiKeyRequired;
        }

        /**
         * 获取连接超时。
         *
         * @return 连接超时（毫秒）
         */
        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        /**
         * 设置连接超时。
         *
         * @param connectTimeoutMs 连接超时（毫秒）
         */
        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        /**
         * 获取读取超时。
         *
         * @return 读取超时（毫秒）
         */
        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        /**
         * 设置读取超时。
         *
         * @param readTimeoutMs 读取超时（毫秒）
         */
        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    /**
     * Tool 注册配置。
     *
     * @author Sean
     */
    public static class Tools {
        /** 允许暴露给模型的 Tool 名称白名单，空列表表示不限制。 */
        private List<String> allowedNames = new ArrayList<String>();

        /**
         * 获取 Tool 白名单。
         *
         * @return 白名单列表
         */
        public List<String> getAllowedNames() {
            return allowedNames;
        }

        /**
         * 设置 Tool 白名单，null 会被替换为空列表。
         *
         * @param allowedNames 白名单列表
         */
        public void setAllowedNames(List<String> allowedNames) {
            this.allowedNames = allowedNames == null ? new ArrayList<String>() : allowedNames;
        }
    }
}
