package com.sean.agenthub.agent.attachment.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文档处理能力中的内存附件记录。
 *
 * @author Sean
 */
public class AttachmentRecord {

    /** 附件唯一标识，格式为 att-{UUID} */
    private String attachmentId;

    /** 原始文件名 */
    private String filename;

    /** 文件 MIME 类型 */
    private String contentType;

    /** 解析后的纯文本内容 */
    private String text;

    /** 使用的解析器名称，如 pdfbox、markdown、mimo-image 等 */
    private String parserName;

    /** 解析过程中产生的元数据，如页数、标题、格式等 */
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
     * 获取文件 MIME 类型。
     *
     * @return MIME 类型
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 设置文件 MIME 类型。
     *
     * @param contentType MIME 类型
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取解析后的文本内容。
     *
     * @return 文本内容
     */
    public String getText() {
        return text;
    }

    /**
     * 设置解析后的文本内容。
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
