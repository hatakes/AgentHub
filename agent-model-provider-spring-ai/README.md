# agent-model-provider-spring-ai

Spring AI adapter Spike for AgentHub `ModelProvider`.

This module is Java 17+ only and is not part of the default Java 8 reactor. Build it with:

```bash
mvn -Padapters-java17 test
```

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
