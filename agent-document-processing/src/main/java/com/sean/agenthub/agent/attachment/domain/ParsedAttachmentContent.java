package com.sean.agenthub.agent.attachment.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 附件解析后的内容。
 *
 * @author Sean
 */
public class ParsedAttachmentContent {
    private String text;
    private String parserName;
    private Map<String, Object> metadata = new LinkedHashMap<String, Object>();

    public ParsedAttachmentContent() {
    }

    public ParsedAttachmentContent(String text, String parserName) {
        this.text = text;
        this.parserName = parserName;
    }

    public ParsedAttachmentContent(String text, String parserName, Map<String, Object> metadata) {
        this.text = text;
        this.parserName = parserName;
        this.metadata = metadata == null ? new LinkedHashMap<String, Object>() : metadata;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getParserName() {
        return parserName;
    }

    public void setParserName(String parserName) {
        this.parserName = parserName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<String, Object>() : metadata;
    }
}
