# Agent 能力平台设计草案

## 1. 背景

当前希望建设一套可复用的 Agent 能力层，用于让业务系统具备 AI 调度、工具调用、数据查询、结果生成等能力。

目标不是先做一个固定业务 Agent，而是抽象出一套可扩展架构，使后续根据调研结果，可以快速适配不同场景。

MCP 的核心能力包括 Tools、Resources、Prompts，可作为后续对外暴露能力的重要协议参考。官方 MCP 规范中也明确将 Tools 定义为 AI 可执行函数，Resources 定义为上下文和数据来源。

## 2. 总体目标

建设一套可嵌入业务系统的 Agent 业务抽象层，并支持后续演进到平台化接入。

AgentHub 的核心定位不是替代 Spring AI、LangChain4j、Hutool AI 或 MCP SDK，而是稳定承接业务系统侧的 Agent 能力边界：

```text
Agent 统一入口
业务 Tool 抽象
Tool Schema 和风险等级
用户上下文
权限校验
审计记录
会话记忆
模型供应商适配入口
```

底层 AI 能力可以通过独立 adapter 接入：

```text
HTTP OpenAI-compatible / Anthropic-compatible
Spring AI
LangChain4j
Hutool AI
MCP Java SDK / Spring AI MCP
```

核心原则：

```text
agent-core 不依赖具体 AI 框架
agent-core 不依赖 MCP SDK
agent-core 不依赖 Spring、数据库或具体模型厂商 SDK
Spring AI / LangChain4j / Hutool AI 只能作为 ModelProvider 或 MCP adapter 的实现层
MCP 保留为生态互通协议方向，不作为第一版完整自研协议栈
```

建设方式上保留两种接入形态：

```text
1. SDK 集成方式
2. Gateway 网关接入方式
```

两种方式共用同一套核心抽象：

```text
Agent Runtime
Tool Registry
Model Provider
Model Provider Capability
Memory
Permission
Audit Log
MCP Adapter
```

推荐分层：

```text
业务系统
  -> AgentHub Starter / AgentHub API
    -> AgentService / AgentRuntime
      -> AgentTool / PermissionEngine / AuditService / AgentMemory
        -> ModelProvider / MCP Adapter
          -> HTTP 直连 / Spring AI / LangChain4j / Hutool AI / MCP SDK
```

## 3. 推荐项目结构

```text
agent-platform
├─ agent-core
│  ├─ AgentRuntime
│  ├─ AgentService
│  ├─ AgentTool
│  ├─ ToolRegistry
│  ├─ ModelProvider
│  ├─ AgentMemory
│  ├─ PermissionEngine
│  ├─ AuditService
│  └─ AgentContext
│
├─ agent-test-support
│  ├─ ModelProviderContract adapter 契约断言
│  └─ RecordingModelStreamListener 流式测试监听器
│
├─ agent-spring-boot-starter
│  ├─ 自动配置
│  ├─ 自动扫描 Tool Bean
│  ├─ 提供 /agent/chat 接口
│  ├─ 提供 /agent/chat/stream 接口
│  └─ 适配 Spring Boot 2.x / JDK 8
│
├─ agent-model-provider-http
│  ├─ OpenAI-compatible 适配
│  ├─ Anthropic-compatible 适配
│  └─ 当前保持单一 http package，后续类明显增多后再整体拆 openai / anthropic / client / codec
│
├─ agent-model-provider-spring-ai
│  └─ 后续 Spike，用 Spring AI 实现 ModelProvider
│
├─ agent-model-provider-langchain4j
│  └─ 后续 Spike，用 LangChain4j 实现 ModelProvider
│
├─ agent-model-provider-hutool-ai
│  └─ 后续 Spike，用 Hutool AI 实现 ModelProvider
│
├─ agent-gateway-server
│  ├─ 三方应用注册 Tool
│  ├─ 统一鉴权
│  ├─ 统一限流
│  ├─ 统一审计
│  ├─ Tool Schema 管理
│  └─ 对外暴露 Agent API / MCP API
│
├─ agent-mcp-adapter
│  ├─ AgentTool -> MCP Tool
│  ├─ ToolRegistry -> tools/list
│  ├─ PermissionEngine + AgentTool -> tools/call
│  └─ 后续优先评估 MCP Java SDK / Spring AI MCP
│
└─ agent-admin-ui
   ├─ Tool 管理
   ├─ 调用日志
   ├─ 权限配置
   ├─ 模型配置
   └─ 会话记录
```

