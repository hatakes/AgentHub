# Agent 能力平台当前进度

## 1. 当前状态

当前处于第一版 MVP 核心闭环已完成、MVP 接入说明和验收清单已完成、示例业务只读 Tool 试点已完成、MCP Adapter 最小设计已完成、真实业务系统接入验收材料已完成、等待真实业务接入阶段。
2026-06-04 已完成 DeepSeek OpenAI-compatible 真实接口第一轮联调，并已完成 DS 默认模型配置固化、Tool 选择约束、错误恢复和基础验收文档。MiMo / SiliconFlow / 本地模型先暂停适配，后续再评估。
2026-06-04 已完成 P0 DS 默认模型配置固化、P0 Tool 选择约束增强、P1 错误恢复和边界测试、P2 JSON Schema Structured Output 基础支持。当前 61 个测试全部通过。
2026-06-04 已完成 P0 MVP 接入说明和验收清单，新增 `docs/agenthub-starter-integration.md` 和 `docs/agenthub-mvp-acceptance.md`。
2026-06-04 已完成 P1 示例业务只读 Tool 试点，新增 `query_budget_balance`、示例权限、示例审计和集成测试覆盖。
2026-06-04 已完成 P1 MCP Adapter 最小设计，新增 `agent-mcp-adapter` 模块，不依赖 MCP SDK 和 Gateway。
2026-06-04 已完成真实业务系统接入验收记录模板和二阶段方向选择文档。
2026-06-04 已完成同级独立最小业务接入样板 `../agent-business-minimal-demo`，用于验证外部业务服务通过 starter 接入。

已确认：

```text
整体架构方向可行
项目 Maven groupId 使用 com.sean.agenthub
Java package 使用 com.sean.agenthub.agent.*
优先落地 SDK 集成方式
第一版只做只读 Tool
agent-core 保持 JDK 8 兼容
agent-spring-boot-starter 适配 Spring Boot 2.3.x
MVP 阶段暂不拆分 Java 8 / 新 JDK 双 Starter
后续配置文件和文档示例优先使用 YAML
新增 Java 类型补充必要 JavaDoc 和 @author Sean，不强制添加日期
Gateway / MCP / Admin UI 延后到第二阶段以后
AgentHub 定位为业务 Agent 抽象层，不替代 Spring AI / LangChain4j / Hutool AI
agent-core 不依赖具体 AI 框架、MCP SDK、数据库或模型厂商 SDK
Spring AI / LangChain4j / Hutool AI 后续只通过独立 adapter 接入 ModelProvider
MCP 保留为生态互通协议方向，后续优先评估 MCP Java SDK / Spring AI MCP
```

当前仓库状态：

```text
已有设计文档：docs/agent-platform-design.md
已有 Java 工程代码
已有 Maven 多模块骨架
已有 agent-test-support 测试支撑模块
已有 agent-model-provider-http 协议适配模块
已有 agent-mcp-adapter 最小 MCP 映射模块
已有 agent-core 单元测试
已有 agent-model-provider-http mock HTTP 协议测试
已有 agent-mcp-adapter 单元测试
已有 agent-spring-boot-starter 自动配置测试
已有 agent-example-spring-boot2 集成测试
已有 ../agent-business-minimal-demo 同级独立最小业务接入样板
```

## 2. 已完成事项

