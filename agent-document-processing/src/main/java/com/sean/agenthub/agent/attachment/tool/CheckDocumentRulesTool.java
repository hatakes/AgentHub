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
 * <p>规则判断放在 Tool 中，而不是交给模型自由发挥，保证年龄门槛这类合规结论可重复、可测试、可审计。</p>
 *
 * @author Sean
 */
@Component
public class CheckDocumentRulesTool implements AgentTool {

    /** 附件存储仓库 */
    private final AttachmentRepository repository;

    /**
     * 构造器注入依赖。
     *
     * @param repository 附件仓库
     */
    public CheckDocumentRulesTool(AttachmentRepository repository) {
        this.repository = repository;
    }

    /**
     * 返回 Tool 名称。
     *
     * @return Tool 名称
     */
    @Override
    public String name() {
        return "check_document_rules";
    }

    /**
     * 返回 Tool 描述，用于模型理解何时调用。
     *
     * @return Tool 描述
     */
    @Override
    public String description() {
        return "执行附件业务规则校验。仅当用户明确要求分析 attachmentId 对应附件时调用";
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
     * 执行业务规则校验。
     * <p>当前规则：根据出生日期计算年龄，判断是否成年（>=18 岁）。
     * 识别不到出生日期时不过度拒绝，交给最终审核意见说明信息不足。</p>
     *
     * @param context Tool 执行上下文，包含参数
     * @return 包含 ruleCode、age、adult、passed 的结构化结果
     */
    @Override
    public ToolResult execute(ToolContext context) {
        String attachmentId = AttachmentToolSupport.argument(context.getArguments(), "attachmentId");
        AttachmentRecord record = repository.getRequired(attachmentId);
        String birthDate = AttachmentToolSupport.extractBirthDate(record);
        int age = AttachmentToolSupport.ageOn2026(birthDate);
        boolean adult = age >= 18;
        String ruleCode = adult ? "AGE_ADULT" : "AGE_UNDER_18";
        // 识别不到出生日期时不过度拒绝，交给最终审核意见说明信息不足。
        boolean passed = adult || age < 0;
        return ToolResult.success("{attachmentId=" + attachmentId
                + ", ruleCode=" + ruleCode
                + ", age=" + age
                + ", adult=" + adult
                + ", passed=" + passed + "}");
    }
}
