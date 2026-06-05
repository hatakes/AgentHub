# agent-spring-boot-starter

`agent-spring-boot-starter` 是 AgentHub 面向 Spring Boot 业务系统的快速接入模块。

业务系统引入该 starter 后，可以自动获得 Agent 默认 Bean、自动注册 `AgentTool` Bean，并暴露 `/agent/chat` 和 `/agent/chat/stream` 接口。

## 职责

```text
提供 Spring Boot 自动配置
自动收集业务系统中的 AgentTool Bean
创建默认 ToolRegistry
创建默认 AgentMemory
创建默认 PermissionEngine
创建默认 AuditService
创建默认 ModelProvider
创建默认 AgentRuntime 和 AgentService
暴露 POST /agent/chat
暴露 POST /agent/chat/stream
```

## 自动配置类

```text
com.sean.agenthub.agent.starter.AgentAutoConfiguration
```

自动配置通过 `META-INF/spring.factories` 注册，兼容 Spring Boot 2.3.x。

## 配置项

当前配置项：

```yaml
agent:
  enabled: true
  tools:
    allowed-names: []
  model:
    protocol: echo
    base-url:
    api-key:
    api-key-required: true
    model:
    connect-timeout-ms: 10000
    read-timeout-ms: 60000
```

后续新增配置示例优先使用 YAML，不默认使用 `application.properties`。

`agent.tools.allowed-names` 用于限制自动注册到 `ToolRegistry` 的 Tool 名称。默认空列表表示注册全部 `AgentTool` Bean；配置后只注册名单内 Tool，名单中出现不存在的 Tool 名称会在启动期 fail-fast。

示例：

```yaml
agent:
  tools:
    allowed-names:
      - query_budget_balance
```

`agent.model.protocol` 当前支持：

```text
echo       默认本地回显模型，用于开发和测试
openai     OpenAI-compatible /v1/chat/completions
anthropic  Anthropic-compatible /v1/messages
```

当 `agent.model.protocol=openai` 或 `agent.model.protocol=anthropic` 时，starter 会在启动期校验：

```text
agent.model.base-url 必填
agent.model.model 必填
agent.model.api-key 默认必填
connect-timeout-ms / read-timeout-ms 必须大于 0
```

如果接入本地 mlx、Ollama 网关或内网无鉴权 OpenAI-compatible 服务，可以显式关闭 API Key 校验：

```yaml
agent:
  model:
    protocol: openai
    base-url: http://127.0.0.1:4000
    api-key-required: false
    model: local-model
```

## 覆盖默认实现

业务系统可以通过声明同类型 Bean 覆盖 starter 默认实现。

常见覆盖点：

```text
ModelProvider      接真实模型或公司内网模型
PermissionEngine   复用业务系统用户和数据权限
AuditService       写入日志平台或数据库
AgentMemory        接 Redis / PostgreSQL / MySQL
AgentTool          注册业务能力
```

示例：

```java
@Component
public class CustomModelProvider implements ModelProvider {
    @Override
    public ModelResponse chat(ModelRequest request) {
        // 调用真实模型
    }
}
```

## 当前范围

```text
支持非流式 chat
支持 SSE 流式 chat
支持只读 Tool
支持自动扫描 Spring Bean Tool
支持配置化 Tool 白名单
暂不支持动态 Tool 注册
暂不支持数据库持久化
暂不支持 MCP 暴露
```

## 流式接口

```bash
curl -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"s001","userId":"u001","message":"你好"}'
```

返回 Server-Sent Events：

```text
event: delta
data: {"type":"delta","text":"..."}

event: complete
data: {"type":"complete","ok":true}
```

## 测试

从仓库根目录执行：

```bash
mvn test
```

当前已覆盖：

```text
默认 Agent Bean 自动装配
AgentTool Bean 自动注册到 ToolRegistry
agent.tools.allowed-names 限制自动注册的 Tool
业务自定义 ModelProvider 覆盖默认实现
agent.model.protocol=openai 自动装配 OpenAiCompatibleModelProvider
agent.model.protocol=anthropic 自动装配 AnthropicCompatibleModelProvider
HTTP 模型配置缺失时启动期 fail-fast
本地无鉴权 HTTP 模型可通过 agent.model.api-key-required=false 放行
未知 agent.model.protocol 启动期 fail-fast
agent.enabled=false 时关闭自动配置
```