```text
完成 Agent 能力平台总体设计草案
明确 SDK 集成方式和 Gateway 接入方式
明确核心抽象：AgentRuntime、AgentService、AgentTool、ToolRegistry、ModelProvider、AgentMemory、PermissionEngine、AuditService
明确第一版只支持 READ Tool
明确 JDK 8 + Spring Boot 2.3 的兼容策略
完成可行性评估
完成 MVP 收敛建议
完成 Maven 多模块结构建议
完成 Tool Schema 和 MCP 兼容策略建议
完成 Maven 多模块基础框架初始化
完成 agent-core 核心接口和默认实现
完成 agent-spring-boot-starter 自动配置
完成 agent-example-spring-boot2 示例应用
完成 /agent/chat 本地接口验证
完成项目命名空间调整：com.agenthub -> com.sean.agenthub
完成 agent-core 包结构分层
完成 Java 类型必要注释和作者信息补充
明确 MVP 暂不拆分 Boot 2 / Boot 3 双 Starter
完成 agent-core 第一批单元测试
完成 agent-spring-boot-starter 自动配置测试
完成 agent-example-spring-boot2 集成测试
新增 agent-model-provider-http 模块
完成 OpenAI-compatible Chat Completions 基础适配
完成 Anthropic-compatible Messages 基础适配
完成多 ToolCall 基础执行链路
完成 OpenAI-compatible Tool 结果消息格式基础适配
完成 Anthropic-compatible Tool 结果消息格式基础适配
完成 AgentService / AgentRuntime 流式接口基础版
完成 AgentRuntime 流式 ToolCall 决策、Tool 执行和流式总结链路
完成 /agent/chat/stream SSE 接口基础版
完成 OpenAI-compatible 文本流式响应基础适配
完成 Anthropic-compatible 文本流式响应基础适配
完成 OpenAI-compatible 流式 ToolCall 分片解析基础适配
完成 Anthropic-compatible 流式 tool_use 分片解析基础适配
完成 ModelProvider 能力声明机制
完成 Echo / OpenAI-compatible / Anthropic-compatible 能力矩阵基线
新增 agent-test-support 模块
完成 ModelProviderContract adapter 契约断言
完成 ModelProviderContract 文本响应、流式文本响应和 ToolCall 通用断言
完成 ModelProviderContract 流式 ToolCall 通用断言
完成默认 ModelProvider.streamChat 对 ToolCall 的兼容回调
完成 agent-example-spring-boot2 DeepSeek profile 配置
完成 agent-example-spring-boot2 SiliconFlow profile 配置
完成 agent-example-spring-boot2 MiMo Pay As You Go profile 配置
完成 agent-example-spring-boot2 MiMo Token Plan profile 配置
完成 agent-example-spring-boot2 MiMo Global Token Plan profile 配置
完成 AGENTHUB_* 本机环境变量约定文档
完成 DeepSeek 非流式和流式接口验证命令文档
完成 HTTP 模型配置启动期 fail-fast 校验
完成本地无鉴权 HTTP 模型网关 api-key-required 放行开关
完成未知 agent.model.protocol 启动期 fail-fast
明确 MVP 优先兼容 OpenAI-compatible 和 Anthropic-compatible 两类标准协议
明确后续不继续扩大自研模型协议面，优先做主流 AI 框架 adapter Spike
明确当前默认模型优先使用 DeepSeek / DS
明确近期不新增 MiMo / SiliconFlow / 本地模型适配任务
统一 README 与 YAML 环境变量命名：AGENTHUB_FAST_MODEL -> AGENTHUB_MODEL_DS_FAST
在根 README 补充 DS Quick Start、已验证能力和已知问题清单
完成 Tool 选择约束增强：ModelRequest 新增 systemPrompt 和 toolChoice 字段
完成 DefaultAgentRuntime 在有 Tool 注册时自动注入 Tool 选择约束 system prompt
完成 OpenAI-compatible 适配器支持从 ModelRequest 读取 tool_choice
完成 Anthropic-compatible 适配器支持 system prompt 和 tool_choice
完成示例 Tool description 增强，明确触发条件
完成 system prompt 和 tool_choice 相关测试（新增 5 个测试）
完成 OpenAI-compatible error response 检测（不再吞掉 provider 错误）
完成 Anthropic-compatible error response 检测
完成 ToolCall arguments 非法 JSON 恢复（返回空 map 而非抛异常）
完成 OpenAI-compatible 流式 JSON 解析容错（单行 malformed 不终止流）
完成 Anthropic-compatible 流式 JSON 解析容错
完成错误恢复和边界测试（新增 8 个测试）
完成 JSON Schema Structured Output 基础支持：ModelRequest 新增 responseFormat，OpenAI 适配器透传 response_format
完成 OpenAI-compatible 适配器 STRUCTURED_OUTPUT 能力声明
完成 Structured Output 相关测试（新增 2 个测试）
完成 MVP 接入说明：依赖、配置、Tool Bean、权限、审计和接口调用
完成 MVP 验收清单：本地构建、DS profile、非流式、流式、ToolCall、失败场景和第一版边界
完成 query_budget_balance 示例业务只读 Tool 试点
完成 ExamplePermissionEngine 示例权限边界：仅 finance-admin 可查询预算余额
完成 ExampleAuditService 示例审计记录，支持集成测试断言审计字段和脱敏结果
完成普通聊天不误触发 Tool、明确预算查询触发 Tool、无权限、参数缺失、业务失败集成测试
新增 agent-mcp-adapter 模块
完成 AgentTool 到 MCP Tool DTO 的最小映射
完成 ToolRegistry 到 tools/list 的最小映射
完成 AgentTool.execute 到 tools/call 的最小调用链路
完成 MCP tools/call 前置 PermissionEngine 检查和 AuditService 审计记录
完成 MCP Adapter 不依赖 MCP SDK、不依赖 agent-gateway-server 的模块边界
完成真实业务系统接入验收记录模板：`docs/agenthub-business-acceptance-record.md`
完成二阶段方向选择文档：`docs/agenthub-phase2-decision.md`
完成 ../agent-business-minimal-demo 同级独立最小业务接入样板
完成最小业务样板的订单只读 Tool、权限、审计和 HTTP 集成测试
```

