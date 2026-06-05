package com.sean.agenthub.agent.starter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AgentHub Starter 配置项。
 *
 * @author Sean
 */
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    private boolean enabled = true;
    private Model model = new Model();
    private Tools tools = new Tools();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model == null ? new Model() : model;
    }

    public Tools getTools() {
        return tools;
    }

    public void setTools(Tools tools) {
        this.tools = tools == null ? new Tools() : tools;
    }

    /**
     * 模型协议配置。
     *
     * @author Sean
     */
    public static class Model {
        private String protocol = "echo";
        private String baseUrl;
        private String apiKey;
        private String model;
        private boolean apiKeyRequired = true;
        private int connectTimeoutMs = 10000;
        private int readTimeoutMs = 60000;

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean isApiKeyRequired() {
            return apiKeyRequired;
        }

        public void setApiKeyRequired(boolean apiKeyRequired) {
            this.apiKeyRequired = apiKeyRequired;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

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
        private List<String> allowedNames = new ArrayList<String>();

        public List<String> getAllowedNames() {
            return allowedNames;
        }

        public void setAllowedNames(List<String> allowedNames) {
            this.allowedNames = allowedNames == null ? new ArrayList<String>() : allowedNames;
        }
    }
}
