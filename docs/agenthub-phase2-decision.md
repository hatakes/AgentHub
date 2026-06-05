# AgentHub 二阶段方向选择

## 1. 背景

第一版 MVP 当前已完成核心闭环、Starter 接入说明、MVP 验收清单、示例业务只读 Tool 试点和 MCP Adapter 最小映射。

二阶段不应默认同时启动 Gateway、完整 MCP Server、Admin UI、数据库持久化和多租户模型路由。下一阶段应由真实业务系统接入结果驱动。

## 2. 决策输入

进入二阶段前至少完成一次真实业务系统接入验收，并沉淀：

```text
docs/agenthub-business-acceptance-record.md
真实业务 Tool Schema
权限接入方式
审计落点
脱敏策略
失败场景记录
模型误触发记录
```

## 3. 方向 A：继续强化 SDK / Starter

选择条件：

```text
只有 1-2 个业务系统接入
业务系统希望复用本系统权限和数据库访问
Tool 数量少，主要是只读查询
暂不需要统一入口、统一审计和统一限流
```

建议建设：

```text
Starter 配置增强
业务接入样板工程
Tool Schema 辅助工具
审计字段扩展
错误码规范
更多只读 Tool 验收用例
```

暂不建设：

```text
Gateway Server
Admin UI
完整 MCP Server
多租户模型路由
```

## 4. 方向 B：Gateway Server

选择条件：

```text
有 3 个以上业务系统要统一接入
需要统一模型出口
需要统一鉴权、限流和审计
业务系统不希望每个服务都嵌入模型调用配置
需要集中管理 Tool Schema
```

建议最小范围：

```text
统一 /agent/chat 和 /agent/chat/stream 入口
应用级鉴权
Tool 注册和白名单
统一 AuditService
统一模型配置
限流和超时保护
```

仍不建议第一步建设：

```text
复杂多租户计费
Admin UI 全量功能
完整 MCP Server
写操作 Tool 和人工审批
```

## 5. 方向 C：MCP SDK Adapter

选择条件：

```text
需要对外暴露 AgentHub Tool 给外部 Agent / IDE / MCP Client
已有真实 MCP Client 消费方
已有明确的 tools/list 和 tools/call 调用方
能接受引入 MCP Java SDK 或 Spring AI MCP 的 JDK / Spring 版本要求
```

建议最小范围：

```text
复用 agent-mcp-adapter 的 DTO 和调用边界
在新模块中接入 MCP Java SDK 或 Spring AI MCP
只暴露 tools/list 和 tools/call
继续复用 PermissionEngine 和 AuditService
保留 Tool 白名单
```

暂不建设：

```text
Resources / Prompts 完整协议面
STDIO 本地命令能力
Gateway 多租户 MCP 暴露
写操作 Tool
```

## 6. 方向 D：Admin UI

选择条件：

```text
Tool 数量明显增长
非研发人员需要查看 Tool 和调用记录
需要人工配置 Tool 白名单、风险等级和启停状态
需要查询审计日志和失败原因
```

建议最小范围：

```text
Tool 列表只读展示
调用日志查询
模型配置只读展示
启停状态展示
失败记录查看
```

暂不建设：

```text
在线编辑 Tool 代码
动态注册任意远程 Tool
写操作审批流
复杂权限后台
```

## 7. 推荐优先级

在没有真实业务接入结论前，默认优先级：

```text
1. 真实业务系统接入验收
2. SDK / Starter 小幅增强
3. Gateway Server 最小入口
4. MCP SDK Adapter
5. Admin UI
```

如果真实业务接入证明单系统 SDK 模式足够，继续强化 Starter。如果出现多个系统统一接入诉求，再进入 Gateway。如果先出现外部 MCP Client 消费诉求，再进入 MCP SDK Adapter。

## 8. 决策记录模板

```text
决策日期：
参与人：
业务系统数量：
真实接入是否通过：
主要阻塞：
选择方向：SDK / Gateway / MCP SDK Adapter / Admin UI
本阶段明确不做：
下一阶段交付物：
验收标准：
```

