# AgentHub MVP 验收清单

## 1. 验收目标

本清单用于判断第一版 MVP 是否达到业务系统可接入状态。

验收重点：

```text
业务系统可引入 starter
只读 Tool 可注册和调用
Tool 调用前经过 PermissionEngine
Tool 调用后记录 AuditEvent
HTTP ModelProvider 配置可 fail-fast
/agent/chat 非流式链路可用
/agent/chat/stream SSE 流式链路可用
DeepSeek / DS OpenAI-compatible 真实链路可用
```

## 2. 本地构建验收

在仓库根目录执行：

```bash
mvn test
mvn install -DskipTests
```

通过标准：

```text
mvn test 无失败
mvn install -DskipTests 可完成多模块安装
```

MVP Java 8 reactor 文档基线结果：

```text
Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
```

说明：`adapters-java17` profile 在 JDK 17+ 自动激活，用于让 Spring AI / LangChain4j adapter Spike 在 IDE 和 Maven reactor 中被识别。JDK 17+ 下默认 `mvn test` 会包含这两个模块。

JDK 17+ reactor 验证：

```bash
mvn test
```

```text
Tests run: 79, Failures: 0, Errors: 0, Skipped: 0
```

如需在 JDK 17+ 下只验证第一版 MVP 主链路，可显式禁用 profile：

```bash
mvn '-P!adapters-java17' test
```

`adapters-java17` 只用于验证 Spring AI / LangChain4j 的 `TEXT_CHAT` / `TEXT_STREAM`、Tool schema 下发和 ToolCall 响应映射最小适配，不作为第一版 MVP 通过门槛。

## 3. 示例应用启动

本地 echo 模型：

```bash
mvn -pl agent-example-spring-boot2 spring-boot:run
```

DeepSeek / DS profile：

```bash
export AGENTHUB_DEEPSEEK_API_KEY="sk-..."
export AGENTHUB_DEEPSEEK_BASE_URL="https://api.deepseek.com"
export AGENTHUB_MODEL_DS_FAST="deepseek-v4-flash"
mvn -pl agent-example-spring-boot2 spring-boot:run -Dspring-boot.run.profiles=deepseek
```

通过标准：

```text
应用监听 8080
openai / anthropic 缺少必填配置时启动期 fail-fast
未知 agent.model.protocol 启动期 fail-fast
本地无鉴权网关配置 agent.model.api-key-required=false 时可启动
```

## 4. 非流式文本响应

命令：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"ds-chat-001","userId":"u001","message":"请介绍一下 AgentHub"}'
```

通过标准：

```text
HTTP 200
响应 ok=true
answer 非空
普通介绍类问题不触发 Tool
toolCalls 为空或不包含成功 Tool 调用
```

## 5. 非流式 ToolCall

命令：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"ds-tool-001","userId":"u001","message":"帮我查询用户信息"}'
```

通过标准：

```text
HTTP 200
响应 ok=true
模型选择只读 Tool
Tool 参数按 schema 生成和解析
Tool 调用前经过 PermissionEngine
Tool 调用后记录 AuditEvent
Tool 返回结果参与模型总结
answer 不泄露不应展示的敏感字段
```

## 6. 流式文本响应

命令：

```bash
curl -sS -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"ds-stream-chat-001","userId":"u001","message":"你好"}'
```

通过标准：

```text
HTTP 200
Content-Type 为 text/event-stream
返回 delta 事件
最后返回 complete 事件
普通聊天不触发 tool 事件
```

## 7. 流式 ToolCall

命令：

```bash
curl -sS -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"ds-stream-tool-001","userId":"u001","message":"帮我查询用户信息"}'
```

通过标准：

```text
HTTP 200
Content-Type 为 text/event-stream
出现 tool 事件
Tool 调用结果 success=true
随后出现 delta 事件输出模型总结
最后返回 complete 事件
```

## 8. 权限和审计验收

业务系统覆盖 `PermissionEngine` 后验收：

```text
有权限用户可以调用目标 Tool
无权限用户返回可读拒绝原因
无权限场景不执行 AgentTool.execute
权限拒绝场景有审计或可追踪日志
```

业务系统覆盖 `AuditService` 后验收：

```text
成功 Tool 调用记录 success=true
失败 Tool 调用记录 success=false
记录 sessionId、userId、toolName、latencyMs
requestSummary 和 toolResultSummary 已脱敏
errorMessage 不包含密钥、token 或完整异常堆栈
```

## 9. 失败场景验收

必须覆盖：

```text
Tool arguments 非法 JSON 不导致运行时崩溃
模型服务返回 error response 时不被吞掉
流式响应中单行 malformed JSON 不终止整个流
Tool 无权限时返回可读错误
Tool 参数缺失时返回可读错误
业务接口失败时返回 ToolResult.error
```

## 10. 第一版边界

验收时不要求覆盖：

```text
写操作 Tool
动态 Tool 注册
Gateway Server
Admin UI
完整 MCP Server / Client
数据库持久化
多租户模型路由
完整 Structured Output 校验框架
Spring AI / LangChain4j / Hutool AI adapter 正式接入
MiMo / SiliconFlow / 本地模型真实联调
```

## 11. 通过判定

满足以下条件即可判定 MVP 达到业务接入基线：

```text
mvn test 通过
mvn install -DskipTests 通过
示例应用可启动
/agent/chat 文本响应通过
/agent/chat ToolCall 链路通过
/agent/chat/stream 文本流式通过
/agent/chat/stream ToolCall 流式链路通过
只读 Tool 的权限、审计和脱敏边界明确
第一版不支持项已在接入文档中说明
```