## 4. 两种接入方式

### 4.1 SDK 集成方式

适合单个业务系统内部增强。

```text
业务系统
  ↓ 引入 agent-spring-boot-starter
自动获得 Agent 能力
  ↓
本系统内部注册 Tool
  ↓
提供 /agent/chat 接口
```

适用场景：

```text
预算系统 AI 助手
档案系统智能问答
表单自动填写
报表解释
业务数据查询
```

优点：

```text
落地快
权限容易复用
业务上下文获取方便
对老系统友好
```

缺点：

```text
多个系统各自接入
统一审计和统一模型出口较弱
```

### 4.2 Gateway 接入方式

适合平台化接入多个系统。

```text
业务系统 A
业务系统 B
业务系统 C
     ↓ 注册 Tool
Agent Gateway
     ↓ 暴露 Tool Schema / MCP
Agent / LLM 调度
```

适用场景：

```text
多个系统统一 AI 出口
统一模型调用
统一审计
统一权限
统一 Tool 管理
统一 MCP 暴露
```

优点：

```text
平台化
统一安全
统一审计
统一限流
统一模型管理
```

缺点：

```text
初期设计复杂度更高
需要业务系统提供标准 API
需要设计权限映射
```

## 5. 核心抽象

### 5.1 AgentService

统一 Agent 入口。

```java
public interface AgentService {
    AgentResponse chat(AgentRequest request);

    void streamChat(AgentRequest request, AgentStreamListener listener);
}
```

### 5.2 AgentRuntime

负责一次 Agent 执行流程。

职责：

```text
接收用户输入
构建上下文
调用模型
判断是否需要 Tool
执行 Tool
汇总结果
返回最终答案
记录审计日志
```

### 5.3 AgentTool

业务能力插件。

```java
public interface AgentTool {

    String name();

    String description();

    ToolSchema schema();

    ToolRiskLevel riskLevel();

    ToolResult execute(ToolContext context);
}
```

示例 Tool：

```text
query_budget_balance
query_file_metadata
query_dict_item
query_kafka_topic
query_kingbase_data
```

### 5.4 ToolRegistry

工具注册中心。

```java
public interface ToolRegistry {

    List<AgentTool> listTools();

    Optional<AgentTool> getTool(String name);

    void register(AgentTool tool);
}
```

### 5.5 ModelProvider

模型供应商抽象。

```java
public interface ModelProvider {
    Set<ModelProviderCapability> capabilities();

    ModelResponse chat(ModelRequest request);

    void streamChat(ModelRequest request, ModelStreamListener listener);
}
```

当前 MVP 已实现非流式 `chat`、基础文本流式 `streamChat`，以及基础流式 ToolCall 解析和流式 Tool 总结链路。后续重点不是重新实现流式 ToolCall，而是补强供应商协议边界、错误恢复和真实模型兼容性。

`ModelProviderCapability` 用于横向比较 HTTP 直连、Spring AI、LangChain4j、Hutool AI 等 adapter 的覆盖范围，避免后续 adapter 接入时只靠文档描述能力。

可适配：

```text
OpenAI-compatible：当前默认优先 DeepSeek / DS；Mimo、本地 Qwen、Gemma4、mlx、公司内网模型网关后续再评估
Anthropic-compatible：Claude 风格 Messages / tool_use 网关
Spring AI：后续 Java 17 / Spring Boot 3 场景优先评估
LangChain4j：后续 RAG、复杂 Tool、AI Service 场景优先评估
Hutool AI：后续国内模型轻量接入场景评估
```

### 5.6 AgentMemory

会话记忆。

```java
public interface AgentMemory {

    List<AgentMessage> load(String sessionId);

    void save(String sessionId, AgentMessage message);

    void clear(String sessionId);
}
```

可实现：

```text
内存
Redis
MySQL
PostgreSQL
向量库
```

### 5.7 PermissionEngine

权限控制。

```java
public interface PermissionEngine {

    boolean canCallTool(UserContext user, AgentTool tool, ToolContext context);

    boolean requireApproval(AgentTool tool, ToolContext context);
}
```

