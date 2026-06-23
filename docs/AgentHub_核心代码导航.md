# AgentHub 核心代码导航

> 用途：给项目负责人一个可以直接跳转的代码入口。先看这些类，就能理解 AgentHub 当前主链路和业务样板，不需要从全仓库随机翻起。

## 1. 什么算核心代码

本项目里的“核心代码”按影响范围定义，不按代码行数定义。

```text
核心代码 =
HTTP 入口
+ Spring Boot 自动装配
+ AgentRuntime 主编排
+ ModelProvider 模型抽象
+ AgentTool / ToolRegistry 扩展契约
+ PermissionEngine / AuditService 横切能力
+ 当前业务样板主链路
```

暂时不优先看的代码：

```text
普通 getter / setter
单纯 DTO 字段
具体 provider 的边角解析逻辑
Spike 模块的框架适配细节
尚未进入主链路的规划文档
```

## 2. 推荐阅读顺序

```text
第 1 步：入口 — AgentChatController（第 3、4 章）
  理解 /agent/chat 请求如何进入系统，HTTP 层只做协议转换

第 2 步：装配 — AgentAutoConfiguration（第 5 章）
  理解 Spring Boot 如何组装 AgentHub 组件，谁创建了谁

第 3 步：编排核心 — DefaultAgentRuntime（第 6 章）
  理解 6 步执行链路：模型决策 → 受控 Tool 执行 → 模型总结

第 4 步：模型隔离 — ModelProvider（第 7、9 章）
  理解 ModelRequest/ModelResponse 如何隔离具体模型协议

第 5 步：业务能力 — AgentTool（第 8、10 章）
  理解 Tool 的 5 层安全边界和扩展契约
```

## 3. 主链路入口

| 顺序 | 类 / 文档 | 作用 | 重点看 |
|---:|---|---|---|
| 1 | [AgentChatController.java](../agent-spring-boot-starter/src/main/java/com/sean/agenthub/agent/starter/AgentChatController.java) | `/agent/chat` 和 `/agent/chat/stream` HTTP 入口 | `chat`、`streamChat` |
| 2 | [AgentAutoConfiguration.java](../agent-spring-boot-starter/src/main/java/com/sean/agenthub/agent/starter/AgentAutoConfiguration.java) | Starter 自动装配 AgentHub 运行组件 | `agentRuntime`、`agentService`、`agentToolRegistry` |
| 3 | [DefaultAgentService.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/runtime/DefaultAgentService.java) | 把业务请求转成 Runtime 调用 | `chat`、`streamChat` |
| 4 | [DefaultAgentRuntime.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/runtime/DefaultAgentRuntime.java) | AgentHub 最核心执行编排 | `run`、`runStream`、`executeTool`、`buildModelRequest` |
| 5 | [ModelProvider.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/ModelProvider.java) | 模型供应商抽象 | `chat`、`streamChat`、`capabilities` |
| 6 | [AgentTool.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/AgentTool.java) | Tool 插件契约 | `name`、`description`、`schema`、`execute` |
| 7 | [ToolRegistry.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/ToolRegistry.java) | Tool 注册和查找接口 | `list`、`get` |
| 8 | [PermissionEngine.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/PermissionEngine.java) | Tool 执行前权限判断 | `check` |
| 9 | [AuditService.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/AuditService.java) | Tool 执行审计 | `record` |
| 10 | [AgentMemory.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/AgentMemory.java) | 会话记忆抽象 | `load`、`save`、`clear` |

## 4. 分层架构：每层只依赖下一层

