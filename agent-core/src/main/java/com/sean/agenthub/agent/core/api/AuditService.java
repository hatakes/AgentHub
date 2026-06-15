package com.sean.agenthub.agent.core.api;

import com.sean.agenthub.agent.core.model.AuditEvent;

/**
 * 审计记录接口。
 *
 * <p>Agent 应用的风险不只在最终回答，还在中间 Tool 调用了什么、用什么参数调用、是否成功、
 * 耗时多久以及失败原因是什么。审计接口放在 core，是为了让所有 Tool 执行都能统一落审计，
 * 后续可以替换成数据库、日志平台或可观测性系统实现。</p>
 *
 * @author Sean
 */
public interface AuditService {
    /**
     * 记录一次 Agent 或 Tool 执行事件。
     *
     * <p>实现需要注意脱敏，不应把密钥、完整证件号、完整业务敏感数据直接写入日志或数据库。</p>
     */
    void record(AuditEvent event);
}