权限控制重点：

```text
用户是否可访问该 Tool
用户是否可访问该数据范围
Tool 是否为高风险操作
是否需要人工确认
```

### 5.8 AuditService

审计日志。

记录内容：

```text
用户
会话 ID
输入内容
调用模型
调用 Tool
Tool 参数
Tool 返回结果摘要
Token 消耗
执行耗时
是否成功
错误信息
审批记录
```

## 6. Tool 风险等级

建议第一版只支持只读 Tool。

```java
public enum ToolRiskLevel {
    READ,
    WRITE,
    DANGEROUS
}
```

### READ

```text
查询数据
读取文件元数据
获取字典
查询状态
```

### WRITE

```text
新增数据
修改数据
发送消息
触发流程
```

### DANGEROUS

```text
删除数据
审批通过
资金操作
执行脚本
批量变更
```

第一版建议：

```text
只实现 READ
WRITE / DANGEROUS 仅保留设计
```

## 7. Gateway Tool 注册模型

业务系统向 Gateway 注册 Tool。

### 7.1 注册信息

```json
{
  "name": "query_budget_balance",
  "description": "查询某单位某年度预算余额",
  "method": "POST",
  "url": "https://budget-system/api/ai/query-budget-balance",
  "parameters": {
    "type": "object",
    "properties": {
      "unitCode": {
        "type": "string",
        "description": "单位编码"
      },
      "year": {
        "type": "integer",
        "description": "年度"
      }
    },
    "required": ["unitCode", "year"]
  },
  "risk": "READ"
}
```

### 7.2 调用流程

```text
Agent 判断需要调用 Tool
  ↓
Gateway 根据 Tool 名称找到真实业务接口
  ↓
校验权限
  ↓
调用业务系统 API
  ↓
记录审计日志
  ↓
返回结果给 Agent
```

## 8. MCP 适配设计

MCP 是 Agent / LLM 与外部工具、资源、提示词交互的开放协议，不是 Spring AI 或 LangChain4j 的私有实现。

后续 Gateway 可以对外暴露 MCP Server 能力，但第一版不自研完整 MCP Server / Client。MCP 当前只保留为 AgentHub Tool 抽象的生态互通方向。

实现策略：

```text
短期：保持 AgentTool / ToolSchema / PermissionEngine / AuditService 稳定
中期：设计 agent-mcp-adapter，把 AgentHub Tool 映射到 MCP Tool
后续：优先评估 MCP Java SDK / Spring AI MCP 作为协议实现层
避免：在 agent-core 内直接依赖 MCP SDK 或自研完整 MCP 协议栈
```

MCP 可参考的核心能力：

```text
tools/list
tools/call
resources/list
resources/read
prompts/list
prompts/get
```

映射关系：

```text
AgentTool       -> MCP Tool
业务数据只读资源 -> MCP Resource
业务提示词模板   -> MCP Prompt
ToolRegistry    -> tools/list
AgentTool.execute + PermissionEngine + AuditService -> tools/call
```

需要注意 MCP 安全风险，尤其是：

```text
工具权限声明不可完全信任
Tool 调用必须服务端鉴权
高风险操作必须人工确认
STDIO / 本地命令执行要谨慎
Prompt Injection 需要隔离处理
```

## 9. 第一版 MVP 范围

### 9.1 必须实现

```text
agent-core
agent-model-provider-http
agent-spring-boot-starter
基础 AgentService
基础 ModelProvider
基础 ToolRegistry
基础 AgentTool
基础审计日志
只读 Tool 调用
非流式 chat
SSE 流式 chat
```

模型协议优先级：

```text
P0 OpenAI-compatible Chat Completions
P0 Anthropic-compatible Messages
P1 Function Calling 边界增强（基础多 ToolCall、Tool 结果消息、流式 ToolCall 已完成）
P1 JSON Schema Structured Output 基础支持（OpenAI-compatible 透传已完成）
P2 MCP adapter 设计或 PoC
P2 Spring AI / LangChain4j / Hutool AI adapter Spike（MVP 验收后再进入）
```

