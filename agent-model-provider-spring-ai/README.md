# agent-model-provider-spring-ai

Spring AI adapter Spike for AgentHub `ModelProvider`.

This module is Java 17+ only. The root `adapters-java17` profile activates automatically on JDK 17+, so Maven and IDE imports should include it when the project JDK is 17 or newer.

```bash
mvn test
```

Default dependency line uses Spring AI `1.1.x`. The adapter is also verified with Spring AI `2.0.0-RC2`:

```bash
mvn -pl agent-model-provider-spring-ai -am test -Pspring-ai-2.0-rc
```

Spring AI `2.0.0-RC2` removed `ToolCallingChatOptions.internalToolExecutionEnabled`. The adapter probes that option at runtime: on 1.1.x it disables Spring AI internal tool execution explicitly; on 2.0 RC it skips the removed option and still lets AgentHub execute returned tool calls after permission and audit checks.

Current scope:

```text
Spring AI ChatModel -> AgentHub ModelProvider
Spring AI ChatModel stream -> AgentHub streamChat
AgentHub ToolSchema -> Spring AI ToolCallback request mapping with internal execution disabled
Spring AI AssistantMessage.ToolCall -> AgentHub ToolCall response mapping
TEXT_CHAT + TEXT_STREAM capabilities
systemPrompt + basic message history mapping
```

Not covered yet:

```text
Full TOOL_CALL capability declaration
Tool result messages
structured output
Spring Boot 3 starter auto-configuration
```
