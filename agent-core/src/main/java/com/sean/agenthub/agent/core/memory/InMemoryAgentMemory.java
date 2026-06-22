package com.sean.agenthub.agent.core.memory;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于进程内存的会话记忆实现。
 *
 * <p>适合本地开发和 MVP 验证，生产环境可替换为 Redis、PostgreSQL 等持久化实现。</p>
 *
 * @author Sean
 */
public class InMemoryAgentMemory implements AgentMemory {
    /** 以会话 ID 为 key 的消息存储，使用 ConcurrentHashMap 保证线程安全。 */
    private final Map<String, List<AgentMessage>> messages = new ConcurrentHashMap<String, List<AgentMessage>>();

    /**
     * 加载指定会话的历史消息。
     *
     * @param sessionId 会话 ID
     * @return 历史消息列表，不存在时返回空列表
     */
    @Override
    public List<AgentMessage> load(String sessionId) {
        List<AgentMessage> history = messages.get(normalizeSessionId(sessionId));
        if (history == null) {
            return Collections.emptyList();
        }
        return new ArrayList<AgentMessage>(history);
    }

    /**
     * 保存一条消息到指定会话。
     *
     * @param sessionId 会话 ID
     * @param message   待保存的消息
     */
    @Override
    public void save(String sessionId, AgentMessage message) {
        String key = normalizeSessionId(sessionId);
        if (!messages.containsKey(key)) {
            messages.put(key, Collections.synchronizedList(new ArrayList<AgentMessage>()));
        }
        messages.get(key).add(message);
    }

    /**
     * 清除指定会话的所有历史消息。
     *
     * @param sessionId 会话 ID
     */
    @Override
    public void clear(String sessionId) {
        messages.remove(normalizeSessionId(sessionId));
    }

    /**
     * 规范化会话 ID，null 或空字符串统一为 "default"。
     *
     * @param sessionId 原始会话 ID
     * @return 规范化后的会话 ID
     */
    private String normalizeSessionId(String sessionId) {
        return sessionId == null || sessionId.trim().isEmpty() ? "default" : sessionId;
    }
}