MVP 阶段协议层优先兼容 OpenAI-compatible 和 Anthropic-compatible 已足够。当前真实模型默认优先 DeepSeek / DS，先用 DS 打稳 AgentRuntime、ToolCall、流式输出和错误恢复；其他模型后续再说。

后续不建议继续扩大自研模型协议面，而应通过独立 adapter 评估主流 AI 框架：

```text
Spring AI：适合 Java 17 / Spring Boot 3 体系，且已提供 MCP 支持
LangChain4j：适合 RAG、AI Service、复杂 Tool 和链路编排能力验证
Hutool AI：适合国内模型轻量快捷调用场景验证
MCP Java SDK / Spring AI MCP：适合后续 MCP Server / Client 实现
```

MCP 更偏 Tool 生态互联，JSON Schema Structured Output 更偏输出强约束，二者都不应阻塞第一版 Agent SDK 的嵌入式闭环。

### 9.2 示例 Tool

```text
query_dict_item
query_file_metadata
query_user_info_mock
```

### 9.3 示例接口

```http
POST /agent/chat
POST /agent/chat/stream
```

请求：

```json
{
  "sessionId": "s001",
  "userId": "u001",
  "message": "帮我查询 2026 年 A 单位预算余额"
}
```

响应：

```json
{
  "ok": true,
  "answer": "A 单位 2026 年预算余额为 xxx 元。",
  "toolCalls": [
    {
      "tool": "query_budget_balance",
      "success": true
    }
  ]
}
```

## 10. 第二版能力

```text
Gateway Server
Tool 动态注册
MCP Adapter
Redis Memory
权限配置
调用日志后台
多模型切换
Token 统计
```

## 11. 第三版能力

```text
Workflow 编排
多 Agent 协作
RAG 知识库
向量检索
人工审批
高风险 Tool 控制
Tool 调用回放
Prompt 模板管理
```

## 12. JDK 版本建议

### SDK / Starter

建议兼容：

```text
JDK 8
Spring Boot 2.3.x
```

原因：

```text
方便接入老业务系统
降低落地成本
兼容现有企业 Java 项目
```

### Gateway Server

新项目建议：

```text
JDK 17
Spring Boot 3.x
```

原因：

```text
生态更新
性能更好
维护周期更长
适合作为平台服务
```

### 推荐策略

```text
agent-core 尽量 JDK 8 兼容
agent-spring-boot-starter 支持 Spring Boot 2.x
agent-gateway-server 可使用 JDK 17
```

### Starter 拆分决策

MVP 阶段暂不拆分 Java 8 Starter 和新版 JDK Starter。

当前保持：

```text
agent-core                    JDK 8 兼容，纯核心抽象
agent-spring-boot-starter     JDK 8 / Spring Boot 2.3.x 接入层
agent-example-spring-boot2    Spring Boot 2 示例应用
```

暂不新增：

```text
agent-spring-boot-common
agent-spring-boot2-starter
agent-spring-boot3-starter
```

原因：

```text
当前 starter 代码量很小，拆 common 会增加模块复杂度
当前还没有真实 Spring Boot 3 业务系统接入需求
Spring Boot 2 和 3 的差异点尚未在代码中出现
MVP 优先验证 Agent Runtime、Tool、ModelProvider、Permission、Audit 的闭环
```

后续如果出现以下条件，再新增 Spring Boot 3 Starter：

```text
有真实 Spring Boot 3 / JDK 17+ 业务系统接入
需要 jakarta.servlet 生态
需要 Spring Boot 3 AutoConfiguration.imports 注册方式
需要使用 JDK 17 专属能力
Boot 2 / Boot 3 starter 代码差异超过 20%-30%
```

未来可演进为：

```text
agent-spring-boot-common      Starter 公共装配逻辑
agent-spring-boot2-starter    JDK 8 / Spring Boot 2.x
agent-spring-boot3-starter    JDK 17+ / Spring Boot 3.x
```

## 13. 关键设计原则

```text
Agent Core 不依赖 Spring
Tool 是插件
ModelProvider 可切换
Memory 可替换
Permission 必须内置
Audit 必须内置
Gateway 和 SDK 共用核心抽象
第一版只做只读能力
不要一开始做大而全平台
```

配置文件约定：

```text
Spring Boot 示例和接入文档优先使用 application.yml
后续新增配置示例优先使用 YAML
除非目标业务系统已有明确约束，否则不默认使用 application.properties
```

