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
 * <p>该 Tool 只返回文件元数据和脱敏后的文本预览，避免把完整附件内容直接塞进模型上下文和审计日志。</p>
 *
 * @author Sean
 */
@Component
public class ParseAttachmentTool implements AgentTool {

    /** 附件存储仓库 */
    private final AttachmentRepository repository;

    /**
     * 构造器注入依赖。
     *
     * @param repository 附件仓库
     */
    public ParseAttachmentTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    /**
     * 返回 Tool 名称。
     *
     * @return Tool 名称
     */
    @Override
    public String name() {
        return "parse_attachment";
    }

    /**
     * 返回 Tool 描述，用于模型理解何时调用。
     *
     * @return Tool 描述
     */
    @Override
    public String description() {
        return "解析附件文本和元数据。仅当用户明确要求分析 attachmentId 对应附件时调用";
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
     * 执行附件解析。
     * <p>返回文件元数据（文件名、MIME 类型）和脱敏后的文本预览（前 80 字符）。</p>
     *
     * @param context Tool 执行上下文，包含参数
     * @return 包含 attachmentId、filename、contentType、textPreview 的结构化结果
     */
    @Override
    public ToolResult execute(ToolContext context) {
        String attachmentId = AttachmentToolSupport.argument(context.getArguments(), "attachmentId");
        AttachmentRecord record = repository.getRequired(attachmentId);
        // 附件正文可能包含证件号等敏感信息，Tool 返回前先脱敏，再截取预览。
        String text = AttachmentToolSupport.redactSensitiveText(record.getText());
        int previewLength = Math.min(text.length(), 80);
        return ToolResult.success("{attachmentId=" + attachmentId
                + ", filename=" + record.getFilename()
                + ", contentType=" + record.getContentType()
                + ", textPreview=" + text.substring(0, previewLength) + "}");
    }
}
