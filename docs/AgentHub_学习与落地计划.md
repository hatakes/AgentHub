# AgentHub 学习与落地总入口

> 当前项目主要由 Codex 持续推进，本文档用于帮助项目负责人快速理解现状、找到入口、判断下一步优先级。不要把本文当成从 0 开始的路线图；AgentHub 已经完成第一版核心闭环。

## 1. 一句话定位

AgentHub 是一个面向企业内部业务场景的 Agent 能力底座，重点不是做聊天机器人，而是沉淀：

```text
模型适配 + Tool 调用 + 权限审计 + Memory + Runtime 编排 + 业务样板
```

当前更准确的定位是：

```text
AgentHub = 业务 Agent 抽象层 + Tool Hub + ModelProvider Adapter + Starter 接入层
```

后续可以继续补强 RAG、Workflow、可观测性和管理界面，但这些不应推翻当前已有的 `ModelProvider`、`AgentRuntime`、`AgentTool`、`ToolRegistry` 抽象。

## 2. 快速上手入口

如果你只想快速了解项目，按这个顺序看：

```text
1. README.md
   了解项目模块、运行方式和当前能力。

2. docs/agent-platform-progress.md
   看项目真实进度，判断哪些已经完成，哪些只是后续计划。

3. docs/agent-platform-design.md
   看整体架构、核心抽象、模块边界和后续方向。

4. [docs/AgentHub_核心代码导航.md](AgentHub_核心代码导航.md)
   按主链路跳转核心类、测试和相关文档。

5. agent-core/README.md
   看 AgentHub 核心：AgentRuntime、ModelProvider、AgentTool、ToolRegistry、Memory、权限、审计。

6. agent-spring-boot-starter/README.md
   看业务系统如何通过 Starter 接入 AgentHub。

7. agent-document-processing/README.md
   看当前最完整的业务样板：附件上传、解析、分类、规则校验、大纲提炼。

8. docs/agenthub-attachment-analysis-acceptance.md
   看业务样板的验收口径和已覆盖测试。
```

推荐先运行这两个命令：

```bash
mvn test
```

```bash
mvn -pl agent-document-processing -am test
```

本地启动附件处理样板：

```bash
mvn -pl agent-document-processing spring-boot:run
```

上传并分析文本附件：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@id-card.txt;type=text/plain' \
  http://127.0.0.1:8080/attachment-analysis/analyze-file
```

上传 PDF 或 Markdown 并提炼大纲：

```bash
curl -sS -F 'userId=attachment-reviewer' \
  -F 'file=@/Users/sean/Desktop/policy.md;type=text/markdown' \
  http://127.0.0.1:8080/attachment-analysis/outline-file
```

## 3. 当前项目状态

截至 2026-06-15，项目已完成：

```text
Maven 多模块骨架
agent-core 核心抽象和默认 Runtime
agent-spring-boot-starter 自动配置和 HTTP 入口
agent-example-spring-boot2 示例业务应用
agent-model-provider-http OpenAI / Anthropic 兼容协议适配
DeepSeek / MiMo / SiliconFlow 示例 profile
非流式 chat
流式 chat
ToolCall / 多 ToolCall / 流式 ToolCall 基础链路
Tool 权限校验
Tool 审计
InMemory Memory
Structured Output 基础透传
ModelProviderCapability 能力声明
agent-mcp-adapter 最小 MCP Tool 映射
Spring AI / LangChain4j Java 17 adapter Spike
agent-document-processing 智能附件分析业务样板
PDF / Markdown 大纲提炼入口
MiMo profile 图片解析和文档大纲提炼配置
业务接入验收文档和测试覆盖
```

当前不要重复建设这些抽象：

```text
不要新增 ChatModelClient 替代 ModelProvider
不要新增 ToolExecutor 替代 AgentRuntime + AgentTool
不要把 OCR、附件解析、身份证规则下沉到 agent-core
不要让 Spring AI / LangChain4j 成为 core 强依赖
不要为了 Workflow 重写已有 Runtime
```

## 4. 模块地图

```text
agent-core
  核心抽象和默认运行时。
  重点看：api、runtime、tool、memory、model、capability。