```text
┌─────────────────────────────────────────────────────────────┐
│  Layer 1: HTTP 入口                                          │
│  AgentChatController                                        │
│  职责：HTTP 协议转换（JSON ↔ AgentRequest/AgentResponse）      │
│  不碰：业务逻辑、模型协议、Tool 执行                            │
└───────────────────────────┬─────────────────────────────────┘
                            │ agentService.chat(request)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  Layer 2: 服务层                                             │
│  DefaultAgentService                                        │
│  职责：从 AgentRequest 提取 userId/attributes → 构建 UserContext │
│  不碰：模型调用、Tool 执行                                     │
└───────────────────────────┬─────────────────────────────────┘
                            │ runtime.run(request, context)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  Layer 3: 编排核心                                            │
│  DefaultAgentRuntime                                        │
│  职责：模型决策 → 受控 Tool 执行 → 模型总结                     │
│  依赖：ModelProvider、ToolRegistry、PermissionEngine、        │
│        AuditService、AgentMemory（全部是接口）                 │
└───────┬──────────┬──────────┬──────────┬───────────┬────────┘
        │          │          │          │           │
        ▼          ▼          ▼          ▼           ▼
   ModelProvider  ToolRegistry  PermissionEngine  AuditService  AgentMemory
   (模型抽象)     (Tool注册)    (权限检查)        (审计记录)     (会话记忆)
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  Layer 4: 模型适配                                            │
│  OpenAiCompatibleModelProvider / AnthropicCompatibleModelProvider │
│  职责：ModelRequest ↔ 具体模型协议的转换                        │
│  不碰：Tool 执行、权限、审计                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Layer 5: 业务能力                                            │
│  AgentTool 实现（如 ParseAttachmentTool、ClassifyDocumentTool）│
│  职责：纯粹的业务执行逻辑                                      │
│  不碰：权限判断、审计记录（由 Runtime 统一处理）                 │
└─────────────────────────────────────────────────────────────┘
```

**关键设计约束**：业务代码不直接调用具体模型厂商，只依赖 ModelProvider 抽象；
模型返回的 ToolCall 不直接执行，必须经过 Runtime 的安全边界。

## 5. Spring Boot 组件装配图

AgentAutoConfiguration 负责把 agent-core 的纯 Java 抽象接到 Spring 容器：

```text
AgentProperties (application.yml)
  │
  ├─ agent.model.protocol=echo/openai/anthropic
  │     │
  │     ▼
  │   @Bean ModelProvider
  │     ├─ "echo"      → EchoModelProvider（本地演示）
  │     ├─ "openai"    → OpenAiCompatibleModelProvider（/v1/chat/completions）
  │     └─ "anthropic" → AnthropicCompatibleModelProvider（/v1/messages）
  │
  ├─ agent.tools.allowed-names=[...]
  │     │
  │     ▼
  │   @Bean ToolRegistry
  │     └─ 收集容器中所有 AgentTool Bean，按白名单过滤后注册
  │
  ├─ @Bean AgentMemory      → InMemoryAgentMemory
  ├─ @Bean PermissionEngine  → NoopPermissionEngine
  ├─ @Bean AuditService      → ConsoleAuditService
  │
  ▼
  @Bean AgentRuntime → DefaultAgentRuntime(5 个组件)
       │
       ▼
  @Bean AgentService → DefaultAgentService(runtime)
       │
       ▼
  @Bean AgentChatController → AgentChatController(agentService)
```

**所有 Bean 都是 @ConditionalOnMissingBean**：业务侧声明同类型 Bean 即可覆盖默认实现，
Starter 不会抢占业务侧的显式配置。

## 6. 一次 `/agent/chat` 的完整执行链路

