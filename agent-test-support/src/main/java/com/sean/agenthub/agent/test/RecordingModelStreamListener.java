package com.sean.agenthub.agent.test;

import com.sean.agenthub.agent.core.api.ModelStreamListener;
import com.sean.agenthub.agent.core.model.ToolCall;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试用模型流式输出监听器。
 *
 * @author Sean
 */
public class RecordingModelStreamListener implements ModelStreamListener {
    private final StringBuilder answer = new StringBuilder();
    private final List<ToolCall> toolCalls = new ArrayList<ToolCall>();
    private boolean completed;
    private String error;

    @Override
    public void onDelta(String delta) {
        answer.append(delta);
    }

    @Override
    public void onToolCall(ToolCall toolCall) {
        toolCalls.add(toolCall);
    }

    @Override
    public void onComplete() {
        completed = true;
    }

    @Override
    public void onError(String error) {
        this.error = error;
    }

    public String getAnswer() {
        return answer.toString();
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getError() {
        return error;
    }
}