当前 agent-core 包结构：

```text
api
capability
model
runtime
tool
memory
permission
audit
provider
```

## 3. 第一版目标

第一版目标是交付一个可嵌入业务系统的 Agent SDK。

核心能力：

```text
业务系统引入 starter 后获得 /agent/chat
业务系统引入 starter 后获得 /agent/chat/stream
业务系统通过 Spring Bean 注册 AgentTool
AgentRuntime 可调用模型并选择 Tool
Tool 调用前执行权限校验
Tool 调用后记录审计日志
支持非流式 chat
支持 SSE 流式 chat
支持只读 Tool
```

## 4. 第一版模块清单

```text
agent-platform-parent
├─ agent-core
├─ agent-test-support
├─ agent-model-provider-http
├─ agent-spring-boot-starter
└─ agent-example-spring-boot2
```

Starter 模块决策：

```text
当前 agent-spring-boot-starter 明确面向 JDK 8 / Spring Boot 2.3.x
MVP 不新增 agent-spring-boot-common
MVP 不新增 agent-spring-boot3-starter
等有真实 Spring Boot 3 / JDK 17+ 接入需求后再拆分
```

未来触发拆分条件：

```text
出现真实 Boot 3 业务系统接入
需要 jakarta.servlet
需要 Spring Boot 3 AutoConfiguration.imports
Boot 2 / Boot 3 starter 代码差异明显扩大
```

## 5. 第一版任务拆分

### 5.1 工程骨架

状态：已完成

任务：

```text
创建父 pom.xml
创建 agent-core 模块
创建 agent-spring-boot-starter 模块
创建 agent-example-spring-boot2 模块
统一 JDK 8 编译参数
统一依赖版本管理
```

### 5.2 agent-core

状态：已完成

任务：

```text
定义 AgentService
定义 AgentRuntime
定义 AgentTool
定义 ToolRegistry
定义 ToolSchema
定义 ToolRiskLevel
定义 ModelProvider
定义 AgentMemory
定义 PermissionEngine
定义 AuditService
实现 InMemoryToolRegistry
实现 InMemoryAgentMemory
实现 NoopPermissionEngine
实现 ConsoleAuditService
```

### 5.3 AgentRuntime 执行链路

状态：已完成基础版

任务：

```text
构建 AgentContext
加载会话历史
组装 ModelRequest
调用 ModelProvider
识别模型 ToolCall
校验 Tool 参数
执行 PermissionEngine
执行 AgentTool
记录 AuditEvent
再次调用模型总结结果
保存会话消息
返回 AgentResponse
```

### 5.4 Spring Boot Starter

状态：已完成基础版

任务：

