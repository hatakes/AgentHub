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
    /** 默认每个会话最多保留的消息数，避免本地内存无限增长。 */
    public static final int DEFAULT_MAX_MESSAGES_PER_SESSION = 200;

    /** 以会话 ID 为 key 的消息存储，使用 ConcurrentHashMap 保证线程安全。 */
    private final Map<String, List<AgentMessage>> messages = new ConcurrentHashMap<String, List<AgentMessage>>();
    /** 每个会话最多保留的消息数。 */
    private final int maxMessagesPerSession;

    /**
     * 创建默认内存会话记忆。
     */
    public InMemoryAgentMemory() {
        this(DEFAULT_MAX_MESSAGES_PER_SESSION);
    }

    /**
     * 创建带会话消息上限的内存会话记忆。
     *
     * @param maxMessagesPerSession 每个会话最多保留的消息数，必须大于 0
     */
    public InMemoryAgentMemory(int maxMessagesPerSession) {
        if (maxMessagesPerSession <= 0) {
            throw new IllegalArgumentException("maxMessagesPerSession must be greater than 0");
        }
        this.maxMessagesPerSession = maxMessagesPerSession;
    }

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
            messages.putIfAbsent(key, Collections.synchronizedList(new ArrayList<AgentMessage>()));
        }
        List<AgentMessage> history = messages.get(key);
        synchronized (history) {
            history.add(message);
            while (history.size() > maxMessagesPerSession) {
                history.remove(0);
            }
        }
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
