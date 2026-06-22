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

    /** 附件 ID */
    private String attachmentId;

    /** 原始文件名 */
    private String filename;

    /** 使用的解析器名称 */
    private String parserName;

    /** 文档标题 */
    private String title;

    /** 文档摘要 */
    private String summary;

    /** 大纲节点列表 */
    private List<String> outline = new ArrayList<String>();

    /** 重点内容列表 */
    private List<String> keyPoints = new ArrayList<String>();

    /** 附加元数据，如文本长度、页数等 */
    private Map<String, Object> metadata = new LinkedHashMap<String, Object>();

    /**
     * 获取附件 ID。
     *
     * @return 附件 ID
     */
    public String getAttachmentId() {
        return attachmentId;
    }

    /**
     * 设置附件 ID。
     *
     * @param attachmentId 附件 ID
     */
    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    /**
     * 获取文件名。
     *
     * @return 文件名
     */
    public String getFilename() {
        return filename;
    }

    /**
     * 设置文件名。
     *
     * @param filename 文件名
     */
    public void setFilename(String filename) {
        this.filename = filename;
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
     * 获取文档标题。
     *
     * @return 文档标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置文档标题。
     *
     * @param title 文档标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取文档摘要。
     *
     * @return 文档摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置文档摘要。
     *
     * @param summary 文档摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取大纲节点列表。
     *
     * @return 大纲节点列表
     */
    public List<String> getOutline() {
        return outline;
    }

    /**
     * 设置大纲节点列表。
     *
     * @param outline 大纲节点列表，null 时使用空列表
     */
    public void setOutline(List<String> outline) {
        this.outline = outline == null ? new ArrayList<String>() : outline;
    }

    /**
     * 获取重点内容列表。
     *
     * @return 重点内容列表
     */
    public List<String> getKeyPoints() {
        return keyPoints;
    }

    /**
     * 设置重点内容列表。
     *
     * @param keyPoints 重点内容列表，null 时使用空列表
     */
    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints == null ? new ArrayList<String>() : keyPoints;
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
