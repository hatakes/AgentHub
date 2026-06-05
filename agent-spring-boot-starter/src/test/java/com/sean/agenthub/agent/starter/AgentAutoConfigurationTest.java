package com.sean.agenthub.agent.starter;

import com.sean.agenthub.agent.core.api.AgentMemory;
import com.sean.agenthub.agent.core.api.AgentService;
import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.AuditService;
import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.api.PermissionEngine;
import com.sean.agenthub.agent.core.api.ToolRegistry;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.provider.EchoModelProvider;
import com.sean.agenthub.agent.core.tool.ToolContext;
import com.sean.agenthub.agent.core.tool.ToolResult;
import com.sean.agenthub.agent.core.tool.ToolRiskLevel;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.provider.http.AnthropicCompatibleModelProvider;
import com.sean.agenthub.agent.provider.http.OpenAiCompatibleModelProvider;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentAutoConfiguration 自动配置测试。
 *
 * @author Sean
 */
public class AgentAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentAutoConfiguration.class));

    @Test
    public void shouldCreateDefaultAgentBeans() {
        contextRunner.run(context -> {
            Assert.assertTrue(context.containsBean("agentService"));
            Assert.assertTrue(context.containsBean("agentRuntime"));
            Assert.assertTrue(context.containsBean("agentToolRegistry"));
            Assert.assertTrue(context.containsBean("agentMemory"));
            Assert.assertTrue(context.containsBean("permissionEngine"));
            Assert.assertTrue(context.containsBean("auditService"));
            Assert.assertTrue(context.containsBean("modelProvider"));
            Assert.assertTrue(context.containsBean("agentChatController"));

            Assert.assertNotNull(context.getBean(AgentService.class));
            Assert.assertNotNull(context.getBean(ToolRegistry.class));
            Assert.assertNotNull(context.getBean(AgentMemory.class));
            Assert.assertNotNull(context.getBean(PermissionEngine.class));
            Assert.assertNotNull(context.getBean(AuditService.class));
            Assert.assertTrue(context.getBean(ModelProvider.class) instanceof EchoModelProvider);
        });
    }

    @Test
    public void shouldRegisterAgentToolBeansIntoRegistry() {
        contextRunner.withUserConfiguration(TestToolConfiguration.class).run(context -> {
            ToolRegistry registry = context.getBean(ToolRegistry.class);

            Assert.assertEquals(1, registry.list().size());
            Assert.assertTrue(registry.get("test_tool").isPresent());
        });
    }

    @Test
    public void shouldRegisterOnlyAllowedAgentTools() {
        contextRunner.withUserConfiguration(TwoToolConfiguration.class)
                .withPropertyValues("agent.tools.allowed-names[0]=first_tool")
                .run(context -> {
                    ToolRegistry registry = context.getBean(ToolRegistry.class);

                    Assert.assertEquals(1, registry.list().size());
                    Assert.assertTrue(registry.get("first_tool").isPresent());
                    Assert.assertFalse(registry.get("second_tool").isPresent());
                });
    }

    @Test
    public void shouldFailFastWhenAllowedToolNameUnknown() {
        contextRunner.withUserConfiguration(TwoToolConfiguration.class)
                .withPropertyValues("agent.tools.allowed-names[0]=missing_tool")
                .run(context -> {
                    Assert.assertNotNull(context.getStartupFailure());
                    Assert.assertTrue(rootCause(context.getStartupFailure()).getMessage()
                            .contains("agent.tools.allowed-names contains unknown tools"));
                });
    }

    @Test
    public void shouldBackOffWhenCustomModelProviderExists() {
        contextRunner.withUserConfiguration(CustomModelProviderConfiguration.class).run(context -> {
            ModelProvider modelProvider = context.getBean(ModelProvider.class);

            Assert.assertTrue(modelProvider instanceof CustomModelProvider);
            Assert.assertEquals("custom", modelProvider.chat(new ModelRequest()).getAnswer());
        });
    }

    @Test
    public void shouldCreateOpenAiCompatibleModelProvider() {
        contextRunner.withPropertyValues(
                "agent.model.protocol=openai",
                "agent.model.base-url=http://127.0.0.1:4000",
                "agent.model.api-key=local-key",
                "agent.model.model=local-model"
        ).run(context -> {
            Assert.assertTrue(context.getBean(ModelProvider.class) instanceof OpenAiCompatibleModelProvider);
        });
    }

    @Test
    public void shouldCreateAnthropicCompatibleModelProvider() {
        contextRunner.withPropertyValues(
                "agent.model.protocol=anthropic",
                "agent.model.base-url=http://127.0.0.1:4000",
                "agent.model.api-key=local-key",
                "agent.model.model=local-model"
        ).run(context -> {
            Assert.assertTrue(context.getBean(ModelProvider.class) instanceof AnthropicCompatibleModelProvider);
        });
    }

    @Test
    public void shouldFailFastWhenHttpModelBaseUrlMissing() {
        contextRunner.withPropertyValues(
                "agent.model.protocol=openai",
                "agent.model.api-key=local-key",
                "agent.model.model=local-model"
        ).run(context -> {
            Assert.assertNotNull(context.getStartupFailure());
            Assert.assertTrue(rootCause(context.getStartupFailure()).getMessage().contains("agent.model.base-url"));
        });
    }

    @Test
    public void shouldFailFastWhenHttpModelApiKeyMissingByDefault() {
        contextRunner.withPropertyValues(
                "agent.model.protocol=openai",
                "agent.model.base-url=http://127.0.0.1:4000",
                "agent.model.model=local-model"
        ).run(context -> {
            Assert.assertNotNull(context.getStartupFailure());
            Assert.assertTrue(rootCause(context.getStartupFailure()).getMessage().contains("agent.model.api-key"));
        });
    }

    @Test
    public void shouldAllowHttpModelWithoutApiKeyWhenDisabled() {
        contextRunner.withPropertyValues(
                "agent.model.protocol=openai",
                "agent.model.base-url=http://127.0.0.1:4000",
                "agent.model.model=local-model",
                "agent.model.api-key-required=false"
        ).run(context -> {
            Assert.assertNull(context.getStartupFailure());
            Assert.assertTrue(context.getBean(ModelProvider.class) instanceof OpenAiCompatibleModelProvider);
        });
    }

    @Test
    public void shouldFailFastWhenModelProtocolUnsupported() {
        contextRunner.withPropertyValues("agent.model.protocol=unknown").run(context -> {
            Assert.assertNotNull(context.getStartupFailure());
            Assert.assertTrue(rootCause(context.getStartupFailure()).getMessage().contains("Unsupported agent.model.protocol"));
        });
    }

    @Test
    public void shouldDisableAutoConfigurationWhenAgentDisabled() {
        contextRunner.withPropertyValues("agent.enabled=false").run(context -> {
            Assert.assertFalse(context.containsBean("agentService"));
            Assert.assertFalse(context.containsBean("agentToolRegistry"));
            Assert.assertFalse(context.containsBean("agentChatController"));
        });
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Configuration
    static class TestToolConfiguration {
        @Bean
        public AgentTool testTool() {
            return simpleTool("test_tool");
        }
    }

    @Configuration
    static class TwoToolConfiguration {
        @Bean
        public AgentTool firstTool() {
            return simpleTool("first_tool");
        }

        @Bean
        public AgentTool secondTool() {
            return simpleTool("second_tool");
        }
    }

    @Configuration
    static class CustomModelProviderConfiguration {
        @Bean
        public ModelProvider customModelProvider() {
            return new CustomModelProvider();
        }
    }

    static class CustomModelProvider implements ModelProvider {
        @Override
        public ModelResponse chat(ModelRequest request) {
            return ModelResponse.answer("custom");
        }
    }

    private static AgentTool simpleTool(final String name) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return "测试 Tool";
            }

            @Override
            public ToolSchema schema() {
                return new ToolSchema();
            }

            @Override
            public ToolRiskLevel riskLevel() {
                return ToolRiskLevel.READ;
            }

            @Override
            public ToolResult execute(ToolContext context) {
                return ToolResult.success("ok");
            }
        };
    }
}
