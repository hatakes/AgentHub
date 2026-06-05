package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.AuditEvent;

/**
 * 审计记录接口。
 *
 * @author Sean
 */
public interface AuditService {
    /**
     * 记录一次 Agent 或 Tool 执行事件。
     */
    void record(AuditEvent event);
}