```text
curl -X POST /agent/chat -d '{"sessionId":"s1","userId":"u1","message":"帮我查询用户信息"}'
  │
  ▼
AgentChatController.chat()                    ← HTTP 入口，Jackson 反序列化为 AgentRequest
  │
  ▼
DefaultAgentService.chat()                    ← 构建 UserContext(userId=u1, attributes)
  │                                              组装 AgentContext(user)
  ▼
DefaultAgentRuntime.run(request, context)     ← 编排核心，6 步链路：
  │
  │  ┌─ Step 1 ─────────────────────────────────────────────────────┐
  │  │ memory.save(sessionId, AgentMessage("user", "帮我查询用户信息")) │
  │  └──────────────────────────────────────────────────────────────┘
  │
  │  ┌─ Step 2 ─────────────────────────────────────────────────────┐
  │  │ buildModelRequest()                                          │
  │  │   ├─ memory.load(sessionId)         → 历史消息列表            │
  │  │   ├─ toolRegistry.list()            → 可用 Tool 快照         │
  │  │   └─ buildToolSelectionPrompt()     → 防误触发 system prompt │
  │  └──────────────────────────────────────────────────────────────┘
  │
  │  ┌─ Step 3 ─────────────────────────────────────────────────────┐
  │  │ modelProvider.chat(modelRequest)     ← 第一次调模型           │
  │  │   │                                                          │
  │  │   ├─ ModelResponse.answer 有值 → 直接跳到 Step 6             │
  │  │   └─ ModelResponse.toolCalls 有值 → 继续 Step 4              │
  │  └──────────────────────────────────────────────────────────────┘
  │
  │  ┌─ Step 4 ─────────────────────────────────────────────────────┐
  │  │ for each ToolCall:                                           │
  │  │   ├─ toolRegistry.get(name)         ← 找不到 = 模型越界      │
  │  │   ├─ validateReadOnly(tool)         ← MVP 只允许 READ        │
  │  │   ├─ validateRequiredArguments()    ← 必填参数校验            │
  │  │   ├─ permissionEngine.check()       ← 权限检查               │
  │  │   ├─ tool.execute(context)          ← 业务执行               │
  │  │   └─ auditService.record()          ← 审计记录（无论成败）    │
  │  └──────────────────────────────────────────────────────────────┘
  │
  │  ┌─ Step 5 ─────────────────────────────────────────────────────┐
  │  │ modelProvider.chat(Tool 结果)        ← 第二次调模型，总结回答 │
  │  └──────────────────────────────────────────────────────────────┘
  │
  │  ┌─ Step 6 ─────────────────────────────────────────────────────┐
  │  │ memory.save(sessionId, AgentMessage("assistant", 总结回答))   │
  │  └──────────────────────────────────────────────────────────────┘
  │
  ▼
AgentResponse {ok=true, answer="查询结果：...", toolCalls=[...]}
```

对应必读文件：

```text
agent-spring-boot-starter/.../AgentChatController.java         ← Step 0: HTTP 入口
agent-core/.../runtime/DefaultAgentService.java                 ← Step 0: 上下文组装
agent-core/.../runtime/DefaultAgentRuntime.java                 ← Step 1-6: 编排核心
agent-core/.../api/ModelProvider.java                           ← Step 3,5: 模型抽象
agent-core/.../api/AgentTool.java                               ← Step 4: Tool 契约
```

读 `DefaultAgentRuntime` 时优先看这些注释块：

```text
类注释：为什么 Runtime 只依赖抽象，不依赖具体模型或业务系统
run：第一次模型决策、Tool 顺序执行、第二次模型总结
runStream：为什么流式 ToolCall 要先聚合决策再执行 Tool
buildModelRequest：为什么只在存在 Tool 时注入工具选择提示词
executeTool：ToolRegistry、READ 限制、参数校验、权限校验、审计的顺序
```

## 7. ModelProvider 隔离了什么

```text
                ModelRequest                    ModelResponse
业务代码 / Runtime  ──────────→  ModelProvider  ──────────→  业务代码 / Runtime
(统一模型)                      │                            (统一模型)
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
              OpenAI 协议  Anthropic 协议  Echo 协议
              /v1/chat/    /v1/messages    直接回显
              completions
```

Runtime 不关心具体模型协议。ModelRequest 包含：
- messages（历史消息）
- tools（可用 Tool 列表）
- systemPrompt（系统提示词）
- toolChoice（Tool 选择策略）
- lastToolExecutions（Tool 执行结果，用于总结阶段）

ModelResponse 只有两种形态：
- `.answer` 有值 → 文本回答
- `.toolCalls` 有值 → 模型要求执行 Tool

## 8. AgentTool 安全边界详解

Tool 不是被模型直接调用的。模型只能返回 ToolCall 意图，真正的执行必须经过 Runtime 的 5 层安全检查：

```text
模型返回 ToolCall {name="query_user", arguments={userId="u1"}}
  │
  ▼
① ToolRegistry.get("query_user")     ← 名称匹配，找不到拒绝
  │
  ▼
② validateReadOnly(tool)             ← riskLevel 必须是 READ
  │
  ▼
③ validateRequiredArguments()        ← schema.required 中的字段必须存在
  │
  ▼
④ permissionEngine.check(user, tool) ← 权限引擎判断是否允许
  │
  ▼
⑤ tool.execute(context)              ← 执行业务逻辑
  │
  ▼
⑥ auditService.record(auditEvent)    ← 记录审计（finally 块，无论成败）
```

这个设计保证：即使模型误返回 ToolCall，也必须通过安全边界才能执行。

