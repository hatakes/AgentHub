package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.AgentMessage;

import java.util.List;

/**
 * 会话记忆接口。
 *
 * @author Sean
 */
public interface AgentMemory {
    /**
     * 加载指定会话的历史消息。
     */
    List<AgentMessage> load(String sessionId);

    /**
     * 保存一条会话消息。
     */
    void save(String sessionId, AgentMessage message);

    /**
     * 清理指定会话的历史消息。
     */
    void clear(String sessionId);
}
