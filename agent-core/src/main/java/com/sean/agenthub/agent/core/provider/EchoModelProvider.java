package com.sean.agenthub.agent.core.provider;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * 默认回显模型实现。
 *
 * <p>没有配置真实模型时用于保证框架可启动；示例工程会提供自己的规则型 ModelProvider。</p>
 *
 * @author Sean
 */
public class EchoModelProvider implements ModelProvider {
    @Override
    public java.util.Set<ModelProviderCapability> capabilities() {
        return java.util.EnumSet.of(
                ModelProviderCapability.TEXT_CHAT,
                ModelProviderCapability.TEXT_STREAM
        );
    }

    @Override
    public ModelResponse chat(ModelRequest request) {
        if (request.getLastToolResult() != null) {
            return ModelResponse.answer("Tool result: " + request.getLastToolResult().getData());
        }
        return ModelResponse.answer(request.getUserMessage());
    }
}
