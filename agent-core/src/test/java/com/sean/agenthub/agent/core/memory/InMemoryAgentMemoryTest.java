package com.sean.agenthub.agent.core.memory;

import com.sean.agenthub.agent.core.model.AgentMessage;
import org.junit.Assert;
import org.junit.Test;

/**
 * InMemoryAgentMemory 单元测试。
 *
 * @author Sean
 */
public class InMemoryAgentMemoryTest {

    @Test
    public void shouldSaveLoadAndClearMessages() {
        InMemoryAgentMemory memory = new InMemoryAgentMemory();

        memory.save("s001", new AgentMessage("user", "hello"));
        memory.save("s001", new AgentMessage("assistant", "world"));

        Assert.assertEquals(2, memory.load("s001").size());
        Assert.assertEquals("hello", memory.load("s001").get(0).getContent());

        memory.clear("s001");

        Assert.assertTrue(memory.load("s001").isEmpty());
    }

    @Test
    public void shouldUseDefaultSessionWhenSessionIdIsBlank() {
        InMemoryAgentMemory memory = new InMemoryAgentMemory();

        memory.save("", new AgentMessage("user", "hello"));

        Assert.assertEquals(1, memory.load(null).size());
        Assert.assertEquals("hello", memory.load(null).get(0).getContent());
    }
}