```text
编写 AutoConfiguration
自动收集 AgentTool Bean
自动创建 ToolRegistry
自动创建 AgentService
提供 /agent/chat Controller
提供 /agent/chat/stream Controller
提供配置项 agent.enabled、agent.model
提供配置项 agent.tools.allowed-names
提供 HTTP 模型配置启动期校验
提供 Tool 白名单启动期校验
兼容 Spring Boot 2.3.x
```

说明：

```text
当前 AuditService、AgentMemory 通过同类型 Spring Bean 覆盖默认实现，暂未提供 agent.audit、agent.memory 配置项。
```

### 5.4.1 标准模型协议适配

状态：已完成基础版，当前转入 MVP 接入基线和真实业务只读 Tool 试点准备

模型协议优先级：

```text
P0 OpenAI-compatible Chat Completions
P0 Anthropic-compatible Messages
P1 Function Calling 细节增强（多 ToolCall、Tool 结果消息格式、流式 ToolCall 已完成基础版）
P1 文本流式输出（已完成基础版）
P1 JSON Schema Structured Output 基础支持（OpenAI-compatible 透传已完成）
P2 MCP adapter 设计或 PoC
P2 Spring AI / LangChain4j / Hutool AI adapter Spike（MVP 验收后再进入）
```

优先这样排的原因：

```text
OpenAI-compatible 已被 dsv4、Mimo、本地 Qwen、Gemma4、mlx 服务和多数内网模型网关广泛复用
Anthropic-compatible 覆盖 Claude 风格 Messages 和 tool_use 形态
Function Calling 在 MVP 中先做基础 ToolCall 解析，不追求一次支持所有边界
JSON Schema Structured Output 更适合在核心调用闭环稳定后增强
MCP 主要解决 Tool 生态暴露和外部工具接入，不是 MVP 内嵌 SDK 的首要依赖
Spring AI 适合 Java 17 / Spring Boot 3 体系，且已提供 MCP 支持
LangChain4j 适合 RAG、AI Service、复杂 Tool 和链路编排能力验证
Hutool AI 适合国内模型轻量快捷调用场景验证
```

MVP 验收标准：

```text
通过配置切换 echo / openai / anthropic
OpenAI-compatible 支持文本响应和单个 ToolCall
Anthropic-compatible 支持文本响应和单个 tool_use
AgentRuntime 支持同一轮模型返回多个 ToolCall 并顺序执行
AgentRuntime 流式入口支持模型流式返回 ToolCall 后执行 Tool 并继续流式总结
OpenAI-compatible 支持 Tool 结果消息回传
Anthropic-compatible 支持 tool_result 消息回传
OpenAI-compatible 支持基础文本流式响应
Anthropic-compatible 支持基础文本流式响应
OpenAI-compatible 支持基础流式 ToolCall 分片解析
Anthropic-compatible 支持基础流式 tool_use 分片解析
测试使用本地 mock HTTP server
真实模型联调当前优先 DeepSeek / DS，其他模型后续再评估
```

### 5.5 Example 项目

状态：已完成基础版

任务：

```text
创建 Spring Boot 2 示例应用
接入 agent-spring-boot-starter
实现 query_dict_item
实现 query_file_metadata
实现 query_user_info_mock
提供 curl 示例
提供 README 接入说明
```

### 5.6 测试和验收

状态：已完成第一批测试闭环

任务：

```text
agent-core 单元测试（已完成第一批）
ToolRegistry 测试（已完成）
Tool 参数校验测试（已完成）
PermissionEngine 测试（已完成）
AuditService 调用测试（已完成）
Starter 自动配置测试（已完成）
Example 集成测试（已完成）
```

已完成验证：

```text
mvn test
mvn install -DskipTests
mvn -pl agent-example-spring-boot2 spring-boot:run
curl -sS -X POST http://127.0.0.1:8080/agent/chat
curl -sS -N -X POST http://127.0.0.1:8080/agent/chat/stream
```

最近一次本地验收：

