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
 * 生成附件分析摘要的只读 Tool。
 *
 * <p>该 Tool 汇总前面多个确定性 Tool 的口径，输出 AttachmentAnalysisResult，供模型或 HTTP API 直接返回。</p>
 *
 * @author Sean
 */
@Component
public class SummarizeAttachmentAnalysisTool implements AgentTool {

    /** 附件存储仓库 */
    private final AttachmentRepository repository;

    /**
     * 构造器注入依赖。
     *
     * @param repository 附件仓库
     */
    public SummarizeAttachmentAnalysisTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    /**
     * 返回 Tool 名称。
     *
     * @return Tool 名称
     */
    @Override
    public String name() {
        return "summarize_attachment_analysis";
    }

    /**
     * 返回 Tool 描述，用于模型理解何时调用。
     *
     * @return Tool 描述
     */
    @Override
    public String description() {
        return "生成附件分析摘要和审核意见。仅当用户明确要求分析 attachmentId 对应附件时调用";
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
     * 执行分析摘要生成。
     * <p>汇总分类、字段抽取和规则校验的结果，生成最终的 AttachmentAnalysisResult。</p>
     *
     * @param context Tool 执行上下文，包含参数
     * @return 包含完整分析结论的结构化结果
     */
    @Override
    public ToolResult execute(ToolContext context) {
        String attachmentId = AttachmentToolSupport.argument(context.getArguments(), "attachmentId");
        AttachmentRecord record = repository.getRequired(attachmentId);
        return ToolResult.success(AttachmentToolSupport.analysisResult(attachmentId, record));
    }
}