agent-model-provider-http
  OpenAI-compatible / Anthropic-compatible HTTP 模型适配。
  重点看：OpenAiCompatibleModelProvider、AnthropicCompatibleModelProvider。

agent-spring-boot-starter
  Spring Boot 2 业务系统接入层。
  重点看：AgentAutoConfiguration、AgentChatController、AgentProperties。

agent-example-spring-boot2
  最小业务系统示例。
  适合理解业务 Tool、权限、审计如何接入。

agent-document-processing
  当前最重要的业务样板。
  覆盖附件上传、解析、分类、字段抽取、规则校验、摘要、大纲提炼。

agent-mcp-adapter
  AgentTool 到 MCP Tool 的最小映射。
  当前是生态互通 Spike，不是主链路。

agent-model-provider-spring-ai / agent-model-provider-langchain4j
  JDK 17+ adapter Spike。
  当前用于能力对比，不进入 Java 8 主 reactor。
```

## 5. 核心概念速查

```text
AgentService
  面向业务调用方的服务入口。

AgentRuntime
  一次 Agent 执行链路：模型调用、Tool 选择、权限、审计、Tool 执行、总结。

ModelProvider
  模型供应商抽象。不要新建 ChatModelClient。

AgentTool
  业务能力插件。所有业务动作优先注册成 Tool。

ToolRegistry
  Tool 注册中心。

PermissionEngine
  Tool 执行前的权限判断。

AuditService
  Tool 执行审计。

AgentMemory
  会话记忆抽象，当前默认内存实现。

ModelProviderCapability
  描述模型适配器已覆盖能力，例如 TEXT_CHAT、TEXT_STREAM、TOOL_CALL、STRUCTURED_OUTPUT。