```text
2026-06-04 17:46 mvn test 通过（56 个测试）
2026-06-04 17:46 mvn install -DskipTests 通过
2026-06-04 15:20 mvn test 通过（51 个测试，含 15 个新增）
2026-06-04 15:20 mvn install -DskipTests 通过
2026-06-04 15:16 deepseek profile 启动成功
2026-06-04 15:17 /agent/chat 文本响应通过（未误选 Tool）
2026-06-04 15:17 /agent/chat/stream 文本流式响应通过（未误选 Tool）
2026-06-04 15:18 /agent/chat ToolCall 非流式链路通过
2026-06-04 15:18 /agent/chat/stream 流式 ToolCall 链路通过
2026-06-04 15:18 普通聊天不触发 Tool 验证通过（system prompt 约束生效）
2026-06-04 15:00 P0 DS 默认模型配置固化完成：统一环境变量命名、补充文档
2026-06-04 15:00 P0 Tool 选择约束增强完成：system prompt、tool_choice、Tool description
2026-06-04 15:00 P1 错误恢复和边界测试完成：error response 检测、arguments 恢复、流式容错
2026-06-04 09:36 mvn test 通过
2026-06-04 09:37 mvn install -DskipTests 通过
2026-06-04 09:37 mvn -pl agent-example-spring-boot2 spring-boot:run -Dspring-boot.run.profiles=mimo_plan 启动成功
2026-06-04 09:37 mimo_plan /agent/chat 和 /agent/chat/stream 到达真实模型服务，服务端返回模型名不支持：MiMo-2.5
2026-06-04 09:38 mvn -pl agent-example-spring-boot2 spring-boot:run -Dspring-boot.run.profiles=deepseek 启动成功
2026-06-04 09:38 deepseek 使用环境变量中的 DeepSeek-V4-Flash 调用真实模型，服务端返回模型名大小写不匹配
2026-06-04 09:39 deepseek 覆盖模型名 deepseek-v4-flash 后 /agent/chat 文本响应通过
2026-06-04 09:39 deepseek 覆盖模型名 deepseek-v4-flash 后 /agent/chat/stream 文本流式响应通过
2026-06-04 09:40 deepseek 覆盖模型名 deepseek-v4-flash 后 ToolCall 非流式链路通过
2026-06-04 09:40 deepseek 覆盖模型名 deepseek-v4-flash 后 ToolCall 流式链路通过
2026-06-03 18:37 mvn test 通过
2026-06-03 18:38 mvn install -DskipTests 通过
2026-06-03 18:38 mvn -pl agent-example-spring-boot2 spring-boot:run 启动成功
2026-06-03 18:38 /agent/chat 返回 query_user_info_mock Tool 调用结果
2026-06-03 18:38 /agent/chat/stream 返回 delta + complete SSE
2026-06-03 18:41 /agent/chat/stream Tool 场景返回 tool + delta + complete SSE
```

当前测试覆盖（61 个 @Test 方法）：

