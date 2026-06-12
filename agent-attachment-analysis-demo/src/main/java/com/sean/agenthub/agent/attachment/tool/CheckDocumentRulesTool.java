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
 * 执行附件确定性业务规则的只读 Tool。
 *
 * @author Sean
 */
@Component
public class CheckDocumentRulesTool implements AgentTool {
    private final AttachmentRepository repository;

    public CheckDocumentRulesTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "check_document_rules";
    }

    @Override
    public String description() {
        return "执行附件业务规则校验。仅当用户明确要求分析 attachmentId 对应附件时调用";
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
        String birthDate = AttachmentToolSupport.extractBirthDate(record);
        int age = AttachmentToolSupport.ageOn2026(birthDate);
        boolean adult = age >= 18;
        String ruleCode = adult ? "AGE_ADULT" : "AGE_UNDER_18";
        boolean passed = adult || age < 0;
        return ToolResult.success("{attachmentId=" + attachmentId
                + ", ruleCode=" + ruleCode
                + ", age=" + age
                + ", adult=" + adult
                + ", passed=" + passed + "}");
    }
}
