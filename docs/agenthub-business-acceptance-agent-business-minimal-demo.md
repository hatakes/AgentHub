# AgentHub 业务接入验收记录：agent-business-minimal-demo

## 1. 接入系统信息

```text
业务系统名称：agent-business-minimal-demo
业务系统负责人：Sean
接入分支 / 版本：0.1.0-SNAPSHOT
JDK 版本：本机 Maven 运行环境
Spring Boot 版本：2.3.12.RELEASE
AgentHub 版本：0.1.0-SNAPSHOT
验收日期：2026-06-06
验收人：Codex
```

## 2. 接入范围

```text
接入方式：SDK / Starter
模型协议：自定义 DemoModelProvider
真实模型：未接入真实模型，避免样板依赖 API key
是否启用流式接口：是，覆盖 /agent/chat/stream ToolCall 样板链路
是否覆盖 PermissionEngine：是，DemoPermissionEngine
是否覆盖 AuditService：是，DemoAuditService
是否覆盖 AgentMemory：否，使用 starter 默认实现
```

## 3. 只读 Tool 信息

```text
Tool name：query_order_status
业务场景：查询订单状态
风险等级：READ
参数列表：orderNo
必填参数：orderNo
权限边界：仅 userId=order-reader 允许查询
脱敏字段：buyerMasked 返回用户***001，不返回完整买家信息
失败成本：低，样板内存数据，不访问真实业务数据
```

Tool description 检查：

```text
是否写明明确触发条件：是，仅用户明确要求查询订单状态、订单详情时调用
是否写明不应触发场景：是，不要在闲聊或介绍类问题中使用
是否避免把内部实现细节暴露给模型：是
```

Tool Schema 检查：

```text
type 是否为 object：是，ToolSchema 默认 object
properties 是否覆盖全部入参：是，orderNo
required 是否覆盖业务必填参数：是，orderNo
enum 是否只用于稳定枚举：无 enum
```

## 4. 配置摘要

```yaml
server:
  port: 18080

agent:
  enabled: true
  tools:
    allowed-names:
      - query_order_status
  model:
    protocol: echo
```

说明：样板通过 Spring Bean 覆盖 `ModelProvider`，实际使用 `DemoModelProvider`，不依赖真实模型 API key。

## 5. 验收命令

AgentHub 本地安装：

```bash
mvn install -DskipTests
```

业务样板测试：

```bash
cd ../agent-business-minimal-demo
mvn test
```

可选手工启动：

```bash
cd ../agent-business-minimal-demo
mvn spring-boot:run
```

普通聊天：

```bash
curl -sS -X POST http://127.0.0.1:18080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"demo-chat-001","userId":"demo-user","message":"你好"}'
```

ToolCall：

```bash
curl -sS -X POST http://127.0.0.1:18080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"demo-tool-001","userId":"order-reader","message":"帮我查询订单状态"}'
```

无权限：

```bash
curl -sS -X POST http://127.0.0.1:18080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"demo-denied-001","userId":"demo-user","message":"帮我查询订单状态"}'
```

## 6. 验收结果

| 项目 | 结果 | 证据 |
|------|------|------|
| AgentHub `mvn test` 通过 | 通过 | 2026-06-07 本地执行，65 tests passed |
| AgentHub `mvn install -DskipTests` 通过 | 通过 | 2026-06-06 本地执行，BUILD SUCCESS |
| 业务样板 `mvn test` 通过 | 通过 | 2026-06-07 本地执行，7 tests passed |
| 应用启动成功 | 未手工验收 | 集成测试使用 RANDOM_PORT 启动 Spring Boot 上下文并调用 HTTP 接口 |
| `/agent/chat` 文本响应通过 | 通过 | `shouldAnswerNormalChatWithoutTool` |
| `/agent/chat` ToolCall 通过 | 通过 | `shouldExecuteOrderToolWithPermissionAndAudit` |
| `/agent/chat/stream` 文本流式通过 | 部分通过 | `shouldStreamBudgetBalanceToolWithPermissionAndAudit` 覆盖 Tool 执行后的 delta SSE |
| `/agent/chat/stream` ToolCall 通过 | 通过 | `shouldStreamBudgetBalanceToolWithPermissionAndAudit` 覆盖 tool / delta / complete SSE |
| 普通聊天不误触发 Tool | 通过 | 普通聊天返回空 toolCalls，审计事件为空 |
| 明确查询可触发 Tool | 通过 | 订单查询触发 query_order_status |
| 无权限时不执行 Tool | 通过 | userId=demo-user 返回 Tool permission denied |
| 参数缺失时返回可读错误 | 未覆盖 | 样板测试未覆盖缺参 |
| 业务接口失败时返回可读错误 | 未覆盖 | 样板 Tool 未模拟业务失败 |
| AuditEvent 记录完整 | 部分通过 | 测试断言 sessionId、userId、toolName、success/errorMessage |
| Tool 结果已脱敏 | 通过 | 返回 buyerMasked=用户***001 |
| 模型总结不泄露敏感字段 | 通过 | 样板只返回脱敏字段，未生成完整敏感字段 |

## 7. 审计字段检查

```text
traceId：未断言
sessionId：已断言
userId：已断言
toolName：已断言
requestSummary：未断言
toolResultSummary：未断言
latencyMs：未断言
success：已断言
errorMessage：已断言无权限错误
createdAt：未断言
```

审计要求：

```text
requestSummary 不记录密钥、token、密码、完整证件号：样板无相关字段
toolResultSummary 只记录摘要或脱敏字段：样板返回脱敏买家信息
errorMessage 不包含完整堆栈和敏感配置：无权限错误为 Only order-reader can query order status
```

## 8. 风险和遗留问题

```text
权限模型风险：当前只验证 userId 白名单，生产系统需接入真实 RBAC / 数据权限。
数据脱敏风险：样板只返回 buyerMasked，生产系统需在 ToolResult 返回前完成字段级脱敏。
模型误触发风险：样板 DemoModelProvider 固定触发逻辑，不能代表真实模型误触发概率。
业务接口稳定性风险：样板未访问真实下游接口，未覆盖超时、空结果和业务异常。
审计落库或日志平台风险：样板使用内存 List，生产系统需接数据库、日志平台或审计系统。
其他问题：纯文本流式接口尚未单独覆盖；参数缺失和业务失败场景已在非流式样板中覆盖。
```

## 9. 验收结论

```text
结论：有条件通过
是否允许进入试运行：允许作为外部业务接入样板，不等同生产业务通过
是否需要补 AgentHub core 字段：暂不需要
是否需要补 starter 配置：暂不需要
是否需要进入 Gateway：暂不需要
是否需要进入 MCP SDK Adapter：暂不需要
是否需要进入 Admin UI：暂不需要
```

下一步：

```text
1. 选择生产业务系统做一次真实接入验收。
2. 生产接入必须覆盖纯文本流式接口、ToolCall 流式接口、参数缺失和业务失败场景。
3. 生产接入后再依据 docs/agenthub-phase2-decision.md 判断二阶段方向。
```
