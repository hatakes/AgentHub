package com.sean.agenthub.agent.test;

import com.sean.agenthub.agent.core.api.ModelProvider;
import com.sean.agenthub.agent.core.model.ModelRequest;
import com.sean.agenthub.agent.core.capability.ModelProviderCapability;
import com.sean.agenthub.agent.core.model.ModelResponse;
import com.sean.agenthub.agent.core.model.ToolCall;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;

/**
 * ModelProvider 适配器契约断言。
 *
 * <p>后续 Spring AI、LangChain4j、Hutool AI adapter Spike 应复用这里的能力矩阵断言。</p>
 *
 * @author Sean
 */
public final class ModelProviderContract {
    private ModelProviderContract() {
    }

    public static void assertCapabilities(ModelProvider provider,
                                          ModelProviderCapability[] expected,
                                          ModelProviderCapability[] unexpected) {
        assertContainsAll(provider, expected);
        assertContainsNone(provider, unexpected);
    }

    public static void assertContainsAll(ModelProvider provider, ModelProviderCapability... expected) {
        Set<ModelProviderCapability> capabilities = provider.capabilities();
        for (ModelProviderCapability capability : expected) {
            Assert.assertTrue("Missing capability: " + capability, capabilities.contains(capability));
        }
    }

    public static void assertContainsNone(ModelProvider provider, ModelProviderCapability... unexpected) {
        Set<ModelProviderCapability> capabilities = provider.capabilities();
        for (ModelProviderCapability capability : unexpected) {
            Assert.assertFalse("Unexpected capability: " + capability, capabilities.contains(capability));
        }
    }

    public static Set<ModelProviderCapability> capabilities(ModelProviderCapability... capabilities) {
        return new HashSet<ModelProviderCapability>(Arrays.asList(capabilities));
    }

    public static void assertTextChat(ModelProvider provider, ModelRequest request, String expectedAnswer) {
        ModelResponse response = provider.chat(request);

        Assert.assertFalse("Expected text response, but got tool calls", response.hasToolCalls());
        Assert.assertEquals(expectedAnswer, response.getAnswer());
    }

    public static void assertToolCall(ModelProvider provider,
                                      ModelRequest request,
                                      String expectedId,
                                      String expectedName,
                                      String argumentKey,
                                      Object expectedArgumentValue) {
        ModelResponse response = provider.chat(request);

        Assert.assertTrue("Expected tool call response", response.hasToolCalls());
        Assert.assertEquals(1, response.getToolCalls().size());
        ToolCall toolCall = response.getToolCalls().get(0);
        Assert.assertEquals(expectedId, toolCall.getId());
        Assert.assertEquals(expectedName, toolCall.getName());
        Assert.assertEquals(expectedArgumentValue, toolCall.getArguments().get(argumentKey));
    }

    public static RecordingModelStreamListener assertStreamText(ModelProvider provider,
                                                                ModelRequest request,
                                                                String expectedAnswer) {
        RecordingModelStreamListener listener = new RecordingModelStreamListener();
        provider.streamChat(request, listener);

        Assert.assertEquals(expectedAnswer, listener.getAnswer());
        Assert.assertTrue("Expected stream completion", listener.isCompleted());
        Assert.assertNull("Unexpected stream error", listener.getError());
        return listener;
    }

    public static RecordingModelStreamListener assertStreamToolCall(ModelProvider provider,
                                                                    ModelRequest request,
                                                                    String expectedId,
                                                                    String expectedName,
                                                                    String argumentKey,
                                                                    Object expectedArgumentValue) {
        RecordingModelStreamListener listener = new RecordingModelStreamListener();
        provider.streamChat(request, listener);

        Assert.assertTrue("Expected stream completion", listener.isCompleted());
        Assert.assertNull("Unexpected stream error", listener.getError());
        Assert.assertEquals(1, listener.getToolCalls().size());
        ToolCall toolCall = listener.getToolCalls().get(0);
        Assert.assertEquals(expectedId, toolCall.getId());
        Assert.assertEquals(expectedName, toolCall.getName());
        Assert.assertEquals(expectedArgumentValue, toolCall.getArguments().get(argumentKey));
        return listener;
    }
}
