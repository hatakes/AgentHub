# AgentHub Starter 接入说明

## 1. 接入目标

业务系统引入 `agent-spring-boot-starter` 后，可以在本系统内获得：

```text
POST /agent/chat
POST /agent/chat/stream
AgentTool Bean 自动注册
PermissionEngine 前置权限检查
AuditService Tool 调用审计
AgentMemory 会话记忆
HTTP ModelProvider 模型调用
```

第一版定位为只读 Tool 接入。写操作 Tool、动态 Tool 注册、Gateway、完整 MCP Server / Client 和 Admin UI 暂不支持。

## 2. 引入依赖

业务系统使用 Maven 引入 starter。当前项目还未发布到远端仓库时，先在本地执行：

```bash
mvn install -DskipTests
```

业务系统依赖：

```xml
<dependency>
    <groupId>com.sean.agenthub</groupId>
    <artifactId>agent-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

如果业务系统需要使用 HTTP 模型适配能力，也需要确保 `agent-model-provider-http` 在依赖树中可用。当前 starter 已直接依赖该模块。

## 3. 基础配置

默认配置使用 `echo` 模型协议，适合本地冒烟验证：

```yaml
agent:
  enabled: true
  tools:
    allowed-names:
      - query_budget_balance
  model:
    protocol: echo
```

接入 OpenAI-compatible 模型：

```yaml
agent:
  enabled: true
  model:
    protocol: openai
    base-url: https://api.deepseek.com/chat/completions
    api-key: ${AGENTHUB_DEEPSEEK_API_KEY}
    model: ${AGENTHUB_MODEL_DS_FAST:deepseek-v4-flash}
    connect-timeout-ms: 10000
    read-timeout-ms: 120000
```

接入 Anthropic-compatible 模型：

```yaml
agent:
  enabled: true
  model:
    protocol: anthropic
    base-url: http://127.0.0.1:4001/v1/messages
    api-key: ${AGENTHUB_ANTHROPIC_API_KEY}
    model: local-model
    connect-timeout-ms: 10000
    read-timeout-ms: 120000
```

本地无鉴权 HTTP 模型网关可以关闭 API key 必填校验：

```yaml
agent:
  model:
    api-key-required: false
```

## 4. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `agent.enabled` | `true` | 是否启用 AgentHub 自动配置 |
| `agent.tools.allowed-names` | 空列表 | Tool 自动注册白名单；空列表表示注册全部 `AgentTool` Bean，配置后只注册名单内 Tool |
| `agent.model.protocol` | `echo` | 模型协议，支持 `echo`、`openai`、`anthropic` |
| `agent.model.base-url` | 空 | HTTP 模型接口地址；`openai` / `anthropic` 必填 |
| `agent.model.api-key` | 空 | HTTP 模型 API key；默认必填 |
| `agent.model.api-key-required` | `true` | 是否校验 API key 非空 |
| `agent.model.model` | 空 | 模型名；`openai` / `anthropic` 必填 |
| `agent.model.connect-timeout-ms` | `10000` | HTTP 连接超时，必须大于 0 |
| `agent.model.read-timeout-ms` | `60000` | HTTP 读取超时，必须大于 0 |

当 `agent.model.protocol=openai` 或 `agent.model.protocol=anthropic` 时，starter 会在启动期执行 fail-fast 校验。未知 protocol 也会在启动期失败。

当 `agent.tools.allowed-names` 中出现不存在的 Tool 名称时，starter 会在启动期失败，避免配置拼写错误导致业务误以为 Tool 已开放。

## 5. Tool Bean 编写规范

业务 Tool 实现 `AgentTool` 并注册为 Spring Bean：

```java
@Component
public class QueryUserInfoTool implements AgentTool {
    @Override
    public String name() {
        return "query_user_info";
    }

    @Override
    public String description() {
        return "查询用户基础信息。仅当用户明确要求查询用户信息、用户详情时调用，不要在闲聊或介绍类问题中使用";
    }

    @Override
    public ToolSchema schema() {
        ToolSchema schema = new ToolSchema();
        Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
        properties.put("userId", new ToolSchemaProperty("string", "用户 ID"));
        schema.setProperties(properties);
        schema.setRequired(Arrays.asList("userId"));
        return schema;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        Object userId = context.getArguments().get("userId");
        return ToolResult.success("{userId=" + userId + ", name=测试用户}");
    }
}
```

规范：

```text
name 使用稳定、唯一、可读的 snake_case
description 写清楚触发条件，也写清楚不应触发的场景
schema 使用 JSON Schema object 子集，明确 properties 和 required
riskLevel 第一版必须返回 ToolRiskLevel.READ
execute 内部复用业务系统原有查询逻辑
ToolResult 返回前完成脱敏，不返回密码、token、证件号等敏感字段
失败时返回 ToolResult.error("可读错误原因")
```

## 6. 权限接入

starter 默认提供 `NoopPermissionEngine`，业务系统应声明自己的 `PermissionEngine` Bean 覆盖默认实现：

```java
@Bean
public PermissionEngine permissionEngine() {
    return new PermissionEngine() {
        @Override
        public PermissionResult check(UserContext user, AgentTool tool, ToolContext context) {
            if (!"query_user_info".equals(tool.name())) {
                return PermissionResult.denied("Unsupported tool");
            }
            return PermissionResult.allowed();
        }
    };
}
```

权限实现建议：

```text
先校验用户是否登录和 userId 是否可信
再校验 Tool 级权限
最后校验数据级权限，例如只能查自己部门或授权范围内的数据
权限拒绝时返回明确 reason，便于模型总结和审计定位
```

## 7. 审计接入

starter 默认提供 `ConsoleAuditService`。业务系统可以声明 `AuditService` Bean 写入日志平台或数据库：

```java
@Bean
public AuditService auditService() {
    return new AuditService() {
        @Override
        public void record(AuditEvent event) {
            // 写入业务审计日志、数据库或日志平台。
        }
    };
}
```

当前 `AuditEvent` 字段：

```text
traceId
sessionId
userId
toolName
requestSummary
toolResultSummary
latencyMs
success
errorMessage
createdAt
```

审计记录建议：

```text
requestSummary 不记录完整敏感入参
toolResultSummary 只记录摘要或脱敏字段
errorMessage 可记录可读错误，不记录密钥、token 或原始异常堆栈
traceId / sessionId / userId 用于串联业务日志和 Agent 调用链路
```

## 8. 接口调用

非流式接口：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"s001","userId":"u001","message":"请介绍一下 AgentHub"}'
```

ToolCall 场景：

```bash
curl -sS -X POST http://127.0.0.1:8080/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"tool-001","userId":"u001","message":"帮我查询用户信息"}'
```

流式接口：

```bash
curl -sS -N -X POST http://127.0.0.1:8080/agent/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"stream-001","userId":"u001","message":"你好"}'
```

SSE 事件类型：

```text
delta     模型文本增量
tool      Tool 调用结果
error     流式调用错误
complete  流式调用完成
```

## 9. 第一版不支持项

```text
写操作 Tool
人工审批流
动态 Tool 注册
Gateway Server
Admin UI
完整 MCP Server / Client
Resources / Prompts 完整 MCP 协议面
数据库持久化
多租户模型路由
完整 JSON Schema Structured Output 校验框架
RAG
Workflow
多 Agent 协作
```