```text
InMemoryToolRegistry 注册和查询
InMemoryAgentMemory 保存、加载、清理和默认 session
DefaultAgentRuntime 直接回答链路
DefaultAgentRuntime Tool 调用和总结链路
DefaultAgentRuntime 多 ToolCall 调用和总结链路
DefaultAgentRuntime 流式直接回答链路
DefaultAgentRuntime 流式 ToolCall 调用和流式总结链路
DefaultAgentRuntime Tool 必填参数缺失拦截
DefaultAgentRuntime 权限拒绝时不执行 Tool
DefaultAgentRuntime 有 Tool 注册时自动注入 system prompt
DefaultAgentRuntime 无 Tool 注册时不注入 system prompt
AgentAutoConfiguration 默认 Bean 自动装配
AgentAutoConfiguration AgentTool Bean 自动注册
AgentAutoConfiguration 自定义 ModelProvider 覆盖默认实现
AgentAutoConfiguration openai 协议自动装配
AgentAutoConfiguration anthropic 协议自动装配
AgentAutoConfiguration HTTP 模型 base-url 缺失 fail-fast
AgentAutoConfiguration HTTP 模型 api-key 默认缺失 fail-fast
AgentAutoConfiguration 本地无鉴权 HTTP 模型 api-key-required=false 放行
AgentAutoConfiguration 未知模型协议 fail-fast
AgentAutoConfiguration agent.enabled=false 关闭自动配置
OpenAiCompatibleModelProvider 能力声明
OpenAiCompatibleModelProvider 文本响应解析
OpenAiCompatibleModelProvider /v1 base URL endpoint 拼接
OpenAiCompatibleModelProvider ToolCall 响应解析
OpenAiCompatibleModelProvider Tool 结果消息格式
OpenAiCompatibleModelProvider 文本流式响应解析
OpenAiCompatibleModelProvider 流式 ToolCall 分片解析
OpenAiCompatibleModelProvider system prompt 包含在 messages 中
OpenAiCompatibleModelProvider 从 request 读取 tool_choice
OpenAiCompatibleModelProvider tool_choice 默认为 auto
OpenAiCompatibleModelProvider 非 2xx HTTP 状态抛错
OpenAiCompatibleModelProvider provider error response 抛错
OpenAiCompatibleModelProvider 空 choices 返回空回答
OpenAiCompatibleModelProvider 非法 ToolCall arguments 恢复
OpenAiCompatibleModelProvider 流式 malformed JSON 跳过
OpenAiCompatibleModelProvider response_format 透传
OpenAiCompatibleModelProvider response_format 未设置时不发送
AnthropicCompatibleModelProvider 能力声明
AnthropicCompatibleModelProvider 文本响应解析
AnthropicCompatibleModelProvider tool_use 响应解析
AnthropicCompatibleModelProvider tool_result 消息格式
AnthropicCompatibleModelProvider 文本流式响应解析
AnthropicCompatibleModelProvider 流式 tool_use 分片解析
AnthropicCompatibleModelProvider 非 2xx HTTP 状态抛错
AnthropicCompatibleModelProvider provider error response 抛错
AnthropicCompatibleModelProvider 流式 malformed JSON 跳过
AgentMcpAdapter tools/list 映射 inputSchema
AgentMcpAdapter tools/call 权限和审计链路
AgentMcpAdapter 权限拒绝时不执行 Tool
AgentMcpAdapter 参数缺失时不进入权限检查
AgentMcpAdapter 拒绝 WRITE Tool
EchoModelProvider 能力声明
AgentExampleApplication 用户信息 Tool 集成测试
AgentExampleApplication 普通聊天不误触发 Tool
AgentExampleApplication 预算余额 Tool 权限和审计集成测试
AgentExampleApplication 预算余额 Tool 无权限拒绝集成测试
AgentExampleApplication 预算余额 Tool 参数缺失集成测试
AgentExampleApplication 预算余额 Tool 业务失败审计集成测试
```

当前测试结果：

```text
mvn test
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
```

真实模型联调状态：

```text
已完成 application-deepseek.yml 配置
已完成 application-siliconflow.yml 配置
已完成 application-mimo.yml 配置
已完成 application-mimo_plan.yml 配置
已完成 application-mimo_plan_sgp.yml 配置
已完成 application-mimo_plan_ams.yml 配置
已确认使用 OpenAI-compatible HTTP 适配器接入
已使用 AGENTHUB_* 环境变量约定，未写入密钥
2026-06-04 当前工具执行环境已读取到 AGENTHUB_* 环境变量
已完成 DeepSeek OpenAI-compatible 非流式文本真实接口验证
已完成 DeepSeek OpenAI-compatible 流式文本真实接口验证
已完成 DeepSeek OpenAI-compatible ToolCall 非流式真实接口验证
已完成 DeepSeek OpenAI-compatible ToolCall 流式真实接口验证
已发现 DeepSeek 模型名必须使用 deepseek-v4-flash / deepseek-v4-pro，不能使用 DeepSeek-V4-Flash / DeepSeek-V4-Pro 展示名
已修正 DeepSeek 和 SiliconFlow 示例默认模型名为 deepseek-v4-flash
MiMo token plan 已打到真实服务，但当前 AGENTHUB_REASON_MODEL=MiMo-2.5 被服务端返回不支持，需确认 MiMo token plan 可用模型名
2026-06-04 决策：当前不继续适配新模型，后续以 DS 为默认模型优先收敛 MVP
MiMo / SiliconFlow / 本地 Qwen / Gemma4 / mlx 暂停联调，作为后续候选
真实模型在普通介绍 AgentHub 时曾主动误选 query_dict_item，已通过 system prompt、Tool description 和 tool_choice 控制修复
已先补齐启动期配置校验，避免真实联调时环境变量未注入导致请求阶段才失败
2026-06-03 18:35 再次检查当前工具进程，AGENTHUB_* 仍不可见，因此未执行真实供应商请求
```

