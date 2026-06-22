package com.sean.agenthub.agent.attachment.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文档处理能力配置项。
 *
 * @author Sean
 */
@ConfigurationProperties(prefix = "attachment")
public class AttachmentAnalysisProperties {

    /** 规则型 mock 模型配置 */
    private MockModel mockModel = new MockModel();

    /** 文件解析相关配置 */
    private Parser parser = new Parser();

    /** 文档大纲提炼相关配置 */
    private Outline outline = new Outline();

    /**
     * 获取 mock 模型配置。
     *
     * @return mock 模型配置
     */
    public MockModel getMockModel() {
        return mockModel;
    }

    /**
     * 设置 mock 模型配置。
     *
     * @param mockModel mock 模型配置，null 时使用默认值
     */
    public void setMockModel(MockModel mockModel) {
        this.mockModel = mockModel == null ? new MockModel() : mockModel;
    }

    /**
     * 获取文件解析配置。
     *
     * @return 文件解析配置
     */
    public Parser getParser() {
        return parser;
    }

    /**
     * 设置文件解析配置。
     *
     * @param parser 文件解析配置，null 时使用默认值
     */
    public void setParser(Parser parser) {
        this.parser = parser == null ? new Parser() : parser;
    }

    /**
     * 获取文档大纲提炼配置。
     *
     * @return 文档大纲提炼配置
     */
    public Outline getOutline() {
        return outline;
    }

    /**
     * 设置文档大纲提炼配置。
     *
     * @param outline 文档大纲提炼配置，null 时使用默认值
     */
    public void setOutline(Outline outline) {
        this.outline = outline == null ? new Outline() : outline;
    }

    /**
     * 规则型 mock 模型配置。
     *
     * @author Sean
     */
    public static class MockModel {

        /** 是否启用 mock 模型 */
        private boolean enabled = true;

        /**
         * 获取是否启用。
         *
         * @return 是否启用
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
    }

    /**
     * 文件解析配置。
     *
     * @author Sean
     */
    public static class Parser {

        /** 图片解析配置 */
        private Image image = new Image();

        /**
         * 获取图片解析配置。
         *
         * @return 图片解析配置
         */
        public Image getImage() {
            return image;
        }

        /**
         * 设置图片解析配置。
         *
         * @param image 图片解析配置，null 时使用默认值
         */
        public void setImage(Image image) {
            this.image = image == null ? new Image() : image;
        }
    }

    /**
     * 图片解析配置。
     *
     * @author Sean
     */
    public static class Image {

        /** 图片解析模式，支持 MOCK 和 MIMO 两种模式 */
        private ImageParserMode mode = ImageParserMode.MOCK;

        /** MiMo 调用配置 */
        private Mimo mimo = new Mimo();

        /**
         * 获取图片解析模式。
         *
         * @return 图片解析模式
         */
        public ImageParserMode getMode() {
            return mode;
        }

        /**
         * 设置图片解析模式。
         *
         * @param mode 图片解析模式
         */
        public void setMode(ImageParserMode mode) {
            this.mode = mode;
        }

        /**
         * 获取 MiMo 调用配置。
         *
         * @return MiMo 调用配置
         */
        public Mimo getMimo() {
            return mimo;
        }

        /**
         * 设置 MiMo 调用配置。
         *
         * @param mimo MiMo 调用配置，null 时使用默认值
         */
        public void setMimo(Mimo mimo) {
            this.mimo = mimo == null ? new Mimo() : mimo;
        }
    }

    /**
     * 文档大纲提炼配置。
     *
     * @author Sean
     */
    public static class Outline {

        /** 大纲提炼模式，支持 LOCAL 和 MIMO 两种模式 */
        private OutlineMode mode = OutlineMode.LOCAL;

        /** MiMo 调用配置 */
        private Mimo mimo = new Mimo();

        /**
         * 获取大纲提炼模式。
         *
         * @return 大纲提炼模式
         */
        public OutlineMode getMode() {
            return mode;
        }

        /**
         * 设置大纲提炼模式。
         *
         * @param mode 大纲提炼模式
         */
        public void setMode(OutlineMode mode) {
            this.mode = mode;
        }

        /**
         * 获取 MiMo 调用配置。
         *
         * @return MiMo 调用配置
         */
        public Mimo getMimo() {
            return mimo;
        }

        /**
         * 设置 MiMo 调用配置。
         *
         * @param mimo MiMo 调用配置，null 时使用默认值
         */
        public void setMimo(Mimo mimo) {
            this.mimo = mimo == null ? new Mimo() : mimo;
        }
    }

    /**
     * MiMo OpenAI-compatible 调用配置。
     *
     * @author Sean
     */
    public static class Mimo {

        /** MiMo API 基础地址 */
        private String baseUrl = "https://api.xiaomimimo.com/v1";

        /** MiMo API 密钥 */
        private String apiKey = "";

        /** 使用的模型名称 */
        private String model;

        /** 连接超时时间，单位毫秒 */
        private int connectTimeoutMs = 10000;

        /** 读取超时时间，单位毫秒 */
        private int readTimeoutMs = 120000;

        /**
         * 获取 API 基础地址。
         *
         * @return API 基础地址
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * 设置 API 基础地址。
         *
         * @param baseUrl API 基础地址
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
         * 获取连接超时时间。
         *
         * @return 连接超时时间，单位毫秒
         */
        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        /**
         * 设置连接超时时间。
         *
         * @param connectTimeoutMs 连接超时时间，单位毫秒
         */
        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        /**
         * 获取读取超时时间。
         *
         * @return 读取超时时间，单位毫秒
         */
        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        /**
         * 设置读取超时时间。
         *
         * @param readTimeoutMs 读取超时时间，单位毫秒
         */
        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    /**
     * 图片解析模式。
     *
     * @author Sean
     */
    public enum ImageParserMode {
        /** 使用 mock 模式解析图片 */
        MOCK,
        /** 使用 MiMo 多模态模型解析图片 */
        MIMO
    }

    /**
     * 文档大纲提炼模式。
     *
     * @author Sean
     */
    public enum OutlineMode {
        /** 使用本地规则提炼大纲 */
        LOCAL,
        /** 使用 MiMo 模型提炼大纲 */
        MIMO
    }
}