## 9. 模型适配入口

| 类 / 文档 | 作用 | 重点看 |
|---|---|---|
| [OpenAiCompatibleModelProvider.java](../agent-model-provider-http/src/main/java/com/sean/agenthub/agent/provider/http/OpenAiCompatibleModelProvider.java) | OpenAI-compatible Chat Completions 协议适配 | `chat`、`streamChat`、`capabilities` |
| [AnthropicCompatibleModelProvider.java](../agent-model-provider-http/src/main/java/com/sean/agenthub/agent/provider/http/AnthropicCompatibleModelProvider.java) | Anthropic-compatible Messages 协议适配 | `chat`、`streamChat`、`capabilities` |
| [ModelProviderCapability.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/capability/ModelProviderCapability.java) | 模型能力矩阵 | `TEXT_CHAT`、`TEXT_STREAM`、`TOOL_CALL`、`STRUCTURED_OUTPUT` |
| [ResponseFormat.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/model/ResponseFormat.java) | Structured Output 请求约束 | `jsonSchema` |
| [agent-model-provider-http/README.md](../agent-model-provider-http/README.md) | HTTP provider 使用说明 | 能力边界、配置示例 |

先理解：

```text
业务代码不直接调用具体模型厂商。
业务系统只依赖 AgentHub 的 ModelProvider 抽象。
OpenAI / Anthropic / DeepSeek / MiMo 这类差异收敛在 provider 模块。
```

## 10. Tool 扩展入口

| 类 / 文档 | 作用 | 重点看 |
|---|---|---|
| [AgentTool.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/AgentTool.java) | 所有业务 Tool 必须实现的接口 | `schema`、`riskLevel`、`execute` |
| [ToolSchema.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/tool/ToolSchema.java) | Tool 入参 JSON Schema 子集 | `properties`、`required` |
| [ToolResult.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/tool/ToolResult.java) | Tool 执行结果 | `success`、`error` |
| [ToolContext.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/tool/ToolContext.java) | Tool 执行上下文 | `user`、`arguments` |
| [InMemoryToolRegistry.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/tool/InMemoryToolRegistry.java) | 默认内存注册中心 | `register`、`get`、`list` |

判断一个业务能力是否应该做成 Tool：

```text
需要被 Agent 调用
有明确输入输出
能做权限控制
能记录审计
失败后可以给出可读错误
```

## 11. 权限和审计入口

| 类 / 文档 | 作用 | 重点看 |
|---|---|---|
| [PermissionEngine.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/PermissionEngine.java) | 权限抽象 | `check` |
| [NoopPermissionEngine.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/permission/NoopPermissionEngine.java) | 默认放行实现 | 为什么生产要替换 |
| [AuditService.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/api/AuditService.java) | 审计抽象 | `record` |
| [ConsoleAuditService.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/audit/ConsoleAuditService.java) | 默认控制台审计 | 当前只是样板 |
| [AuditEvent.java](../agent-core/src/main/java/com/sean/agenthub/agent/core/model/AuditEvent.java) | 审计字段模型 | `traceId`、`toolName`、`latencyMs`、`success` |

后续可观测性会围绕这里增强：

```text
agent_run
agent_step
tool_call_log
model_call_log
traceId
统一错误码
```

## 12. 智能附件业务样板入口

| 顺序 | 类 / 文档 | 作用 | 重点看 |
|---:|---|---|---|
| 1 | [agent-document-processing/README.md](../agent-document-processing/README.md) | 附件样板总说明 | 接口和运行命令 |
| 2 | [AttachmentAnalysisController.java](../agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/api/AttachmentAnalysisController.java) | 业务分析和大纲接口入口 | `analyze`、`analyzeFile`、`outlineFile` |
| 3 | [AttachmentUploadController.java](../agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/api/AttachmentUploadController.java) | 附件上传入口 | `upload` |
| 4 | [AttachmentAnalysisService.java](../agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/application/AttachmentAnalysisService.java) | 保存附件并触发 Agent 分析 | `analyzeFile`、`analyze` |
| 5 | [AttachmentAnalysisModelProvider.java](../agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/application/AttachmentAnalysisModelProvider.java) | 样板规则型模型决策 | 如何生成 5 个 ToolCall |
| 6 | [DocumentOutlineService.java](../agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/application/DocumentOutlineService.java) | PDF / Markdown 大纲提炼 | 本地提炼和 MiMo 提炼 |
| 7 | [AttachmentRepository.java](../agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/infrastructure/AttachmentRepository.java) | 内存附件仓库 | `save`、`get` |
| 8 | [AttachmentContentParserRegistry.java](../agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/infrastructure/parser/AttachmentContentParserRegistry.java) | parser 选择器 | `parse` |
| 9 | [AttachmentContentParser.java](../agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/infrastructure/parser/AttachmentContentParser.java) | 文件解析扩展点 | `supports`、`parse` |
| 10 | [docs/analyze-file-flow.md](../agent-document-processing/docs/analyze-file-flow.md) | analyze-file 流程说明 | 端到端链路 |

