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
 * <p>分类结果会被后续抽字段、规则校验和摘要 Tool 复用，是文档分析链路中的早期判断节点。</p>
 *
 * @author Sean
 */
@Component
public class ClassifyDocumentTool implements AgentTool {

    /** 附件存储仓库 */
    private final AttachmentRepository repository;

    /**
     * 构造器注入依赖。
     *
     * @param repository 附件仓库
     */
    public ClassifyDocumentTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    /**
     * 返回 Tool 名称。
     *
     * @return Tool 名称
     */
    @Override
    public String name() {
        return "classify_document";
    }

    /**
     * 返回 Tool 描述，用于模型理解何时调用。
     *
     * @return Tool 描述
     */
    @Override
    public String description() {
        return "识别附件类型。仅当用户明确要求分析 attachmentId 对应附件时调用";
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
     * 执行证件类型识别。
     * <p>通过关键词匹配识别附件类型，返回 ID_CARD、CONTRACT 或 UNKNOWN。</p>
     *
     * @param context Tool 执行上下文，包含参数
     * @return 包含 attachmentId 和 documentType 的结构化结果
     */
    @Override
    public ToolResult execute(ToolContext context) {
        String attachmentId = AttachmentToolSupport.argument(context.getArguments(), "attachmentId");
        AttachmentRecord record = repository.getRequired(attachmentId);
        return ToolResult.success("{attachmentId=" + attachmentId
                + ", documentType=" + AttachmentToolSupport.classify(record) + "}");
    }
}
