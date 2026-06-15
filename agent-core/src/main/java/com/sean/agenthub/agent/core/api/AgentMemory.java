package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.AgentMessage;

import java.util.List;

/**
 * 会话记忆接口。
 *
 * <p>Memory 用来保存会话上下文，使 Runtime 在下一次调用模型时可以带上历史消息。
 * 当前 core 只定义抽象和内存实现，不直接绑定数据库或 Redis。生产系统可以按需要替换成持久化、
 * 分租户或带过期策略的实现。</p>
 *
 * @author Sean
 */
public interface AgentMemory {
    /**
     * 加载指定会话的历史消息。
     *
     * <p>Runtime 会把这些消息放进 ModelRequest，provider 再转换成具体模型协议需要的 messages。</p>
     */
    List<AgentMessage> load(String sessionId);

    /**
     * 保存一条会话消息。
     *
     * <p>默认 Runtime 会保存用户输入和最终助手回答。Tool 中间结果当前不作为普通对话消息保存，
     * 而是通过 ModelRequest.lastToolExecutions 传给模型总结。</p>
     */
    void save(String sessionId, AgentMessage message);

    /**
     * 清理指定会话的历史消息。
     */
    void clear(String sessionId);
}
