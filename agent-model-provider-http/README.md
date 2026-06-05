# agent-model-provider-http

`agent-model-provider-http` 提供基于 HTTP 的模型协议适配。

该模块是 MVP 阶段的轻量默认实现，用于快速联调 OpenAI-compatible / Anthropic-compatible 模型服务。后续如果接入 Spring AI、LangChain4j、Hutool AI 或 MCP SDK，应通过独立 `agent-model-provider-*` / `agent-mcp-*` 模块实现，不让 `agent-core` 直接依赖这些框架。

当前适配的是协议形态，不绑定具体模型厂商：

```text
OpenAI-compatible     /v1/chat/completions 兼容协议
Anthropic-compatible  /v1/messages 兼容协议
```

当前已覆盖基础文本响应、基础文本流式响应、基础 ToolCall/tool_use 响应、基础流式 ToolCall/tool_use 响应、多 ToolCall 解析，以及 Tool 结果消息回传格式。

OpenAI-compatible 和 Anthropic-compatible 适配器会通过 `ModelProviderCapability` 声明已覆盖能力，便于后续 Spring AI / LangChain4j / Hutool AI adapter Spike 做同口径对比。

## 适用模型

当前 MVP 默认优先 DeepSeek / DS。只要服务端暴露兼容协议，后续也可以通过配置接入其他模型，但当前不把它们作为开发优先级：

```text
当前优先：DeepSeek / DS
后续候选：Mimo
后续候选：本地 Qwen
后续候选：Gemma4
后续候选：mlx 启动的 OpenAI-compatible 服务
后续候选：其他内网模型网关
```

## 关键类

```text
HttpModelProviderProperties      HTTP 模型公共配置
OpenAiCompatibleModelProvider    OpenAI-compatible 协议适配
AnthropicCompatibleModelProvider Anthropic-compatible 协议适配
```

## 设计约束

```text
保持 JDK 8 兼容
不依赖 Spring
不绑定具体模型名称
测试使用本地 mock HTTP server，不依赖真实模型服务
```

## 包结构决策

当前 `com.sean.agenthub.agent.provider.http` 暂不继续拆子包，也不只拆目录。

原因：

```text
Java 源码目录应与 package 保持一致，不采用目录分层和 package 不一致的结构
当前模块只有少量类，扁平 package 仍可读
HttpJsonClient 和 ModelProviderJsonSupport 是包内实现细节，应保持 package-private
如果拆成 client / support 子 package，这两个 helper 需要改成 public 才能被 provider 使用，会扩大模块公开 API
```

当前公开边界：

```text
OpenAiCompatibleModelProvider
AnthropicCompatibleModelProvider
HttpModelProviderProperties
```

当前包内实现：

```text
HttpJsonClient
ModelProviderJsonSupport
ModelProviderJsonFields
```

后续当 OpenAI / Anthropic / MCP / stream / codec / client 类明显增多时，再整体拆 package，并同步重新设计公开 API 边界：

```text
com.sean.agenthub.agent.provider.http.openai
com.sean.agenthub.agent.provider.http.anthropic
com.sean.agenthub.agent.provider.http.client
com.sean.agenthub.agent.provider.http.codec
```

## 常量边界决策

协议 JSON 字段和值使用包内 `final class` 承载，不使用接口承载常量。

原因：

```text
接口表示能力契约，不应仅作为常量容器
协议字段常量虽然是常见单词，但在本模块里属于 OpenAI / Anthropic 载荷转换实现细节
public 常量会形成外部依赖点，后续拆 openai / anthropic / mcp / codec 时会增加兼容负担
当前 ModelProviderJsonFields 保持 package-private，仅服务包内协议转换逻辑
```

后续如果确实出现跨模块稳定复用需求，再单独设计公开常量类型：

```text
JsonSchemaFields
OpenAiProtocolFields
McpProtocolFields
```

公开前提是这些字段已经成为稳定外部契约，而不是仅因为字段名常见。

## YAML 配置示例

DeepSeek：

```yaml
agent:
  model:
    protocol: openai
    base-url: ${AGENTHUB_DEEPSEEK_BASE_URL}/chat/completions
    api-key: ${AGENTHUB_DEEPSEEK_API_KEY}
    model: ${AGENTHUB_MODEL_DS_FAST:deepseek-v4-flash}
```

对于已经包含 `/v1` 的 OpenAI-compatible base URL，例如 `https://api.siliconflow.cn/v1` 或 `https://api.xiaomimimo.com/v1`，适配器会自动拼接 `/chat/completions`。

OpenAI-compatible：

```yaml
agent:
  model:
    protocol: openai
    base-url: http://127.0.0.1:4000
    api-key: local-key
    model: local-model
```

Anthropic-compatible：

```yaml
agent:
  model:
    protocol: anthropic
    base-url: http://127.0.0.1:4001
    api-key: local-key
    model: local-model
```

当前真实联调优先使用 DS。后续扩展到 Mimo、本地 Qwen、Gemma4 或 mlx 服务时，主要替换 `base-url`、`api-key` 和 `model`，协议适配层仍保持模型无关。

## 协议优先级

MVP 优先实现：

```text
1. OpenAI-compatible Chat Completions
2. Anthropic-compatible Messages
```

后续再扩展：

```text
3. Function Calling 细节增强，包括流式 ToolCall 更多边界、错误恢复和更多协议差异
4. JSON Schema Structured Output，用于强约束结构化结果
5. MCP，用于把 Tool 暴露给外部 Agent 或接入外部 MCP Server
```
