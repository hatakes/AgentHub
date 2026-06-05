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
    private final Map<String, List<AgentMessage>> messages = new ConcurrentHashMap<String, List<AgentMessage>>();

    @Override
    public List<AgentMessage> load(String sessionId) {
        List<AgentMessage> history = messages.get(normalizeSessionId(sessionId));
        if (history == null) {
            return Collections.emptyList();
        }
        return new ArrayList<AgentMessage>(history);
    }

    @Override
    public void save(String sessionId, AgentMessage message) {
        String key = normalizeSessionId(sessionId);
        if (!messages.containsKey(key)) {
            messages.put(key, Collections.synchronizedList(new ArrayList<AgentMessage>()));
        }
        messages.get(key).add(message);
    }

    @Override
    public void clear(String sessionId) {
        messages.remove(normalizeSessionId(sessionId));
    }

    private String normalizeSessionId(String sessionId) {
        return sessionId == null || sessionId.trim().isEmpty() ? "default" : sessionId;
    }
}
