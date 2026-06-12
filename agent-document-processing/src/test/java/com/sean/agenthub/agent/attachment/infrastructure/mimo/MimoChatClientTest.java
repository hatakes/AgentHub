package com.sean.agenthub.agent.attachment.infrastructure.mimo;

import org.junit.Assert;
import org.junit.Test;

/**
 * MiMo chat client tests.
 *
 * @author Sean
 */
public class MimoChatClientTest {
    private final MimoChatClient client = new MimoChatClient();

    @Test
    public void shouldNormalizeEndpointFromBaseUrl() {
        Assert.assertEquals("https://api.xiaomimimo.com/v1/chat/completions",
                client.endpoint("https://api.xiaomimimo.com/v1"));
        Assert.assertEquals("https://api.xiaomimimo.com/v1/chat/completions",
                client.endpoint("https://api.xiaomimimo.com/v1/"));
        Assert.assertEquals("https://host/v1/chat/completions",
                client.endpoint("https://host"));
        Assert.assertEquals("https://host/v1/chat/completions",
                client.endpoint("https://host/v1/chat/completions"));
    }

    @Test
    public void shouldRejectBlankConfigValue() {
        try {
            client.requireText(" ", "attachment.outline.mimo.api-key");
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("attachment.outline.mimo.api-key"));
        }
    }
}