```

## 6. 当前最重要的业务样板

`agent-document-processing` 是当前理解 AgentHub 最好的入口，因为它不是空 Demo，而是完整业务链路：

```text
上传附件
-> 解析文本 / 图片 / Markdown / PDF
-> 保存 AttachmentRecord
-> 触发 AgentRuntime
-> 模型选择 ToolCall
-> 执行 parse_attachment
-> 执行 classify_document
-> 执行 extract_document_fields
-> 执行 check_document_rules
-> 执行 summarize_attachment_analysis
-> 返回结构化 analysis + answer
```

另有独立大纲提炼入口：

```text
POST /attachment-analysis/outline-file
```

该入口面向 PDF / Markdown，不进入身份证或材料审核 Tool 链路。

## 7. 后续学习路线

当前不建议按“从 0 到 1”学习，而是按“读懂现有闭环 -> 做二阶段增强”推进。

### 第 1 阶段：读懂现有闭环

目标：能说清楚一次请求如何从 HTTP 进入 AgentHub，并完成 Tool 调用。

重点文件：

```text
docs/AgentHub_核心代码导航.md
agent-spring-boot-starter/src/main/java/com/sean/agenthub/agent/starter/AgentChatController.java
agent-spring-boot-starter/src/main/java/com/sean/agenthub/agent/starter/AgentAutoConfiguration.java
agent-core/src/main/java/com/sean/agenthub/agent/core/runtime/DefaultAgentRuntime.java
agent-core/src/main/java/com/sean/agenthub/agent/core/api/ModelProvider.java
agent-core/src/main/java/com/sean/agenthub/agent/core/api/AgentTool.java
```

验收标准：

```text
能画出 /agent/chat 的执行链路
能说明 Tool 权限和审计在哪里发生
能说明业务 Tool 为什么不放在 agent-core
```

### 第 2 阶段：读懂业务样板

目标：能运行并解释智能附件分析。

重点文件：

```text
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/api
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/application
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/domain
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/tool
agent-document-processing/src/main/java/com/sean/agenthub/agent/attachment/infrastructure/parser
```

验收标准：

```text
能运行 /attachment-analysis/analyze-file
能运行 /attachment-analysis/outline-file
能说明 AttachmentContentParser 扩展点
能说明为什么请求和响应 DTO 放在 domain
```

### 第 3 阶段：补持久化和可观测性

目标：把内存审计和执行过程升级成可追踪的 Agent Run。

建议交付：

```text
agent_run
agent_step
tool_call_log
model_call_log
traceId
统一错误码
Tool 入参 / 出参脱敏快照
```

优先级高于复杂 Workflow，因为没有执行记录就很难排查真实业务问题。

### 第 4 阶段：RAG 最小闭环

目标：做出可演示的企业知识库问答，不追求一次到位。

第一版只建议做：

```text
Markdown / 文本型 PDF
简单 chunk
Embedding provider
向量检索或关键词检索二选一
RAG 问答接口
来源引用
```

暂缓：

```text
复杂 Rerank
多向量库适配
扫描件 PDF OCR
Excel 表格语义解析
大规模知识库调参
```

### 第 5 阶段：附件助手二期

目标：把当前 `agent-document-processing` 从样板升级成更强的业务演示。

建议交付：

```text
Word adapter
扫描件 PDF OCR adapter
附件分类归档
附件摘要和问答
Markdown / Excel 汇总导出
MiMo 真实图片解析验收记录
```

### 第 6 阶段：Workflow 和展示

目标：基于已有 Runtime 做有限 Workflow，不做大而全流程引擎。

建议先做：

```text
固定步骤业务流程
失败重试
人工确认节点
Agent Run 详情接口或页面
演示脚本
架构图
简历描述
```

## 8. 技术栈建议

当前已经确定：

```text
Java 8
Spring Boot 2.3.12.RELEASE
Maven 多模块
agent-core 不依赖具体 AI 框架
HTTP provider 优先支持 OpenAI-compatible / Anthropic-compatible
Spring AI / LangChain4j 只作为 JDK 17+ adapter Spike
```

后续持久化建议先保守选择：

```text
MySQL 或 PostgreSQL 二选一
先不同时适配 Kingbase
Redis 暂缓，等 Memory 或限流真的需要再加
向量库先选一个最容易本地跑通的方案
```

## 9. 不建议优先投入

```text
多 Agent 框架大乱炖
CrewAI / AutoGen API 细节
从零训练模型
微调大模型
复杂向量库调参
完整 Admin UI
把 Spring AI 或 LangChain4j 改成主框架
重新设计一套和现有 core 平行的 Agent 抽象
```

当前最重要的是：

```text
读懂现有 Runtime + 补可观测性 + 做 RAG 最小闭环 + 强化附件业务样板
```

## 10. 每周推进节奏

建议每周 5-10 小时：

```text
工作日 2 晚：读代码、跑测试、补小功能
周六半天：合并一个可验证改动
周日 1 小时：更新文档、记录踩坑、整理演示材料
```

每周至少产出：

```text
一次可运行代码改动
一次测试结果
一段文档更新
一个可演示接口或截图
```

## 11. 简历描述草稿

```text
设计并实现 AgentHub 企业级智能体应用底座，基于 Java 8 和 Spring Boot 2 构建可复用的 Agent Runtime、ModelProvider、ToolRegistry、权限审计和 Memory 抽象，支持 OpenAI / Anthropic compatible 模型协议、流式输出、多 ToolCall、Structured Output 基础透传和 Spring Boot Starter 接入。项目内置智能附件分析业务样板，覆盖附件上传、解析、文档分类、字段抽取、规则校验、审核意见生成以及 PDF / Markdown 大纲提炼，用于验证企业业务系统接入 Agent 能力的完整闭环。
```

后续补 RAG 和可观测性后，再把描述升级为：

```text
进一步实现 RAG 知识库检索、Agent Run 执行链路追踪、Tool 调用日志和业务附件助手演示场景，使 Agent 应用具备可追踪、可审计、可扩展的企业落地能力。
```
