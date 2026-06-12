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
 * 识别附件类型的只读 Tool。
 *
 * @author Sean
 */
@Component
public class ClassifyDocumentTool implements AgentTool {
    private final AttachmentRepository repository;

    public ClassifyDocumentTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "classify_document";
    }

    @Override
    public String description() {
        return "识别附件类型。仅当用户明确要求分析 attachmentId 对应附件时调用";
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
        return ToolResult.success("{attachmentId=" + attachmentId
                + ", documentType=" + AttachmentToolSupport.classify(record) + "}");
    }
}