## 6. 暂缓事项

以下内容不进入第一版：

```text
Gateway Server
MCP Server
Admin UI
Spring AI adapter 正式接入
LangChain4j adapter 正式接入
Hutool AI adapter 正式接入
完整自研模型协议框架
完整替代 Spring AI / LangChain4j 的 AI 框架
Redis Memory
数据库审计日志
多模型动态切换
Tool 动态注册
写操作 Tool
DANGEROUS Tool
人工审批
RAG
Workflow
多 Agent 协作
完整 MCP Server
完整 JSON Schema Structured Output 校验框架
```

PostgreSQL 当前暂不接入。后续需要以下能力时再引入：

```text
持久化审计日志
持久化会话记忆
Tool 调用历史查询
后台管理调用记录
```

如果接入 PostgreSQL，建议先放在 starter 的可选配置中，不让 agent-core 直接依赖数据库。

## 7. 主要风险

### 7.1 权限边界不清

如果业务系统没有统一用户和权限模型，PermissionEngine 只能做 Tool 级控制，无法做数据级控制。

处理建议：

```text
第一版要求 ToolContext 必须携带 UserContext
业务 Tool 内部继续复用原系统权限逻辑
平台只做统一入口拦截和审计
```

### 7.2 Tool Schema 过度自定义

如果 Tool Schema 自定义过深，后续兼容模型 function calling 和 MCP 会变复杂。

处理建议：

```text
第一版采用 JSON Schema 子集
只支持常见字段和基础类型
复杂校验交给 Tool 内部处理
```

### 7.3 审计数据泄露

Tool 参数和返回结果可能包含敏感信息。

处理建议：

```text
审计默认记录摘要
参数支持脱敏
Tool 返回结果不默认全量落库
```

### 7.4 Prompt Injection

Tool 返回内容可能包含伪造指令。

处理建议：

```text
AgentRuntime 区分系统指令、用户输入和 Tool 数据
Tool 返回内容只作为业务数据上下文
模型总结阶段不得让 Tool 数据覆盖系统约束
```

## 8. 下一步建议

建议下一步从“补模型协议”转为“固化 MVP 接入基线 + 真实业务只读 Tool 试点”。
详细执行计划见 `docs/agent-platform-next-plan.md`。

当前阶段判断：

```text
MVP 当前默认模型优先 DeepSeek / DS
协议层保留 OpenAI-compatible / Anthropic-compatible
近期不新增模型适配
不继续扩大自研模型协议面
Spring AI / LangChain4j / Hutool AI adapter Spike 暂缓，不进入主链路
MCP Adapter 最小映射已完成，不实现完整 MCP Server
JSON Schema Structured Output 已完成基础透传，完整校验框架暂缓
```

推荐顺序：

```text
1. 整理 MVP 验收清单和对外接入说明
2. 选择一个真实业务只读 Tool 做试点
3. 用试点 Tool 验证权限、审计、参数脱敏和 Tool 返回内容边界
4. 根据试点结果决定是否补 Tool Schema、AuditEvent 或 PermissionResult 字段
5. 使用真实业务系统按接入文档完成第一版 MVP 验收，并填写 `docs/agenthub-business-acceptance-record.md`
6. 根据真实接入结果按 `docs/agenthub-phase2-decision.md` 决定是否评估 MCP Java SDK / Spring AI MCP、Gateway 或 Admin UI
```

第一轮开发完成后的最小验收命令应包括：

```text
mvn test
mvn install -DskipTests
mvn -pl agent-example-spring-boot2 spring-boot:run
curl -X POST http://localhost:8080/agent/chat
curl -N -X POST http://localhost:8080/agent/chat/stream
```

配置文件约定：

```text
新增 Spring Boot 配置示例优先使用 application.yml
不默认新增 application.properties
```
