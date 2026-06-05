package com.sean.agenthub.agent.starter;

import com.sean.agenthub.agent.core.api.AgentTool;
import com.sean.agenthub.agent.core.api.AuditService;
import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.api.PermissionEngine;
import com.sean.agenthub.agent.core.api.AgentMemory;
import com.sean.agenthub.agent.core.api.AgentRuntime;
import com.sean.agenthub.agent.core.api.AgentService;
import com.sean.agenthub.agent.core.api.ToolRegistry;
import com.sean.agenthub.agent.core.audit.ConsoleAuditService;
import com.sean.agenthub.agent.core.memory.InMemoryAgentMemory;
import com.sean.agenthub.agent.core.permission.NoopPermissionEngine;
import com.sean.agenthub.agent.core.provider.EchoModelProvider;
import com.sean.agenthub.agent.core.runtime.DefaultAgentRuntime;
import com.sean.agenthub.agent.core.runtime.DefaultAgentService;
import com.sean.agenthub.agent.core.tool.InMemoryToolRegistry;
import com.sean.agenthub.agent.provider.http.AnthropicCompatibleModelProvider;
import com.sean.agenthub.agent.provider.http.HttpModelProviderProperties;
import com.sean.agenthub.agent.provider.http.OpenAiCompatibleModelProvider;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为 Spring Boot 应用装配 AgentHub 默认组件。
 *
 * <p>业务系统只要声明同类型 Bean，就可以覆盖这里的默认实现。</p>
 *
 * @author Sean
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry agentToolRegistry(List<AgentTool> tools, AgentProperties properties) {
        // 收集宿主 Spring 容器中的 AgentTool Bean，并注册到核心 ToolRegistry。
        InMemoryToolRegistry registry = new InMemoryToolRegistry();
        Set<String> allowedNames = normalizeToolNames(properties.getTools().getAllowedNames());
        Set<String> registeredNames = new LinkedHashSet<String>();
        for (AgentTool tool : tools) {
            if (!allowedNames.isEmpty() && !allowedNames.contains(tool.name())) {
                continue;
            }
            registry.register(tool);
            registeredNames.add(tool.name());
        }
        validateAllowedToolNames(allowedNames, registeredNames);
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentMemory agentMemory() {
        return new InMemoryAgentMemory();
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionEngine permissionEngine() {
        return new NoopPermissionEngine();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditService auditService() {
        return new ConsoleAuditService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelProvider modelProvider(AgentProperties properties) {
        AgentProperties.Model model = properties.getModel();
        String protocol = normalizeProtocol(model.getProtocol());
        if ("openai".equals(protocol)) {
            validateHttpModelProperties(model);
            return new OpenAiCompatibleModelProvider(toHttpProperties(model));
        }
        if ("anthropic".equals(protocol)) {
            validateHttpModelProperties(model);
            return new AnthropicCompatibleModelProvider(toHttpProperties(model));
        }
        if (!"echo".equals(protocol)) {
            throw new IllegalStateException("Unsupported agent.model.protocol: " + model.getProtocol()
                    + ". Supported values: echo, openai, anthropic.");
        }
        return new EchoModelProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentRuntime agentRuntime(ModelProvider modelProvider,
                                     ToolRegistry toolRegistry,
                                     AgentMemory agentMemory,
                                     PermissionEngine permissionEngine,
                                     AuditService auditService) {
        return new DefaultAgentRuntime(modelProvider, toolRegistry, agentMemory, permissionEngine, auditService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentService agentService(AgentRuntime agentRuntime) {
        return new DefaultAgentService(agentRuntime);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentChatController agentChatController(AgentService agentService) {
        return new AgentChatController(agentService);
    }

    private HttpModelProviderProperties toHttpProperties(AgentProperties.Model model) {
        HttpModelProviderProperties properties = new HttpModelProviderProperties();
        properties.setBaseUrl(model.getBaseUrl());
        properties.setApiKey(model.getApiKey());
        properties.setModel(model.getModel());
        properties.setConnectTimeoutMs(model.getConnectTimeoutMs());
        properties.setReadTimeoutMs(model.getReadTimeoutMs());
        return properties;
    }

    private String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.trim().isEmpty()) {
            return "echo";
        }
        return protocol.trim().toLowerCase();
    }

    private Set<String> normalizeToolNames(List<String> names) {
        Set<String> result = new LinkedHashSet<String>();
        if (names == null) {
            return result;
        }
        for (String name : names) {
            if (name != null && !name.trim().isEmpty()) {
                result.add(name.trim());
            }
        }
        return result;
    }

    private void validateAllowedToolNames(Set<String> allowedNames, Set<String> registeredNames) {
        if (allowedNames.isEmpty()) {
            return;
        }
        Set<String> unknownNames = new LinkedHashSet<String>(allowedNames);
        unknownNames.removeAll(registeredNames);
        if (!unknownNames.isEmpty()) {
            throw new IllegalStateException("agent.tools.allowed-names contains unknown tools: " + unknownNames);
        }
    }

    private void validateHttpModelProperties(AgentProperties.Model model) {
        requireText(model.getBaseUrl(), "agent.model.base-url");
        requireText(model.getModel(), "agent.model.model");
        if (model.isApiKeyRequired()) {
            requireText(model.getApiKey(), "agent.model.api-key");
        }
        if (model.getConnectTimeoutMs() <= 0) {
            throw new IllegalStateException("agent.model.connect-timeout-ms must be greater than 0.");
        }
        if (model.getReadTimeoutMs() <= 0) {
            throw new IllegalStateException("agent.model.read-timeout-ms must be greater than 0.");
        }
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(propertyName + " is required when agent.model.protocol is openai or anthropic.");
        }
    }
}
