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
 * <p>Starter 的职责是把 agent-core 的纯 Java 抽象接到 Spring 容器里。业务工程通常只需要引入 starter，
 * 再声明自己的 AgentTool、ModelProvider、PermissionEngine 或 AuditService Bean，就能替换默认行为。
 * 这里所有默认 Bean 都使用 {@code @ConditionalOnMissingBean}，避免 starter 抢占业务侧显式配置。</p>
 *
 * <p>默认模型 provider 用配置项选择协议：echo 用于本地演示和测试，openai / anthropic 用于接入兼容 HTTP
 * 协议的真实模型服务。</p>
 *
 * @author Sean
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentAutoConfiguration {

    /**
     * 创建 Tool 注册中心，收集容器中的 AgentTool Bean 并按白名单过滤。
     *
     * @param tools      容器中所有 AgentTool Bean
     * @param properties AgentHub 配置
     * @return Tool 注册中心
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry agentToolRegistry(List<AgentTool> tools, AgentProperties properties) {
        // 收集宿主 Spring 容器中的 AgentTool Bean，并注册到核心 ToolRegistry。
        // allowed-names 是部署侧的工具白名单：即使某个 Tool Bean 已经存在，也只有在白名单内才暴露给模型。
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

    /**
     * 创建内存会话记忆。
     *
     * @return 会话记忆实现
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentMemory agentMemory() {
        return new InMemoryAgentMemory();
    }

    /**
     * 创建默认权限引擎（MVP 阶段不做实际权限校验）。
     *
     * @return 权限引擎
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionEngine permissionEngine() {
        return new NoopPermissionEngine();
    }

    /**
     * 创建控制台审计服务（MVP 阶段输出到标准输出）。
     *
     * @return 审计服务
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditService auditService() {
        return new ConsoleAuditService();
    }

    /**
     * 根据协议配置创建模型供应商。
     *
     * <p>支持 echo（默认）、openai、anthropic 三种协议。</p>
     *
     * @param properties AgentHub 配置
     * @return 模型供应商
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelProvider modelProvider(AgentProperties properties) {
        AgentProperties.Model model = properties.getModel();
        String protocol = normalizeProtocol(model.getProtocol());
        // 这里直接创建 HTTP provider，而不是强制用户声明 Bean，降低最小接入成本。
        // 需要更复杂的鉴权、代理、日志或灰度策略时，业务侧仍然可以声明自己的 ModelProvider 覆盖它。
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

    /**
     * 创建默认 Agent 执行器。
     *
     * @param modelProvider    模型供应商
     * @param toolRegistry     Tool 注册中心
     * @param agentMemory      会话记忆
     * @param permissionEngine 权限引擎
     * @param auditService     审计服务
     * @return Agent 执行器
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentRuntime agentRuntime(ModelProvider modelProvider,
                                     ToolRegistry toolRegistry,
                                     AgentMemory agentMemory,
                                     PermissionEngine permissionEngine,
                                     AuditService auditService) {
        return new DefaultAgentRuntime(modelProvider, toolRegistry, agentMemory, permissionEngine, auditService);
    }

    /**
     * 创建默认 Agent 服务。
     *
     * @param agentRuntime Agent 执行器
     * @return Agent 服务
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentService agentService(AgentRuntime agentRuntime) {
        return new DefaultAgentService(agentRuntime);
    }

    /**
     * 创建 Agent 对话 HTTP 控制器。
     *
     * @param agentService Agent 服务
     * @return HTTP 控制器
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentChatController agentChatController(AgentService agentService) {
        return new AgentChatController(agentService);
    }

    /**
     * 将 Starter 配置转换为 HTTP provider 配置。
     *
     * @param model Starter 模型配置
     * @return HTTP provider 配置
     */
    private HttpModelProviderProperties toHttpProperties(AgentProperties.Model model) {
        HttpModelProviderProperties properties = new HttpModelProviderProperties();
        properties.setBaseUrl(model.getBaseUrl());
        properties.setApiKey(model.getApiKey());
        properties.setModel(model.getModel());
        properties.setConnectTimeoutMs(model.getConnectTimeoutMs());
        properties.setReadTimeoutMs(model.getReadTimeoutMs());
        return properties;
    }

    /**
     * 规范化协议名称，null 或空默认为 "echo"。
     *
     * @param protocol 原始协议名
     * @return 规范化后的协议名
     */
    private String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.trim().isEmpty()) {
            return "echo";
        }
        return protocol.trim().toLowerCase();
    }

    /**
     * 规范化 Tool 名称列表，去除 null 和空白项。
     *
     * @param names 原始名称列表
     * @return 规范化后的名称集合
     */
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

    /**
     * 校验白名单中的 Tool 名称是否都已注册，未注册的直接启动失败。
     *
     * @param allowedNames    白名单
     * @param registeredNames 实际注册的名称
     * @throws IllegalStateException 如果白名单中有未注册的 Tool
     */
    private void validateAllowedToolNames(Set<String> allowedNames, Set<String> registeredNames) {
        if (allowedNames.isEmpty()) {
            return;
        }
        // 白名单写错时启动失败，比运行中模型请求一个永远不存在的 Tool 更容易定位。
        Set<String> unknownNames = new LinkedHashSet<String>(allowedNames);
        unknownNames.removeAll(registeredNames);
        if (!unknownNames.isEmpty()) {
            throw new IllegalStateException("agent.tools.allowed-names contains unknown tools: " + unknownNames);
        }
    }

    /**
     * 校验 HTTP 模型配置的必填项。
     *
     * @param model 模型配置
     * @throws IllegalStateException 如果缺少必填配置
     */
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

    /**
     * 校验配置项非空。
     *
     * @param value        配置值
     * @param propertyName 配置项名称（用于错误提示）
     * @throws IllegalStateException 如果值为空
     */
    private void requireText(String value, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(propertyName + " is required when agent.model.protocol is openai or anthropic.");
        }
    }
}
