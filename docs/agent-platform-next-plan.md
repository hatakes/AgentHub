# AgentHub 下一步开发计划

## 1. 当前判断

第一版 MVP 的核心技术闭环已经完成：

```text
agent-core 核心抽象和默认实现
agent-model-provider-http OpenAI-compatible / Anthropic-compatible 基础适配
agent-mcp-adapter AgentTool 到 MCP Tool 的最小映射适配
agent-spring-boot-starter 自动配置
agent-example-spring-boot2 示例接口
非流式 chat
SSE 流式 chat
多 ToolCall
基础流式 ToolCall
Tool 结果消息回传
Tool 选择约束
错误恢复和边界测试
JSON Schema Structured Output 基础透传
DeepSeek / DS 真实链路第一轮验收
文档基线和常量边界决策收敛
```

当前不再横向扩模型，也不进入完整 Gateway / MCP / Admin UI。下一阶段重点是把 MVP 从“技术可用”推进到“业务可接入”：文档基线一致、验收清单固定、接入说明清楚，并用一个真实业务只读 Tool 验证权限、审计和数据边界。

智能附件分析可以作为下一阶段业务样板放在本仓库内推进，但应保持为独立业务模块或示例应用，通过 `agent-spring-boot-starter` 接入 AgentHub。附件上传、OCR、文件解析、文档类型识别、字段抽取和业务规则判断不进入 `agent-core`。

## 2. 当前阶段原则

```text
默认模型继续优先 DeepSeek / DS
近期不新增模型适配
不因为其他供应商差异改动 agent-core
不在第一版实现完整 MCP Server / Client
不在第一版实现 Gateway Server
不在第一版实现 Admin UI
不在第一版实现写操作 Tool 和人工审批
不把附件上传、OCR、文件解析和材料审核规则下沉到 agent-core
Spring AI / LangChain4j 已作为 JDK 17+ 自动激活 profile adapter Spike 启动，Hutool AI 后续再评估
MCP 先做映射设计和最小 PoC，不绑定 Gateway 实现
```

## 3. P0：智能附件分析业务样板设计

状态：已完成第一版最小接入，错误场景验收已补齐

目标：用一个通用“智能附件分析”样板验证 AgentHub 在文件类业务中的接入方式，同时避免把具体业务能力混入平台内核。

推荐模块：

```text
agent-document-processing
```

设计边界：

```text
agent-document-processing 可以放在本仓库
agent-document-processing 通过 agent-spring-boot-starter 接入 AgentHub
agent-core 只保留 AgentRuntime、AgentTool、权限、审计、记忆和模型抽象
附件存储、OCR、解析、规则、审核意见模板都属于业务样板模块
```

第一版能力：

```text
1. 上传附件并生成 attachmentId
2. 根据 attachmentId 触发附件分析
3. 支持 mock OCR 或本地文本解析
4. 识别 documentType，不直接绑定身份证
5. 抽取结构化字段
6. 执行至少一条确定性业务规则
7. 生成结构化分析结果和审核意见
8. 记录 Tool 权限校验和审计事件
9. 提供上传并立即分析的一体化接口，方便其他服务一次调用
```

推荐 Tool：

```text
parse_attachment
ocr_attachment
classify_document
extract_document_fields
check_document_rules
summarize_attachment_analysis
```

验收：

```text
普通聊天不误触发附件分析 Tool
明确要求分析 attachmentId 时触发 Tool 链路
无权限时拒绝分析并记录审计
OCR 或解析失败时返回可读错误
规则判断结果由业务代码确定，LLM 不覆盖确定性结果
敏感字段默认脱敏后再进入模型总结和审计记录
保留分步上传 / 分析接口，同时提供 analyze-file 一体化接口
```

### 3.1 P0：智能附件分析样板分层重构

状态：已完成

`agent-document-processing` 第一版快速验证链路后，已按 api / application / domain / infrastructure / tool / support 完成包结构分层。后续扩展真实 OCR、文件解析或更多材料类型时，继续沿用该边界。

