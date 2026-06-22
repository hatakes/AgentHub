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
 * <p>示例实现只抽取出生日期和脱敏证件号，用来演示 Tool 如何把非结构化附件内容转成模型可消费的字段。</p>
 *
 * @author Sean
 */
@Component
public class ExtractDocumentFieldsTool implements AgentTool {

    /** 附件存储仓库 */
    private final AttachmentRepository repository;

    /**
     * 构造器注入依赖。
     *
     * @param repository 附件仓库
     */
    public ExtractDocumentFieldsTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    /**
     * 返回 Tool 名称。
     *
     * @return Tool 名称
     */
    @Override
    public String name() {
        return "extract_document_fields";
    }

    /**
     * 返回 Tool 描述，用于模型理解何时调用。
     *
     * @return Tool 描述
     */
    @Override
    public String description() {
        return "抽取附件结构化字段。仅当用户明确要求分析 attachmentId 对应附件时调用";
    }

    /**
     * 返回 Tool 参数 Schema。
     *
     * @return 包含 attachmentId 参数的 Schema
     */
    @Override
    public ToolSchema schema() {
        return AttachmentToolSupport.attachmentIdSchema();
    }

    /**
     * 返回 Tool 风险等级，此 Tool 为只读操作。
     *
     * @return 风险等级
     */
    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    /**
     * 执行结构化字段抽取。
     * <p>提取证件类型、出生日期和脱敏后的证件号。结构化结果中只保留脱敏后的证件号，
     * 避免后续模型总结或日志输出完整敏感字段。</p>
     *
     * @param context Tool 执行上下文，包含参数
     * @return 包含 documentType、birthDate、idNumberMasked 的结构化结果
     */
    @Override
    public ToolResult execute(ToolContext context) {
        String attachmentId = AttachmentToolSupport.argument(context.getArguments(), "attachmentId");
        AttachmentRecord record = repository.getRequired(attachmentId);
        String documentType = AttachmentToolSupport.classify(record);
        String birthDate = AttachmentToolSupport.extractBirthDate(record);
        // 结构化结果中只保留脱敏后的证件号，避免后续模型总结或日志输出完整敏感字段。
        String idNumberMasked = AttachmentToolSupport.maskIdNumber(record);
        return ToolResult.success("{attachmentId=" + attachmentId
                + ", documentType=" + documentType
                + ", birthDate=" + birthDate
                + ", idNumberMasked=" + idNumberMasked + "}");
    }
}
