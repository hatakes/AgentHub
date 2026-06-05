# AgentHub 真实业务系统接入验收记录

## 1. 使用方式

本文件用于真实业务系统接入 AgentHub 时记录验收结果。每接入一个业务系统，复制本模板并保留原始验收命令、配置摘要、Tool 边界和结论。

目标不是证明示例应用可用，而是证明业务系统按 `docs/agenthub-starter-integration.md` 接入后，可以稳定完成只读 Tool 查询、权限校验、审计记录和数据脱敏。

## 2. 接入系统信息

```text
业务系统名称：
业务系统负责人：
接入分支 / 版本：
JDK 版本：
Spring Boot 版本：
AgentHub 版本：
验收日期：
验收人：
```

## 3. 接入范围

```text
接入方式：SDK / Starter
模型协议：echo / openai / anthropic
真实模型：DeepSeek / 其他
是否启用流式接口：
是否覆盖 PermissionEngine：
是否覆盖 AuditService：
是否覆盖 AgentMemory：
```

## 4. 只读 Tool 信息

```text
Tool name：
业务场景：
风险等级：READ
参数列表：
必填参数：
权限边界：
脱敏字段：
失败成本：
```

Tool description 检查：

```text
是否写明明确触发条件：
是否写明不应触发场景：
是否避免把内部实现细节暴露给模型：
```

Tool Schema 检查：

```text
type 是否为 object：
properties 是否覆盖全部入参：
required 是否覆盖业务必填参数：
enum 是否只用于稳定枚举：
```

## 5. 配置摘要

不要记录 API key 原文。

```yaml
agent:
  enabled: true
  tools:
    allowed-names:
      - 
  model:
    protocol:
    base-url:
    api-key: "***"
    api-key-required:
    model:
    connect-timeout-ms:
    read-timeout-ms:
```

## 6. 验收命令

构建：

```bash
mvn test
mvn install -DskipTests
```

启动：

```bash
mvn spring-boot:run
```

非流式文本：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"biz-chat-001","userId":"u001","message":"请介绍一下当前系统"}'
```

非流式 ToolCall：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"biz-tool-001","userId":"u001","message":"帮我查询业务数据"}'
```

流式文本：

```bash
curl -sS -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"biz-stream-chat-001","userId":"u001","message":"你好"}'
```

流式 ToolCall：

```bash
curl -sS -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"biz-stream-tool-001","userId":"u001","message":"帮我查询业务数据"}'
```

## 7. 验收结果

| 项目 | 结果 | 证据 |
|------|------|------|
| `mvn test` 通过 | 待填 | 待填 |
| `mvn install -DskipTests` 通过 | 待填 | 待填 |
| 应用启动成功 | 待填 | 待填 |
| `/agent/chat` 文本响应通过 | 待填 | 待填 |
| `/agent/chat` ToolCall 通过 | 待填 | 待填 |
| `/agent/chat/stream` 文本流式通过 | 待填 | 待填 |
| `/agent/chat/stream` ToolCall 通过 | 待填 | 待填 |
| 普通聊天不误触发 Tool | 待填 | 待填 |
| 明确查询可触发 Tool | 待填 | 待填 |
| 无权限时不执行 Tool | 待填 | 待填 |
| 参数缺失时返回可读错误 | 待填 | 待填 |
| 业务接口失败时返回可读错误 | 待填 | 待填 |
| AuditEvent 记录完整 | 待填 | 待填 |
| Tool 结果已脱敏 | 待填 | 待填 |
| 模型总结不泄露敏感字段 | 待填 | 待填 |

## 8. 审计字段检查

```text
traceId：
sessionId：
userId：
toolName：
requestSummary：
toolResultSummary：
latencyMs：
success：
errorMessage：
createdAt：
```

审计要求：

```text
requestSummary 不记录密钥、token、密码、完整证件号
toolResultSummary 只记录摘要或脱敏字段
errorMessage 不包含完整堆栈和敏感配置
```

## 9. 风险和遗留问题

```text
权限模型风险：
数据脱敏风险：
模型误触发风险：
业务接口稳定性风险：
审计落库或日志平台风险：
其他问题：
```

## 10. 验收结论

```text
结论：通过 / 有条件通过 / 不通过
是否允许进入试运行：
是否需要补 AgentHub core 字段：
是否需要补 starter 配置：
是否需要进入 Gateway：
是否需要进入 MCP SDK Adapter：
是否需要进入 Admin UI：
```
