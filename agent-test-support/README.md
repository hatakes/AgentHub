# agent-test-support

`agent-test-support` 提供 AgentHub adapter Spike 的测试支撑代码。

当前重点是 `ModelProviderContract`，用于统一断言不同 `ModelProvider` 实现的能力矩阵。

已提供的通用断言：

```text
capabilities 能力矩阵
text chat 文本响应
stream text 流式文本响应
tool call ToolCall 响应
stream tool call 流式 ToolCall 响应
```

后续新增以下模块时应复用该契约：

```text
agent-model-provider-spring-ai
agent-model-provider-langchain4j
agent-model-provider-hutool-ai
agent-mcp-adapter
```

该模块只用于测试，不承载生产运行逻辑。
