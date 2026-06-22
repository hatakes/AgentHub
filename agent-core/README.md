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

## 核心代码阅读入口

建议先读这些类型的类注释和关键方法注释：

```text
api/ModelProvider.java       为什么 core 通过接口隔离模型厂商和 AI 框架
api/AgentTool.java           为什么业务能力必须注册成受控 Tool
api/AgentRuntime.java        Runtime 只负责编排，不承载具体业务规则
api/PermissionEngine.java    为什么权限检查统一放在 Tool 执行前
api/AuditService.java        为什么 Tool 调用必须统一审计
runtime/DefaultAgentRuntime.java  模型决策、Tool 执行、权限审计、模型总结的主链路
runtime/DelegatingAgentRuntime.java 增强默认链路时使用的 Runtime 包装器基类
```

阅读顺序：

```text
ModelProvider / AgentTool 抽象边界
-> DefaultAgentRuntime.run 非流式主链路
-> DefaultAgentRuntime.executeTool 安全执行边界
-> DefaultAgentRuntime.runStream 流式链路
```

## 当前默认实现

```text
DefaultAgentService
DefaultAgentRuntime
DelegatingAgentRuntime
InMemoryToolRegistry
InMemoryAgentMemory
NoopPermissionEngine
ConsoleAuditService
EchoModelProvider
```

## AgentRuntime 的设计模式

`AgentRuntime` 采用“接口 + 默认实现 + 可选包装器”的设计。

```text
AgentRuntime
  定义一次 Agent 执行链路的稳定接口。

DefaultAgentRuntime
  提供默认编排实现：调用模型、判断 ToolCall、校验参数、检查权限、执行 Tool、写审计、再让模型总结。

业务自定义 AgentRuntime
  当业务需要完全改变执行顺序时，实现 AgentRuntime 并注册为 Spring Bean。

DelegatingAgentRuntime / AgentRuntime 包装器
  当业务只想增强默认链路时，内部持有一个 DefaultAgentRuntime，把请求委托给它，前后追加日志、指标、Trace、限流等逻辑。
```

为什么这样设计：

```text
1. 上层只依赖 AgentRuntime 接口，不绑定 DefaultAgentRuntime。
2. DefaultAgentRuntime 可以持续承载平台默认能力，避免每个业务复制一份复杂编排。
3. Spring Starter 使用 @ConditionalOnMissingBean，业务声明自己的 AgentRuntime Bean 后可以覆盖默认实现。
4. 包装器适合增强默认链路，不破坏默认执行顺序，也不需要继承 DefaultAgentRuntime。
```

推荐使用方式：

```text
只加日志、指标、Trace、异常转换、限流
  -> 使用包装器 Decorator，内部委托 DefaultAgentRuntime。

要改变核心执行顺序，例如多轮规划、人工审批、异步任务队列、不同 Tool 执行策略
  -> 新写一个 AgentRuntime 实现。
```

包装器示例：

```java
public class LoggingAgentRuntime extends DelegatingAgentRuntime {
    public LoggingAgentRuntime(AgentRuntime delegate) {
        super(delegate);
    }

    @Override
    public AgentResponse run(AgentRequest request, AgentContext context) {
        long startedAt = System.currentTimeMillis();
        try {
            return super.run(request, context);
        } finally {
            System.out.println("agent runtime latencyMs=" + (System.currentTimeMillis() - startedAt));
        }
    }

    @Override
    public void runStream(AgentRequest request, AgentContext context, AgentStreamListener listener) {
        super.runStream(request, context, listener);
    }
}
```

在 Spring Boot 中覆盖默认 Runtime 时，建议只向容器暴露一个 `AgentRuntime` Bean：

```java
@Bean
public AgentRuntime agentRuntime(ModelProvider modelProvider,
                                 ToolRegistry toolRegistry,
                                 AgentMemory agentMemory,
                                 PermissionEngine permissionEngine,
                                 AuditService auditService) {
    AgentRuntime defaultRuntime = new DefaultAgentRuntime(
            modelProvider,
            toolRegistry,
            agentMemory,
            permissionEngine,
            auditService
    );
    return new LoggingAgentRuntime(defaultRuntime);
}
```

不要同时暴露多个 `AgentRuntime` Bean，除非明确使用 `@Primary` 或限定名，否则上层注入时容易歧义。

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

这些约束也体现在代码注释里：core 只定义抽象和执行顺序，具体模型、数据库、业务 Tool、权限体系和审计落库都通过接口实现接入。

后续扩展 PostgreSQL、Redis、真实模型供应商、MCP 或 Gateway 时，应通过接口新增实现，不应让 `agent-core` 反向依赖上层模块。
