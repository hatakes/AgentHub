package com.sean.agenthub.agent.attachment.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档大纲和重点提炼结果。
 *
 * @author Sean
 */
public class DocumentOutlineResult {
    private String attachmentId;
    private String filename;
    private String parserName;
    private String title;
    private String summary;
    private List<String> outline = new ArrayList<String>();
    private List<String> keyPoints = new ArrayList<String>();
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

    public String getParserName() {
        return parserName;
    }

    public void setParserName(String parserName) {
        this.parserName = parserName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getOutline() {
        return outline;
    }

    public void setOutline(List<String> outline) {
        this.outline = outline == null ? new ArrayList<String>() : outline;
    }

    public List<String> getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints == null ? new ArrayList<String>() : keyPoints;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<String, Object>() : metadata;
    }
}