项目命名空间约定：

```text
Maven groupId 使用 com.sean.agenthub
Java package 使用 com.sean.agenthub.agent.*
后续新增模块和示例代码应保持该命名空间
```

agent-core 包结构约定：

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
com.sean.agenthub.agent.provider.http    HTTP 标准模型协议适配
```

后续新增类应先判断职责边界，不建议继续直接放在 `core` 根包下。

agent-model-provider-http 包结构约定：

```text
当前不拆子 package，也不采用目录分层和 package 不一致的结构。
Java 源码目录应与 package 保持一致。
HttpJsonClient、ModelProviderJsonSupport、ModelProviderJsonFields 属于包内实现细节，保持 package-private，避免扩大公开 API。
当前对外公开 OpenAiCompatibleModelProvider、AnthropicCompatibleModelProvider、HttpModelProviderProperties。
后续类明显增多后，再整体拆为 http.openai、http.anthropic、http.client、http.codec 等 package，并同步设计公开 API 边界。
```

常量设计约定：

```text
不使用 interface 作为常量容器，接口只表达能力契约。
协议 JSON 字段和值可用 package-private final class 承载。
常见字段名不等于公开 API，只有外部有稳定复用场景时才 public。
后续如需跨模块复用，再设计 JsonSchemaFields、OpenAiProtocolFields、McpProtocolFields 等明确公开类型。
```

## 14. 待调研问题

```text
目标业务系统有哪些？
哪些能力适合作为 Tool？
是否需要写操作？
是否有统一用户中心？
是否有统一权限体系？
是否允许 Gateway 访问业务数据库？
业务系统是否愿意提供 AI 专用 API？
是否需要 MCP 对外暴露？
是否需要支持 Claude / Codex / 自研 Agent？
是否有国产模型或内网模型要求？
```

## 15. 给 Codex / Claude 的评估任务

请基于本文档评估以下内容：

```text
1. 当前分层是否合理？
2. agent-core 应该如何设计接口？
3. JDK 8 + Spring Boot 2.3 是否可行？
4. Gateway 和 Starter 是否应该共用同一个 core？
5. Tool Schema 应该如何定义？
6. 如何兼容 MCP tools/list 和 tools/call？
7. 第一版 MVP 应该包含哪些模块？
8. 哪些地方存在过度设计？
9. 哪些安全风险必须提前处理？
10. 请给出推荐的 Maven 多模块结构和核心接口代码。
```

## 16. 可行性评估结论

整体方案可行，适合作为企业业务系统的 Agent 能力层基础架构。

当前分层方向合理：

```text
agent-core                    纯核心抽象和默认实现
agent-model-provider-http     MVP 轻量模型协议适配
agent-spring-boot-starter     业务系统快速接入
agent-model-provider-spring-ai 后续 Spring AI 适配 Spike
agent-model-provider-langchain4j 后续 LangChain4j 适配 Spike
agent-model-provider-hutool-ai 后续 Hutool AI 适配 Spike
agent-gateway-server          平台化统一接入
agent-mcp-adapter             MCP 协议适配，优先评估 MCP Java SDK / Spring AI MCP
agent-admin-ui                运维和治理后台
```

但第一版不建议同时建设完整平台。推荐先落地 SDK 集成方式，验证 Agent Runtime、Tool 调用、模型适配、权限和审计闭环，再进入 Gateway 和 MCP 阶段。

第一阶段推荐定位：

```text
可嵌入业务系统的只读 Agent SDK
```

第一阶段不推荐定位：

```text
多系统统一 AI 平台
完整 MCP Server
完整自研模型协议框架
完整替代 Spring AI / LangChain4j 的 AI 框架
可视化运维后台
复杂 Workflow / 多 Agent 编排平台
```

## 17. 第一版 MVP 收敛建议

第一版建议只包含以下模块：

```text
agent-platform-parent
├─ agent-core
├─ agent-model-provider-http
├─ agent-spring-boot-starter
└─ agent-example-spring-boot2
```

### 17.1 第一版必须实现

```text
AgentService.chat
AgentRuntime
AgentTool
ToolRegistry
ToolSchema
ModelProvider
AgentMemory
PermissionEngine
AuditService
Spring Boot 自动配置
自动扫描 AgentTool Bean
POST /agent/chat
POST /agent/chat/stream
3 个只读示例 Tool
```

### 17.2 第一版暂不实现

```text
Gateway Server
MCP Server
Admin UI
完整供应商协议边界覆盖
Redis Memory
动态 Tool 注册
写操作 Tool
人工审批
RAG
Workflow
多 Agent 协作
```

### 17.3 第一版验收标准

```text
业务系统引入 starter 后可自动获得 /agent/chat
业务系统可通过 Spring Bean 注册只读 Tool
Agent 可根据用户问题选择 Tool
Tool 参数可按 Schema 校验
Tool 调用前经过 PermissionEngine
Tool 调用和模型调用均记录 AuditEvent
一次请求可完成：用户输入 -> 模型判断 -> Tool 执行 -> 模型总结 -> 返回答案
```

## 18. agent-core 接口细化建议

agent-core 应保持 JDK 8 兼容，不依赖 Spring、Servlet、数据库和具体 HTTP 客户端。

推荐核心接口：

```java
public interface AgentService {
    AgentResponse chat(AgentRequest request);
}
```

```java
public interface AgentRuntime {
    AgentResponse run(AgentRequest request, AgentContext context);
}
```

```java
public interface AgentTool {
    String name();

