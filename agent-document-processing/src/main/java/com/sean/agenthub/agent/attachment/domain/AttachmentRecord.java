package com.sean.agenthub.agent.attachment.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文档处理能力中的内存附件记录。
 *
 * @author Sean
 */
public class AttachmentRecord {
    private String attachmentId;
    private String filename;
    private String contentType;
    private String text;
    private String parserName;
    private Map<String, Object> metadata = new LinkedHashMap<String, Object>();

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
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