目标包结构：

```text
com.sean.agenthub.agent.attachment
├─ api
├─ application
├─ domain
├─ infrastructure
├─ tool
└─ support
```

重构任务：

```text
1. 将 AttachmentUploadController 移入 api                                      ✅
2. 将 AttachmentRecord 和后续分析结果模型移入 domain                            ✅
3. 将 AttachmentRepository、AttachmentAuditService、AttachmentPermissionEngine 移入 infrastructure ✅
4. 将所有 AgentTool 实现移入 tool                                               ✅
5. 将 AttachmentAnalysisModelProvider 移入 application                           ✅
6. 将 AttachmentToolSupport 暂放 support，并逐步拆成 domain 规则和 infrastructure adapter ✅
7. 补充分层后的集成测试，确保现有上传、分析、权限、审计和脱敏行为不变              ✅
```

验收：

```text
agent-document-processing 不再把所有类型放在一个目录
包名能体现 api / application / domain / infrastructure / tool 边界
mvn -pl agent-document-processing test 通过
mvn '-P!adapters-java17' test 通过
```

## 4. P0：文档和验收基线收敛 ✅ 已完成

目标：让设计文档、进度文档、下一步计划和 README 讲同一套状态，后续开发只看文档就能判断当前边界。

任务：

```text
1. 对齐 design / progress / next-plan 的 MVP 状态                    ✅
2. 明确基础流式 ToolCall 已完成，后续只做协议边界增强                  ✅
3. 明确 Structured Output 已完成基础透传，完整校验框架暂缓             ✅
4. 明确 Spring AI / LangChain4j / Hutool AI adapter Spike 不进入第一版主链路 ✅
5. 明确 agent-mcp-adapter 不默认依赖 agent-gateway-server             ✅
6. 固化最小验收命令和真实 DS 验收口径                                  ✅
```

验收：

```text
docs/agent-platform-design.md 不再包含过期状态
docs/agent-platform-progress.md 只记录已完成事实和真实下一步
docs/agent-platform-next-plan.md 只描述当前阶段计划
README 中的 Quick Start 与文档验收命令一致
```

补充设计决策：

```text
ModelProviderCapability 迁入 core.capability
agent-model-provider-http 已拆出 protocol / transport 内部子包
OpenAiCompatibleModelProvider、AnthropicCompatibleModelProvider、HttpModelProviderProperties 是稳定公开边界
protocol / transport 下的 public helper 是模块内部复用类型，业务侧不应作为稳定 API 依赖
协议字段常量使用 final class，不使用 interface 常量容器
常见字段名不等于公开 API，只有外部稳定复用场景才纳入公开契约
```

## 5. P0：MVP 接入说明和验收清单 ✅ 已完成

目标：业务系统接入时知道最少需要配置什么、实现什么、怎么验收。

任务：

```text
1. 整理 Starter 接入步骤：依赖、配置、Tool Bean、接口调用                 ✅
2. 整理 HTTP ModelProvider 配置项：protocol、base-url、api-key、model、timeout ✅
3. 整理 Tool 编写规范：name、description、schema、riskLevel、execute       ✅
4. 整理权限和审计接入点：PermissionEngine、AuditService 覆盖方式          ✅
5. 整理本地和 DS profile 的最小验收命令                                  ✅
6. 标记第一版不支持的能力：写操作、动态注册、Gateway、完整 MCP             ✅
```

验收：

```text
新业务系统可按文档完成只读 Tool 接入
mvn test 通过
mvn install -DskipTests 通过
/agent/chat 文本响应通过
/agent/chat ToolCall 链路通过
/agent/chat/stream 文本流式通过
/agent/chat/stream ToolCall 流式链路通过
```

## 6. P1：真实业务只读 Tool 试点 ✅ 已完成示例试点

目标：用一个真实业务只读场景验证 AgentHub 的业务边界，而不是继续只跑 mock Tool。