    String description();

    ToolSchema schema();

    ToolRiskLevel riskLevel();

    ToolResult execute(ToolContext context);
}
```

```java
public interface ToolRegistry {
    List<AgentTool> list();

    Optional<AgentTool> get(String name);

    void register(AgentTool tool);
}
```

```java
public interface ModelProvider {
    Set<ModelProviderCapability> capabilities();

    ModelResponse chat(ModelRequest request);

    void streamChat(ModelRequest request, ModelStreamListener listener);
}
```

```java
public interface AgentMemory {
    List<AgentMessage> load(String sessionId);

    void save(String sessionId, AgentMessage message);

    void clear(String sessionId);
}
```

```java
public interface PermissionEngine {
    PermissionResult check(UserContext user, AgentTool tool, ToolContext context);
}
```

```java
public interface AuditService {
    void record(AuditEvent event);
}
```

ModelResponse 需要明确表达两类结果：

```text
普通文本回答
模型请求调用 Tool
```

否则 AgentRuntime 无法稳定驱动：

```text
模型 -> Tool -> 模型总结
```

## 19. Tool Schema 定义建议

Tool Schema 建议采用 JSON Schema 子集，避免自定义复杂协议。

第一版支持：

```text
type
properties
required
description
enum
items
```

第一版类型范围：

```text
string
integer
number
boolean
object
array
```

示例：

```json
{
  "type": "object",
  "properties": {
    "unitCode": {
      "type": "string",
      "description": "单位编码"
    },
    "year": {
      "type": "integer",
      "description": "年度"
    }
  },
  "required": ["unitCode", "year"]
}
```

这样后续可以较自然地适配：

```text
OpenAI / Claude / Qwen function calling
MCP tools/list
MCP tools/call
Gateway Tool 注册
```

## 20. MCP 兼容策略

第一版不直接实现 MCP Server，但内部模型应预留 MCP 适配空间。

推荐映射：

```text
AgentTool.name        -> MCP Tool name
AgentTool.description -> MCP Tool description
AgentTool.schema      -> MCP Tool inputSchema
AgentTool.execute     -> MCP tools/call
```

需要注意：

```text
MCP 只作为协议适配层，不应绕过 PermissionEngine
MCP tools/call 必须记录 AuditEvent
MCP 暴露的 Tool 应支持白名单配置
Tool 返回内容必须和系统提示词隔离，避免 Prompt Injection
不建议第一版暴露 STDIO / 本地命令执行类能力
```

## 21. 安全风险前置处理

即使第一版只实现只读 Tool，也必须提前处理以下风险。

### 21.1 权限风险

Tool 调用前必须经过 PermissionEngine。

权限校验至少包含：

```text
用户是否可使用该 Tool
用户是否可访问该业务数据范围
Tool 风险等级是否允许
请求参数是否符合 Schema
```

### 21.2 审计风险

AuditEvent 建议记录：

```text
traceId
sessionId
userId
modelName
requestSummary
toolName
toolArgumentsMasked
toolResultSummary
latencyMs
success
errorCode
errorMessage
createdAt
```

Tool 返回结果不建议全量入库，应优先记录摘要和脱敏后的参数。

### 21.3 Prompt Injection 风险

AgentRuntime 需要区分：

```text
系统指令
用户输入
模型输出
Tool 参数
Tool 返回数据
```

Tool 返回数据只能作为业务数据上下文，不能被当作系统指令执行。

### 21.4 写操作风险

第一版只支持 READ。

WRITE 和 DANGEROUS 仅保留枚举和接口扩展点，不开放实际执行能力。

## 22. 推荐 Maven 多模块结构

第一版推荐：

```text
agent-platform
├─ pom.xml
├─ agent-core
│  ├─ pom.xml
│  └─ src/main/java
├─ agent-test-support
│  ├─ pom.xml
│  └─ src/main/java
├─ agent-model-provider-http
│  ├─ pom.xml
│  └─ src/main/java
├─ agent-spring-boot-starter
│  ├─ pom.xml
│  └─ src/main/java
└─ agent-example-spring-boot2
   ├─ pom.xml
   └─ src/main/java
