# agent-core

`agent-core` 是 AgentHub 的核心抽象模块。

它只定义 Agent 能力平台的基础接口、运行时模型和少量默认实现，不依赖 Spring、Servlet、数据库、HTTP Client 或具体模型厂商 SDK。

## 职责

```text
定义 Agent 统一入口
定义 Agent 执行链路
定义 Tool 插件协议
定义模型供应商适配接口
定义会话记忆接口
定义权限检查接口
定义审计记录接口
提供 MVP 阶段可运行的默认实现
```

## 包结构

```text
com.sean.agenthub.agent.core.api         核心扩展接口
com.sean.agenthub.agent.core.capability  模型、Tool、MCP 等能力声明
com.sean.agenthub.agent.core.model       请求、响应、上下文和事件模型
com.sean.agenthub.agent.core.runtime     Agent 执行链路和默认服务实现
com.sean.agenthub.agent.core.tool        Tool Schema、Tool 上下文和 ToolRegistry 实现
com.sean.agenthub.agent.core.memory      Memory 默认实现
com.sean.agenthub.agent.core.permission  Permission 默认实现
com.sean.agenthub.agent.core.audit       Audit 默认实现
com.sean.agenthub.agent.core.provider    ModelProvider 默认实现
```

## 关键类型

```text
AgentService       Agent 统一服务入口
AgentRuntime       一次 Agent 执行链路
AgentTool          可被 Agent 调用的业务能力
ToolRegistry       Tool 注册中心
ModelProvider      模型供应商适配接口
ModelProviderCapability 模型供应商能力声明
ModelStreamListener 模型流式输出回调
AgentStreamListener Agent 流式输出回调
AgentMemory        会话记忆接口
PermissionEngine   Tool 调用权限检查接口
AuditService       审计记录接口
```

## 当前默认实现

```text
DefaultAgentService
DefaultAgentRuntime
InMemoryToolRegistry
InMemoryAgentMemory
NoopPermissionEngine
ConsoleAuditService
EchoModelProvider
```

## 测试

从仓库根目录执行：

```bash
mvn test
```

当前已覆盖：

```text
InMemoryToolRegistry 注册和查询
InMemoryAgentMemory 保存、加载、清理和默认 session
DefaultAgentRuntime 直接回答链路
DefaultAgentRuntime Tool 调用和总结链路
DefaultAgentRuntime 多 ToolCall 调用和总结链路
DefaultAgentRuntime 流式直接回答链路
DefaultAgentRuntime 流式 ToolCall 调用和流式总结链路
EchoModelProvider 能力声明
DefaultAgentRuntime Tool 必填参数缺失拦截
DefaultAgentRuntime 权限拒绝时不执行 Tool
```

## 设计约束

```text
保持 JDK 8 兼容
不依赖 Spring
不直接依赖数据库
不直接依赖具体模型厂商 SDK
MVP 阶段只允许 READ Tool
```

后续扩展 PostgreSQL、Redis、真实模型供应商、MCP 或 Gateway 时，应通过接口新增实现，不应让 `agent-core` 反向依赖上层模块。
