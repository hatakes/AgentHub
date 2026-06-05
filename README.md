# AgentHub

AgentHub is a reusable Agent capability platform prototype.

It focuses on the business Agent abstraction layer: Tool registration, permission, audit, memory, runtime orchestration, and Spring Boot integration. Model invocation can be provided by HTTP-compatible adapters today and by Spring AI, LangChain4j, Hutool AI, or MCP adapters later.

Current scope:

```text
agent-core
agent-test-support
agent-model-provider-http
agent-mcp-adapter
agent-spring-boot-starter
agent-example-spring-boot2
```

## Modules

```text
agent-core                 核心抽象和默认运行时实现
agent-test-support         adapter Spike 测试支撑和契约断言
agent-model-provider-http  OpenAI / Anthropic 兼容 HTTP 模型协议适配
agent-mcp-adapter          AgentTool 到 MCP Tool 的最小映射适配
agent-spring-boot-starter  Spring Boot 2 业务系统接入层
agent-example-spring-boot2 Spring Boot 2 示例应用
```

Module docs:

```text
agent-core/README.md
agent-test-support/README.md
agent-model-provider-http/README.md
agent-mcp-adapter/README.md
agent-spring-boot-starter/README.md
agent-example-spring-boot2/README.md
```

## Build

```bash
mvn test
```

Current test coverage:

```text
agent-core                 unit tests
agent-test-support         provider contract tests
agent-model-provider-http  protocol adapter tests with mock HTTP server
agent-mcp-adapter          MCP Tool mapping and tools/call tests
agent-spring-boot-starter  auto-configuration tests
agent-example-spring-boot2 integration test
```

Current result:

```text
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
```

## Run Example

```bash
mvn install -DskipTests
mvn -pl agent-example-spring-boot2 spring-boot:run
```

## Minimal Business Demo

`../agent-business-minimal-demo` is a standalone sibling Spring Boot business service sample. It is not part of the root Maven reactor and depends on `agent-spring-boot-starter` through the local Maven repository.

```bash
mvn install -DskipTests
cd ../agent-business-minimal-demo
mvn test
mvn spring-boot:run
```

## Chat API

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"s001","userId":"u001","message":"帮我查询用户信息"}'
```

Example response:

```json
{
  "ok": true,
  "answer": "查询结果：{userId=u001, name=测试用户, department=AI 平台组}",
  "errorMessage": null,
  "toolCalls": [
    {
      "tool": "query_user_info_mock",
      "success": true,
      "errorMessage": null
    }
  ]
}
```

## Streaming Chat API

```bash
curl -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"s001","userId":"u001","message":"你好"}'
```

The streaming endpoint returns Server-Sent Events with `delta`, `tool`, `error`, and `complete` event types.

## Default Model Direction

The MVP default model direction is DeepSeek / DS through the OpenAI-compatible adapter. Other provider profiles can remain as examples or later candidates, but they are not part of the current implementation priority.

Real provider profiles use `AGENTHUB_*` environment variables and do not store API keys in the repository.

For `agent.model.protocol=openai` or `agent.model.protocol=anthropic`, the Spring Boot starter validates required HTTP model settings during startup. Local unauthenticated model gateways can opt out of API key validation with:

```yaml
agent:
  model:
    api-key-required: false
```

### DeepSeek Quick Start

```bash
export AGENTHUB_DEEPSEEK_API_KEY="sk-..."
export AGENTHUB_DEEPSEEK_BASE_URL="https://api.deepseek.com"
export AGENTHUB_MODEL_DS_FAST="deepseek-v4-flash"
mvn install -DskipTests
mvn -pl agent-example-spring-boot2 spring-boot:run -Dspring-boot.run.profiles=deepseek
```

### DS Verified Capabilities

| 能力 | 状态 | 备注 |
|------|------|------|
| 非流式文本响应 | ✅ 已验证 | |
| 流式文本响应 | ✅ 已验证 | |
| 非流式 ToolCall | ✅ 已验证 | |
| 流式 ToolCall | ✅ 已验证 | |
| 多 ToolCall | ✅ 已验证 | 顺序执行 |
| Tool 结果消息回传 | ✅ 已验证 | |
| 普通聊天不触发 Tool | ✅ 已验证 | system prompt 约束生效 |

### DS Known Issues

| 问题 | 状态 | 备注 |
|------|------|------|
| 模型名大小写敏感 | 已规避 | 必须使用 `deepseek-v4-flash`，不能使用 `DeepSeek-V4-Flash` 展示名 |
| 普通聊天误选 Tool | ✅ 已修复 | 通过 system prompt + Tool description 约束，已验证生效 |

## Documents

```text
docs/agent-platform-design.md
docs/agent-platform-progress.md
docs/agent-platform-next-plan.md
docs/agenthub-starter-integration.md
docs/agenthub-mvp-acceptance.md
docs/agenthub-business-acceptance-record.md
docs/agenthub-phase2-decision.md
```