附件分析 Tool 顺序：

```text
parse_attachment
classify_document
extract_document_fields
check_document_rules
summarize_attachment_analysis
```

对应代码：

```text
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/tool/ParseAttachmentTool.java
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/tool/ClassifyDocumentTool.java
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/tool/ExtractDocumentFieldsTool.java
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/tool/CheckDocumentRulesTool.java
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/tool/SummarizeAttachmentAnalysisTool.java
```

## 13. 测试入口

| 测试类 | 作用 |
|---|---|
| [DefaultAgentRuntimeTest.java](../agent-core/src/test/java/com/sean/agenthub/agent/core/runtime/DefaultAgentRuntimeTest.java) | 理解 Runtime 行为最重要的单元测试 |
| [InMemoryToolRegistryTest.java](../agent-core/src/test/java/com/sean/agenthub/agent/core/tool/InMemoryToolRegistryTest.java) | Tool 注册中心行为 |
| [OpenAiCompatibleModelProviderTest.java](../agent-model-provider-http/src/test/java/com/sean/agenthub/agent/provider/http/OpenAiCompatibleModelProviderTest.java) | OpenAI-compatible provider 协议映射 |
| [AnthropicCompatibleModelProviderTest.java](../agent-model-provider-http/src/test/java/com/sean/agenthub/agent/provider/http/AnthropicCompatibleModelProviderTest.java) | Anthropic-compatible provider 协议映射 |
| [AgentAutoConfigurationTest.java](../agent-spring-boot-starter/src/test/java/com/sean/agenthub/agent/starter/AgentAutoConfigurationTest.java) | Starter 自动装配 |
| [AttachmentAnalysisApplicationTest.java](../agent-document-processing/src/test/java/com/sean/agenthub/agent/attachment/AttachmentAnalysisApplicationTest.java) | 附件业务样板端到端接口测试 |

建议先跑：

```bash
mvn -pl agent-document-processing -am test
```

然后再看：

```bash
mvn test
```

## 14. 相关文档入口

| 文档 | 用途 |
|---|---|
| [AgentHub_学习与落地计划.md](AgentHub_学习与落地计划.md) | 总入口和后续路线 |
| [agent-platform-progress.md](agent-platform-progress.md) | 当前真实进度 |
| [agent-platform-design.md](agent-platform-design.md) | 总体设计和边界 |
| [agent-platform-next-plan.md](agent-platform-next-plan.md) | 下一步计划 |
| [agenthub-mvp-acceptance.md](agenthub-mvp-acceptance.md) | MVP 验收记录 |
| [agenthub-starter-integration.md](agenthub-starter-integration.md) | Starter 接入说明 |
| [agenthub-attachment-analysis-acceptance.md](agenthub-attachment-analysis-acceptance.md) | 附件样板验收记录 |
| [plans/AgentHub_第一周日计划.md](plans/AgentHub_第一周日计划.md) | 第一周日计划 |

## 15. 第一周最低掌握标准

读完第一周，不要求理解所有实现细节，但至少要能回答：

```text
1. /agent/chat 请求进来后经过哪些类？
2. AgentRuntime 在什么时候调用模型？
3. ToolCall 是谁返回的？
4. Tool 真正执行前在哪里做权限校验？
5. Tool 执行结果在哪里记录审计？
6. agent-document-processing 为什么是业务样板，不是 agent-core 的一部分？
7. analyze-file 和 outline-file 的差异是什么？
8. 后续做可观测性时，应该从哪些接口和模型入手？
```
