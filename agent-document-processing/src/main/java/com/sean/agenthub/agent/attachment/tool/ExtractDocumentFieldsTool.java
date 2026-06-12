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
 * 抽取附件结构化字段的只读 Tool。
 *
 * @author Sean
 */
@Component
public class ExtractDocumentFieldsTool implements AgentTool {
    private final AttachmentRepository repository;

    public ExtractDocumentFieldsTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "extract_document_fields";
    }

    @Override
    public String description() {
        return "抽取附件结构化字段。仅当用户明确要求分析 attachmentId 对应附件时调用";
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
        String documentType = AttachmentToolSupport.classify(record);
        String birthDate = AttachmentToolSupport.extractBirthDate(record);
        String idNumberMasked = AttachmentToolSupport.maskIdNumber(record);
        return ToolResult.success("{attachmentId=" + attachmentId
                + ", documentType=" + documentType
                + ", birthDate=" + birthDate
                + ", idNumberMasked=" + idNumberMasked + "}");
    }
}
