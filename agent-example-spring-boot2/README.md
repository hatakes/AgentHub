# agent-example-spring-boot2

`agent-example-spring-boot2` 是 AgentHub 的 Spring Boot 2 示例应用。

它用于验证 `agent-spring-boot-starter` 的接入方式和 MVP 执行链路，不依赖真实大模型。

## 职责

```text
演示如何引入 agent-spring-boot-starter
演示如何注册 AgentTool Bean
演示如何覆盖默认 ModelProvider
验证 /agent/chat 接口
验证 /agent/chat/stream 接口
验证模型选择 Tool -> Tool 执行 -> 模型总结的闭环
```

## 示例组件

```text
AgentExampleApplication   示例应用启动入口
ExampleModelProvider      规则型模型供应商，用于模拟模型选择 Tool
QueryDictItemTool         查询模拟字典项
QueryFileMetadataTool     查询模拟文件元数据
QueryUserInfoMockTool     查询模拟用户信息
```

## 配置

示例应用使用 YAML 配置：

```yaml
server:
  port: 8080

agent:
  enabled: true
```

## 启动

从仓库根目录执行：

```bash
mvn install -DskipTests
mvn -pl agent-example-spring-boot2 spring-boot:run
```

## 启动 DeepSeek

DeepSeek 官方 API 兼容 OpenAI Chat Completions 协议，示例工程通过 `agent-model-provider-http` 的 OpenAI-compatible 适配器接入。

从仓库根目录执行：

```bash
export AGENTHUB_DEEPSEEK_API_KEY=你的 DeepSeek API Key
export AGENTHUB_DEEPSEEK_BASE_URL=https://api.deepseek.com
export AGENTHUB_MODEL_DS_FAST=deepseek-v4-flash
mvn install -DskipTests
mvn -pl agent-example-spring-boot2 spring-boot:run -Dspring-boot.run.profiles=deepseek
```

也可以使用统一默认 provider 变量。当前 MVP 默认模型方向优先使用 DS：

```bash
export AGENTHUB_LLM_PROVIDER=deepseek
mvn -pl agent-example-spring-boot2 spring-boot:run -Dspring-boot.run.profiles=${AGENTHUB_LLM_PROVIDER}
```

这些真实供应商 profile 默认要求 API Key 存在。如果只接本地无鉴权模型网关，可以额外传入：

```bash
-Dspring-boot.run.arguments=--agent.model.api-key-required=false
```

## Provider Profiles

示例工程内置以下 profile。当前开发和验收优先 `deepseek`，其他 profile 先作为后续候选保留，不作为近期适配计划：

```text
deepseek       DeepSeek 官方 API
siliconflow    SiliconFlow OpenAI-compatible API
mimo           MiMo Pay As You Go
mimo_plan      MiMo Token Plan
mimo_plan_sgp  MiMo Global Token Plan Singapore endpoint
mimo_plan_ams  MiMo Global Token Plan Amsterdam endpoint
```

环境变量约定：

```bash
export AGENTHUB_DEEPSEEK_API_KEY="sk-..."
export AGENTHUB_DEEPSEEK_BASE_URL="https://api.deepseek.com"

export AGENTHUB_SILICONFLOW_API_KEY="sk-..."
export AGENTHUB_SILICONFLOW_BASE_URL="https://api.siliconflow.cn/v1"

export AGENTHUB_MIMO_API_KEY="sk-..."
export AGENTHUB_MIMO_BASE_URL="https://api.xiaomimimo.com/v1"

export AGENTHUB_MIMO_PLAN_API_KEY="tp-sk-..."
export AGENTHUB_MIMO_PLAN_BASE_URL="https://token-plan-cn.xiaomimimo.com/v1"
export AGENTHUB_MIMO_PLAN_SGP_BASE_URL="https://token-plan-sgp.xiaomimimo.com/v1"
export AGENTHUB_MIMO_PLAN_AMS_BASE_URL="https://token-plan-ams.xiaomimimo.com/v1"

export AGENTHUB_LLM_PROVIDER="deepseek"
export AGENTHUB_MODEL_DS_FAST="deepseek-v4-flash"
export AGENTHUB_MODEL_DS_PRO="deepseek-v4-pro"
```

注意：仓库只记录环境变量名称，不记录 API Key 值。

## 验证接口

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"s001","userId":"u001","message":"帮我查询用户信息"}'
```

预期会返回 `query_user_info_mock` 的 Tool 调用结果。

DeepSeek 直连验证：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"ds001","userId":"u001","message":"请用一句话介绍 DeepSeek"}'
```

DeepSeek 流式验证：

```bash
curl -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"ds002","userId":"u001","message":"请用三句话介绍 AgentHub"}'
```

## 示例模型规则

`ExampleModelProvider` 根据用户输入中的关键词模拟模型决策：

```text
包含“字典” -> query_dict_item
包含“文件” -> query_file_metadata
包含“用户” -> query_user_info_mock
其他输入   -> 直接回显
```

该模块只用于演示和联调，不承载平台核心逻辑。

## 测试

从仓库根目录执行：

```bash
mvn test
```

当前已覆盖：

```text
随机端口启动 Spring Boot 示例应用
通过 HTTP 调用 /agent/chat
通过 HTTP 调用 /agent/chat/stream
验证 query_user_info_mock Tool 被执行
验证 query_budget_balance Tool 权限、审计和 SSE 输出
验证接口返回 Tool 调用摘要
```