候选 Tool：

```text
query_user_info
query_dict_item
query_file_metadata
query_budget_balance
```

选择标准：

```text
只读
参数少
权限边界明确
返回数据可脱敏
失败成本低
能代表真实业务查询路径
```

任务：

```text
1. 选定一个真实业务只读 Tool                                      ✅ query_budget_balance 示例试点
2. 定义 Tool Schema 和 description 触发条件                         ✅
3. 接入业务系统原有权限逻辑或 mock 权限逻辑                         ✅ ExamplePermissionEngine
4. 明确 AuditEvent 记录字段和脱敏策略                               ✅ ExampleAuditService + Tool 脱敏返回
5. 验证普通聊天不误触发 Tool                                        ✅
6. 验证明示查询可触发 Tool                                          ✅
7. 验证无权限、参数缺失、业务接口失败时的返回和审计                  ✅
```

验收：

```text
Tool 参数按 Schema 校验
Tool 调用前经过 PermissionEngine
Tool 调用后记录 AuditEvent
Tool 返回内容不覆盖 system prompt 约束
模型总结不泄露不应展示的敏感字段
失败场景有可读错误信息
```

## 7. P1：MCP Adapter 最小设计 ✅ 已完成

目标：先设计最小映射，不急于实现完整 MCP Server。

设计边界：

```text
agent-mcp-adapter 依赖 agent-core
agent-mcp-adapter 不默认依赖 agent-gateway-server
Gateway 后续可以组合 agent-mcp-adapter 对外暴露 MCP API
agent-core 不依赖 MCP SDK
```

最小映射：

```text
AgentTool.name        -> MCP Tool name                         ✅
AgentTool.description -> MCP Tool description                  ✅
AgentTool.schema      -> MCP Tool inputSchema                  ✅
AgentTool.execute     -> MCP tools/call                        ✅
PermissionEngine      -> MCP tools/call 前置权限检查             ✅
AuditService          -> MCP tools/call 审计记录                 ✅
```

暂不实现：

```text
完整 MCP Server
完整 MCP Client
STDIO 本地命令能力
Resources / Prompts 完整协议面
Gateway 多租户 MCP 暴露
```

## 8. P0 / P1：当前缺口收敛

状态：进行中

当前项目已经有 SDK、starter、HTTP provider、MCP adapter、示例业务 Tool、外部最小业务样板和智能附件分析样板。下一阶段不继续横向铺能力，先把“真实接入会遇到的问题”补扎实。

P0 缺口：

```text
设计文档、README、进度文档持续同步实际包结构和能力边界
保持 Maven 测试全绿，避免文档推进时破坏主链路
补生产业务系统接入验收记录，至少选一个真实只读 Tool 走完整权限、审计、脱敏和失败场景
基于 DelegatingAgentRuntime 继续补日志、指标、Trace、耗时统计这类非侵入增强样例
```

P0 已推进补强：

```text
InMemoryAgentMemory 每 session 消息上限，避免默认内存记忆无限增长
ModelRequest 旧单 Tool 字段标记兼容，新 provider 优先读取 lastToolExecutions
Tool 参数 required / 已知类型 / enum 校验前置到权限检查之前
Tool 结果审计摘要默认脱敏并截断
Starter HTTP 入口补请求体、sessionId、message 最小校验
Tool 结果不能作为新指令执行的最小 Prompt Injection 防护提示
```

P1 缺口：

```text
agent-document-processing 补真实文件解析 adapter 方向，优先 Word / Excel / 图片 OCR 的扩展点和失败语义
补持久化落地方向：AgentMemory、AuditEvent、附件记录至少给一个 Redis 或数据库样板方案
补可观测性口径：TraceId、模型耗时、Tool 耗时、Token 用量、错误分类
补安全治理口径：Prompt Injection 隔离、敏感字段策略、WRITE / DANGEROUS Tool 审批模型
```

