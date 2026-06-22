package com.sean.agenthub.agent.example;

import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 示例工程使用的规则型模型供应商。
 *
 * <p>这样不用接真实大模型，也能验证 Tool 调用链路。它按关键词返回固定 ToolCall，
 * 用来演示 Runtime 如何处理正常查询、缺参数、Tool 失败和普通回答等路径。</p>
 *
 * @author Sean
 */
@Component
@ConditionalOnProperty(prefix = "agent.example.mock-model", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExampleModelProvider implements ModelProvider {
    @Override
    public ModelResponse chat(ModelRequest request) {
        if (request.getLastToolResult() != null) {
            // 第二轮调用表示 Runtime 已经执行完 Tool，示例模型只负责把 Tool 结果总结成用户可读文本。
            return ModelResponse.answer("查询结果：" + request.getLastToolResult().getData());
        }

        String message = request.getUserMessage() == null ? "" : request.getUserMessage();
        // 下面的分支是可预测的演示规则，方便在没有真实 LLM 的环境里稳定复现各类 ToolCall 场景。
        if (message.contains("字典")) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("code", "status");
            return ModelResponse.toolCall(new ToolCall("query_dict_item", arguments));
        }
        if (message.contains("文件")) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("fileId", "file-001");
            return ModelResponse.toolCall(new ToolCall("query_file_metadata", arguments));
        }
        if (message.contains("预算缺参数")) {
            return ModelResponse.toolCall(new ToolCall("query_budget_balance", new HashMap<String, Object>()));
        }
        if (message.contains("预算失败")) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("budgetCode", "BUDGET-FAIL");
            return ModelResponse.toolCall(new ToolCall("query_budget_balance", arguments));
        }
        if (message.contains("预算")) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("budgetCode", "BUDGET-001");
            return ModelResponse.toolCall(new ToolCall("query_budget_balance", arguments));
        }
        if (message.contains("用户")) {
            Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("userId", "u001");
            return ModelResponse.toolCall(new ToolCall("query_user_info_mock", arguments));
        }
        return ModelResponse.answer("示例模型收到：" + message);
    }
}
