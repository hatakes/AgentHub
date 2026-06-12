package com.sean.agenthub.agent.attachment.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文档处理能力配置项。
 *
 * @author Sean
 */
@ConfigurationProperties(prefix = "attachment")
public class AttachmentAnalysisProperties {
    private MockModel mockModel = new MockModel();
    private Parser parser = new Parser();
    private Outline outline = new Outline();

    public MockModel getMockModel() {
        return mockModel;
    }

    public void setMockModel(MockModel mockModel) {
        this.mockModel = mockModel == null ? new MockModel() : mockModel;
    }

    public Parser getParser() {
        return parser;
    }

    public void setParser(Parser parser) {
        this.parser = parser == null ? new Parser() : parser;
    }

    public Outline getOutline() {
        return outline;
    }

    public void setOutline(Outline outline) {
        this.outline = outline == null ? new Outline() : outline;
    }

    /**
     * 规则型 mock 模型配置。
     *
     * @author Sean
     */
    public static class MockModel {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

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
        private Image image = new Image();

        public Image getImage() {
            return image;
        }

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
        private ImageParserMode mode = ImageParserMode.MOCK;
        private Mimo mimo = new Mimo();

        public ImageParserMode getMode() {
            return mode;
        }

        public void setMode(ImageParserMode mode) {
            this.mode = mode;
        }

        public Mimo getMimo() {
            return mimo;
        }

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
        private OutlineMode mode = OutlineMode.LOCAL;
        private Mimo mimo = new Mimo();

        public OutlineMode getMode() {
            return mode;
        }

        public void setMode(OutlineMode mode) {
            this.mode = mode;
        }

        public Mimo getMimo() {
            return mimo;
        }

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
        private String baseUrl = "https://api.xiaomimimo.com/v1";
        private String apiKey = "";
        private String model;
        private int connectTimeoutMs = 10000;
        private int readTimeoutMs = 120000;

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
     * 图片解析模式。
     *
     * @author Sean
     */
    public enum ImageParserMode {
        MOCK,
        MIMO
    }

    /**
     * 文档大纲提炼模式。
     *
     * @author Sean
     */
    public enum OutlineMode {
        LOCAL,
        MIMO
    }
}