```

第二版再增加：

```text
agent-model-provider-spring-ai
agent-model-provider-langchain4j
agent-model-provider-hutool-ai
agent-gateway-server
agent-mcp-adapter
```

第三版再增加：

```text
agent-admin-ui
agent-rag
agent-workflow
```

依赖方向：

```text
agent-core
  ↑
agent-model-provider-http
  ↑
agent-spring-boot-starter
  ↑
agent-example-spring-boot2

agent-core
  ↑
agent-gateway-server

agent-core
  ↑
agent-mcp-adapter
```

说明：

```text
agent-gateway-server 和 agent-mcp-adapter 都应依赖 agent-core。
agent-mcp-adapter 不默认依赖 agent-gateway-server，避免 MCP 协议适配被 Gateway 服务器实现绑定。
Gateway 后续可以组合 agent-mcp-adapter 对外暴露 MCP API。
```

禁止依赖方向：

```text
agent-core -> Spring
agent-core -> Gateway
agent-core -> MCP SDK
agent-core -> Spring AI
agent-core -> LangChain4j
agent-core -> Hutool AI
agent-core -> 数据库实现
agent-core -> 具体模型厂商 SDK
```

## 23. 推荐推进顺序

当前已完成第一版核心闭环、DeepSeek / DS 真实链路收敛、Tool 选择约束、错误恢复边界测试和 Structured Output 基础透传。2026-06-04 起默认模型方向继续收敛为 DeepSeek / DS，暂停新增模型适配。

```text
1. [done] 建 Maven 多模块骨架
2. [done] 实现 agent-core 接口和内存默认实现
3. [done] 实现 ModelProvider 的 mock、OpenAI-compatible 和 Anthropic-compatible 适配
4. [done] 实现 AgentRuntime 的非流式单轮 Tool 调用
5. [done] 实现 Spring Boot Starter 自动配置
6. [done] 实现 /agent/chat 示例接口
7. [done] 增加 3 个只读示例 Tool
8. [done] 增加权限、审计、参数校验和异常处理
9. [done] 编写 example 项目和接入说明
10. [done] 完成多 ToolCall、Tool 结果消息、基础文本流式输出和基础流式 ToolCall
11. [done] 以 DeepSeek / DS 作为默认模型完成真实链路收敛
12. [done] 补强 DS Tool 选择约束和错误恢复
13. [done] 完成 JSON Schema Structured Output 基础透传和能力声明
14. [done] 收敛设计文档、进度文档和下一步计划，形成一致开发基线
15. [next] 整理 MVP 验收清单和对外接入说明
16. [next] 选择一个真实业务只读 Tool 做试点，验证权限、审计和数据边界
17. [later] 评估 MCP Java SDK / Spring AI MCP，设计 agent-mcp-adapter 最小 PoC
18. [later] 其他模型、Spring AI / LangChain4j / Hutool AI adapter Spike
19. [later] 验收后再进入 Gateway / Admin UI / MCP 完整能力阶段
```

下一轮具体计划以 `docs/agent-platform-next-plan.md` 为准。
