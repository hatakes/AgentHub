package com.sean.agenthub.agent.core.provider;

import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import org.junit.Assert;
import org.junit.Test;

/**
 * EchoModelProvider 单元测试。
 *
 * @author Sean
 */
public class EchoModelProviderTest {
    @Test
    public void shouldDeclareEchoModelCapabilities() {
        EchoModelProvider provider = new EchoModelProvider();

        Assert.assertTrue(provider.capabilities().contains(ModelProviderCapability.TEXT_CHAT));
        Assert.assertTrue(provider.capabilities().contains(ModelProviderCapability.TEXT_STREAM));
        Assert.assertFalse(provider.capabilities().contains(ModelProviderCapability.TOOL_CALL));
    }
}
