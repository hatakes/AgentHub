# agent-model-provider-langchain4j

LangChain4j adapter Spike for AgentHub `ModelProvider`.

This module is Java 17+ only. The root `adapters-java17` profile activates automatically on JDK 17+, so Maven and IDE imports should include it when the project JDK is 17 or newer.

```bash
mvn test
```

Current scope:

```text
LangChain4j ChatModel -> AgentHub ModelProvider
LangChain4j StreamingChatModel -> AgentHub streamChat
AgentHub ToolSchema -> LangChain4j ToolSpecification request mapping
LangChain4j ToolExecutionRequest -> AgentHub ToolCall response mapping
TEXT_CHAT + TEXT_STREAM capabilities
systemPrompt + basic message history mapping
```

Not covered yet:

```text
Full TOOL_CALL capability declaration
Tool result messages
structured output
Spring Boot LangChain4j starter integration
```