暂缓：

```text
在没有真实生产接入反馈前，不启动完整 Gateway Server / Admin UI
在没有明确外部 Agent 接入需求前，不启动完整 MCP Server / Client
在 DS / OpenAI-compatible 主链路未继续打稳前，不扩大更多模型供应商真实联调
```

## 9. 暂不进入

```text
新增模型适配
MiMo / SiliconFlow / 本地模型真实联调
Anthropic-compatible 真实网关联调
Hutool AI adapter Spike
Gateway Server
Admin UI
数据库持久化
多租户模型路由
完整 JSON Schema Structured Output 校验框架
完整 MCP Server / Client
写操作 Tool
人工审批流
RAG
Workflow
多 Agent 协作
把 OCR / 文件解析能力做成 AgentHub core 内置能力
围绕身份证单一材料类型设计平台抽象
```

已启动但不进入默认主链路：

```text
agent-model-provider-langchain4j：JDK 17+ profile 下完成 TEXT_CHAT / TEXT_STREAM / Tool schema 下发 / ToolCall 响应映射
agent-model-provider-spring-ai：JDK 17+ profile 下完成 TEXT_CHAT / TEXT_STREAM / Tool schema 下发 / ToolCall 响应映射
```

## 10. 最小验收命令

```bash
mvn test
mvn install -DskipTests
mvn -pl agent-example-spring-boot2 spring-boot:run -Dspring-boot.run.profiles=deepseek
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"ds-chat-001","userId":"u001","message":"请介绍一下 AgentHub"}'
curl -sS -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"ds-tool-001","userId":"u001","message":"帮我查询用户信息"}'
```

## 11. 下一次开发入口

同级独立最小业务样板 `../agent-business-minimal-demo` 已完成第一份外部业务接入验收记录：

```text
docs/agenthub-business-acceptance-agent-business-minimal-demo.md
```

下一次开发优先从 `生产业务系统接入验收` 开始；完成至少一次生产业务接入验收后，再按 `docs/agenthub-phase2-decision.md` 选择二阶段方向。

如果选择先做智能附件分析样板，则从 `agent-document-processing` 开始，目标是完成一个通用附件分析最小闭环，而不是直接建设身份证专用 Agent。

建议交付物：

```text
使用生产业务系统接入 docs/agenthub-starter-integration.md
选择一个生产只读查询 Tool
复制 docs/agenthub-business-acceptance-record.md 完成生产验收记录
对照 docs/agenthub-business-acceptance-agent-business-minimal-demo.md 检查差异
根据接入结果决定是否进入 Gateway、MCP SDK Adapter 或 Admin UI
或新增 agent-document-processing，完成智能附件分析业务样板验收
```

已完成交付物：

```text
docs/agenthub-mvp-acceptance.md
docs/agenthub-starter-integration.md
agent-example-spring-boot2 query_budget_balance 示例只读 Tool 试点
agent-mcp-adapter 最小模块
AgentTool -> MCP Tool 映射对象
PermissionEngine / AuditService 在 MCP tools/call 前后复用
docs/agenthub-business-acceptance-record.md
docs/agenthub-business-acceptance-agent-business-minimal-demo.md
docs/agenthub-phase2-decision.md
../agent-business-minimal-demo 同级独立最小业务接入样板
agent-document-processing 智能附件分析业务样板第一版
docs/agenthub-attachment-analysis-acceptance.md 智能附件分析业务样板验收记录
agent-model-provider-langchain4j Java 17 TEXT_CHAT / TEXT_STREAM / Tool schema 下发 / ToolCall 响应映射 adapter Spike
agent-model-provider-spring-ai Java 17 TEXT_CHAT / TEXT_STREAM / Tool schema 下发 / ToolCall 响应映射 adapter Spike
agent-model-provider-http protocol / transport 内部拆包
AgentRuntime 接口 + 默认实现 + DelegatingAgentRuntime 可选包装器设计说明
```
