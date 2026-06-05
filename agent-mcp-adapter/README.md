# agent-mcp-adapter

AgentHub Tool 到 MCP Tool 的最小适配模块。

当前边界：

```text
依赖 agent-core
不依赖 MCP SDK
不依赖 agent-gateway-server
不实现完整 MCP Server / Client
不实现 STDIO、本地命令、Resources、Prompts
```

当前能力：

```text
ToolRegistry -> tools/list
AgentTool -> MCP Tool DTO
ToolSchema -> inputSchema
AgentTool.execute -> tools/call
PermissionEngine -> tools/call 前置权限检查
AuditService -> tools/call 审计记录
```

该模块输出的是稳定的 Java DTO 和调用服务，后续 Gateway 或 MCP SDK 层可以在外侧转换为真实 MCP JSON-RPC 协议。
