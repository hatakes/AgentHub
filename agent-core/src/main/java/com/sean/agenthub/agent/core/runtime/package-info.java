/**
 * Agent 执行链路和默认服务实现。
 *
 * <p>本包包含 AgentRuntime 的默认编排实现（DefaultAgentRuntime）、包装器基类（DelegatingAgentRuntime）
 * 和 AgentService 默认实现（DefaultAgentService）。DefaultAgentRuntime 是 agent-core 的主编排点，
 * 负责串起模型决策、Tool 受控执行、权限校验、审计记录和模型总结。</p>
 */
package com.sean.agenthub.agent.core.runtime;
