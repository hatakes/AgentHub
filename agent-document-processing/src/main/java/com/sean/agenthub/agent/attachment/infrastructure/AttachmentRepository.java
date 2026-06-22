package com.sean.agenthub.agent.attachment.infrastructure;

import com.sean.agenthub.agent.attachment.domain.AttachmentRecord;
import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 智能文档处理能力使用的内存附件仓库。
 *
 * @author Sean
 */
@Component
public class AttachmentRepository {

    /** 以 attachmentId 为键的附件记录存储，使用 ConcurrentHashMap 保证线程安全 */
    private final Map<String, AttachmentRecord> records = new ConcurrentHashMap<String, AttachmentRecord>();

    /**
     * 保存附件（纯文本模式）。
     * <p>内部构造 ParsedAttachmentContent 后委托给 {@link #save(String, String, ParsedAttachmentContent)}。</p>
     *
     * @param filename    文件名
     * @param contentType 文件 MIME 类型
     * @param text        解析后的纯文本
     * @return 保存后的附件记录
     */
    public AttachmentRecord save(String filename, String contentType, String text) {
        return save(filename, contentType, new ParsedAttachmentContent(text, "legacy-text"));
    }

    /**
     * 保存附件（结构化解析结果模式）。
     * <p>自动生成 att-{UUID} 格式的附件 ID。</p>
     *
     * @param filename    文件名
     * @param contentType 文件 MIME 类型
     * @param parsed      解析后的附件内容
     * @return 保存后的附件记录
     */
    public AttachmentRecord save(String filename, String contentType, ParsedAttachmentContent parsed) {
        AttachmentRecord record = new AttachmentRecord();
        record.setAttachmentId("att-" + UUID.randomUUID().toString());
        record.setFilename(filename);
        record.setContentType(contentType);
        record.setText(parsed == null || parsed.getText() == null ? "" : parsed.getText());
        record.setParserName(parsed == null ? null : parsed.getParserName());
        record.setMetadata(parsed == null ? null : parsed.getMetadata());
        records.put(record.getAttachmentId(), record);
        return record;
    }

    /**
     * 根据附件 ID 获取附件记录，不存在时抛出异常。
     *
     * @param attachmentId 附件 ID
     * @return 附件记录
     * @throws IllegalArgumentException 附件不存在时抛出
     */
    public AttachmentRecord getRequired(String attachmentId) {
        AttachmentRecord record = records.get(attachmentId);
        if (record == null) {
            throw new IllegalArgumentException("Attachment not found: " + attachmentId);
        }
        return record;
    }

    /**
     * 清空所有附件记录。
     */
    public void clear() {
        records.clear();
    }
}
