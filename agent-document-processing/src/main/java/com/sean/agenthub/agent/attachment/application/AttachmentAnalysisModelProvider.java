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
 * <p>用于本地验证 AgentHub Tool 调度链路，不依赖真实大模型。</p>
 *
 * @author Sean
 */
@Component
@ConditionalOnProperty(prefix = "attachment.mock-model", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AttachmentAnalysisModelProvider implements ModelProvider {
    private static final Pattern ATTACHMENT_ID_PATTERN = Pattern.compile("att-[0-9a-fA-F\\-]{36}");

    @Override
    public ModelResponse chat(ModelRequest request) {
        if (request.getLastToolExecutions() != null && !request.getLastToolExecutions().isEmpty()) {
            return ModelResponse.answer(buildAnswer(request.getLastToolExecutions()));
        }

        String message = request.getUserMessage() == null ? "" : request.getUserMessage();
        String attachmentId = findAttachmentId(message);
        if (attachmentId != null && (message.contains("分析") || message.toLowerCase().contains("analyze"))) {
            return ModelResponse.toolCalls(buildAnalysisToolCalls(attachmentId));
        }
        return ModelResponse.answer("文档处理能力收到：" + message);
    }

    private List<ToolCall> buildAnalysisToolCalls(String attachmentId) {
        List<ToolCall> toolCalls = new ArrayList<ToolCall>();
        toolCalls.add(new ToolCall("parse_attachment", arguments(attachmentId)));
        toolCalls.add(new ToolCall("classify_document", arguments(attachmentId)));
        toolCalls.add(new ToolCall("extract_document_fields", arguments(attachmentId)));
        toolCalls.add(new ToolCall("check_document_rules", arguments(attachmentId)));
        toolCalls.add(new ToolCall("summarize_attachment_analysis", arguments(attachmentId)));
        return toolCalls;
    }

    private Map<String, Object> arguments(String attachmentId) {
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("attachmentId", attachmentId);
        return arguments;
    }

    private String findAttachmentId(String message) {
        Matcher matcher = ATTACHMENT_ID_PATTERN.matcher(message);
        return matcher.find() ? matcher.group() : null;
    }

    private String buildAnswer(List<ToolExecutionResult> executions) {
        StringBuilder answer = new StringBuilder();
        answer.append("附件分析结果：");
        for (ToolExecutionResult execution : executions) {
            if ("summarize_attachment_analysis".equals(execution.getToolCall().getName())) {
                answer.append(execution.getToolResult().getData());
                return answer.toString();
            }
        }
        answer.append(executions.get(executions.size() - 1).getToolResult().getData());
        return answer.toString();
    }
}
