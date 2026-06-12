package com.sean.agenthub.agent.attachment.tool;

import com.sean.agenthub.agent.attachment.domain.AttachmentRecord;
import com.sean.agenthub.agent.attachment.infrastructure.AttachmentRepository;
import com.sean.agenthub.agent.attachment.support.AttachmentToolSupport;
import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import org.springframework.stereotype.Component;

/**
 * 解析附件基础内容的只读 Tool。
 *
 * @author Sean
 */
@Component
public class ParseAttachmentTool implements AgentTool {
    private final AttachmentRepository repository;

    public ParseAttachmentTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "parse_attachment";
    }

    @Override
    public String description() {
        return "解析附件文本和元数据。仅当用户明确要求分析 attachmentId 对应附件时调用";
    }

    @Override
    public ToolSchema schema() {
        return AttachmentToolSupport.attachmentIdSchema();
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        String attachmentId = AttachmentToolSupport.argument(context.getArguments(), "attachmentId");
        AttachmentRecord record = repository.getRequired(attachmentId);
        String text = AttachmentToolSupport.redactSensitiveText(record.getText());
        int previewLength = Math.min(text.length(), 80);
        return ToolResult.success("{attachmentId=" + attachmentId
                + ", filename=" + record.getFilename()
                + ", contentType=" + record.getContentType()
                + ", textPreview=" + text.substring(0, previewLength) + "}");
    }
}
