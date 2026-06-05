package com.sean.agenthub.agent.core.audit;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 将审计事件输出到控制台的默认实现。
 *
 * <p>仅用于开发和 MVP 验证，后续可替换为数据库或日志平台实现。</p>
 *
 * @author Sean
 */
public class ConsoleAuditService implements AuditService {
    @Override
    public void record(AuditEvent event) {
        System.out.println("[agent-audit] traceId=" + event.getTraceId()
                + ", sessionId=" + event.getSessionId()
                + ", userId=" + event.getUserId()
                + ", toolName=" + event.getToolName()
                + ", success=" + event.isSuccess()
                + ", latencyMs=" + event.getLatencyMs()
                + ", error=" + event.getErrorMessage());
    }
}
