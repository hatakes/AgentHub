package com.sean.agenthub.agent.attachment.infrastructure;

import com.sean.agenthub.agent.attachment.domain.AttachmentRecord;
import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 智能附件分析样板使用的内存附件仓库。
 *
 * @author Sean
 */
@Component
public class AttachmentRepository {
    private final Map<String, AttachmentRecord> records = new ConcurrentHashMap<String, AttachmentRecord>();

    public AttachmentRecord save(String filename, String contentType, String text) {
        return save(filename, contentType, new ParsedAttachmentContent(text, "legacy-text"));
    }

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

    public AttachmentRecord getRequired(String attachmentId) {
        AttachmentRecord record = records.get(attachmentId);
        if (record == null) {
            throw new IllegalArgumentException("Attachment not found: " + attachmentId);
        }
        return record;
    }

    public void clear() {
        records.clear();
    }
}
