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
protocol.ModelProviderJsonSupport 模型协议 JSON 转换
protocol.ModelProviderJsonFields  模型协议字段和值常量
transport.HttpJsonClient          JDK HttpURLConnection JSON 客户端
transport.HttpRequestSupport      HTTP / SSE 请求响应辅助方法
```

## 设计约束

```text
保持 JDK 8 兼容
不依赖 Spring
不绑定具体模型名称
测试使用本地 mock HTTP server，不依赖真实模型服务
```

## 包结构决策

当前已从单一 `com.sean.agenthub.agent.provider.http` 拆出内部 `protocol` / `transport` 子包。

原因：

```text
Java 源码目录应与 package 保持一致，不采用目录分层和 package 不一致的结构
OpenAI / Anthropic provider 已经开始共享较多协议转换和 HTTP 传输逻辑
protocol 用于隔离模型协议 JSON 载荷转换
transport 用于隔离 JDK HttpURLConnection、响应体读取和 SSE data 行处理
子包内 public helper 是跨 Java package 复用所需，仍按模块内部实现看待
```

当前公开边界：

```text
OpenAiCompatibleModelProvider
AnthropicCompatibleModelProvider
HttpModelProviderProperties
```

当前模块内部实现：

```text
protocol.ModelProviderJsonSupport
protocol.ModelProviderJsonFields
transport.HttpJsonClient
transport.HttpRequestSupport
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
public 常量类不等于外部稳定契约，当前仅服务模块内部协议转换逻辑
业务侧不应依赖 protocol / transport 子包类型
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
