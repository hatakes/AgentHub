package com.sean.agenthub.agent.attachment.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 附件解析后的内容。
 *
 * @author Sean
 */
public class ParsedAttachmentContent {

    /** 解析后的纯文本内容 */
    private String text;

    /** 使用的解析器名称 */
    private String parserName;

    /** 解析过程中产生的元数据 */
    private Map<String, Object> metadata = new LinkedHashMap<String, Object>();

    /**
     * 默认构造器。
     */
    public ParsedAttachmentContent() {
    }

    /**
     * 构造解析结果，使用默认空元数据。
     *
     * @param text       解析后的文本
     * @param parserName 解析器名称
     */
    public ParsedAttachmentContent(String text, String parserName) {
        this.text = text;
        this.parserName = parserName;
    }

    /**
     * 构造解析结果，包含自定义元数据。
     *
     * @param text       解析后的文本
     * @param parserName 解析器名称
     * @param metadata   元数据，null 时使用空 Map
     */
    public ParsedAttachmentContent(String text, String parserName, Map<String, Object> metadata) {
        this.text = text;
        this.parserName = parserName;
        this.metadata = metadata == null ? new LinkedHashMap<String, Object>() : metadata;
    }

    /**
     * 获取解析后的文本。
     *
     * @return 文本内容
     */
    public String getText() {
        return text;
    }

    /**
     * 设置解析后的文本。
     *
     * @param text 文本内容
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * 获取解析器名称。
     *
     * @return 解析器名称
     */
    public String getParserName() {
        return parserName;
    }

    /**
     * 设置解析器名称。
     *
     * @param parserName 解析器名称
     */
    public void setParserName(String parserName) {
        this.parserName = parserName;
    }

    /**
     * 获取元数据。
     *
     * @return 元数据 Map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 设置元数据。
     *
     * @param metadata 元数据 Map，null 时使用空 Map
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<String, Object>() : metadata;
    }
}
