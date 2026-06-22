package com.sean.agenthub.agent.attachment.application;

import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import com.sean.agenthub.agent.core.model.ToolExecutionResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 文档处理能力使用的规则型模型供应商。
 *
 * <p>用于本地验证 AgentHub Tool 调度链路，不依赖真实大模型。它模拟一个会在识别到 attachmentId 和
 * "分析 / analyze"意图后，一次性规划多个 ToolCall 的模型。</p>
 *
 * @author Sean
 */
@Component
@ConditionalOnProperty(prefix = "attachment.mock-model", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AttachmentAnalysisModelProvider implements ModelProvider {

    /** 用于从用户消息中提取 attachmentId 的正则表达式，格式为 att- 后跟 36 位 UUID */
    private static final Pattern ATTACHMENT_ID_PATTERN = Pattern.compile("att-[0-9a-fA-F\\-]{36}");

    /**
     * 模拟模型对话入口。
     *
     * <p>第一轮：识别用户消息中的 attachmentId 和分析意图，返回多个 ToolCall。
     * 第二轮：Tool 执行完成后，将结果组织成最终回答。</p>
     *
     * @param request 模型请求，包含用户消息和上一轮 Tool 执行结果
     * @return 模型响应，可能是 ToolCall 列表或最终文本回答
     */
    @Override
    public ModelResponse chat(ModelRequest request) {
        if (request.getLastToolExecutions() != null && !request.getLastToolExecutions().isEmpty()) {
            // 第二轮调用表示 Runtime 已经顺序执行完全部 Tool，这里只把关键结果组织成最终回答。
            return ModelResponse.answer(buildAnswer(request.getLastToolExecutions()));
        }

        String message = request.getUserMessage() == null ? "" : request.getUserMessage();
        String attachmentId = findAttachmentId(message);
        if (attachmentId != null && (message.contains("分析") || message.toLowerCase().contains("analyze"))) {
            return ModelResponse.toolCalls(buildAnalysisToolCalls(attachmentId));
        }
        return ModelResponse.answer("文档处理能力收到：" + message);
    }

    /**
     * 构建附件分析的 Tool 调用链。
     * <p>模拟真实模型的任务拆解：先解析，再分类和抽字段，最后做规则判断与摘要。</p>
     *
     * @param attachmentId 附件 ID
     * @return 按顺序排列的 ToolCall 列表
     */
    private List<ToolCall> buildAnalysisToolCalls(String attachmentId) {
        List<ToolCall> toolCalls = new ArrayList<ToolCall>();
        // 这组 ToolCall 模拟真实模型的任务拆解：先解析，再分类和抽字段，最后做规则判断与摘要。
        toolCalls.add(new ToolCall("parse_attachment", arguments(attachmentId)));
        toolCalls.add(new ToolCall("classify_document", arguments(attachmentId)));
        toolCalls.add(new ToolCall("extract_document_fields", arguments(attachmentId)));
        toolCalls.add(new ToolCall("check_document_rules", arguments(attachmentId)));
        toolCalls.add(new ToolCall("summarize_attachment_analysis", arguments(attachmentId)));
        return toolCalls;
    }

    /**
     * 构建包含 attachmentId 参数的 Map。
     *
     * @param attachmentId 附件 ID
     * @return 参数 Map
     */
    private Map<String, Object> arguments(String attachmentId) {
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("attachmentId", attachmentId);
        return arguments;
    }

    /**
     * 从用户消息中提取 attachmentId。
     *
     * @param message 用户消息文本
     * @return 匹配到的 attachmentId，未匹配则返回 null
     */
    private String findAttachmentId(String message) {
        Matcher matcher = ATTACHMENT_ID_PATTERN.matcher(message);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * 将 Tool 执行结果组装为最终文本回答。
     * <p>优先使用 summarize_attachment_analysis Tool 的结果作为主体，否则取最后一个 Tool 的结果。</p>
     *
     * @param executions Tool 执行结果列表
     * @return 面向用户的最终回答文本
     */
    private String buildAnswer(List<ToolExecutionResult> executions) {
        StringBuilder answer = new StringBuilder();
        answer.append("附件分析结果：");
        for (ToolExecutionResult execution : executions) {
            if ("summarize_attachment_analysis".equals(execution.getToolCall().getName())) {
                // 摘要 Tool 已经返回面向业务的结构化结论，优先使用它作为最终答案主体。
                answer.append(execution.getToolResult().getData());
                return answer.toString();
            }
        }
        answer.append(executions.get(executions.size() - 1).getToolResult().getData());
        return answer.toString();
    }
}
