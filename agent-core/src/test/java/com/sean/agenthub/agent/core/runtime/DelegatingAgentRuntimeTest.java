package com.sean.agenthub.agent.core.runtime;

import com.sean.agenthub.agent.core.api.AgentRuntime;
import com.sean.agenthub.agent.core.api.AgentStreamListener;
import com.sean.agenthub.agent.core.model.AgentContext;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;
import com.sean.agenthub.agent.core.model.ToolCallResult;
import com.sean.agenthub.agent.core.model.UserContext;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;

/**
 * DelegatingAgentRuntime 单元测试。
 *
 * @author Sean
 */
public class DelegatingAgentRuntimeTest {
    @Test
    public void shouldDelegateRun() {
        RecordingRuntime delegate = new RecordingRuntime();
        DelegatingAgentRuntime runtime = new DelegatingAgentRuntime(delegate);
        AgentRequest request = request();
        AgentContext context = context();

        AgentResponse response = runtime.run(request, context);

        Assert.assertTrue(response.isOk());
        Assert.assertEquals("delegated", response.getAnswer());
        Assert.assertSame(request, delegate.runRequest);
        Assert.assertSame(context, delegate.runContext);
    }

    @Test
    public void shouldDelegateRunStream() {
        RecordingRuntime delegate = new RecordingRuntime();
        DelegatingAgentRuntime runtime = new DelegatingAgentRuntime(delegate);
        RecordingStreamListener listener = new RecordingStreamListener();
        AgentRequest request = request();
        AgentContext context = context();

        runtime.runStream(request, context, listener);

        Assert.assertEquals("streamed", listener.answer.toString());
        Assert.assertTrue(listener.completed);
        Assert.assertSame(request, delegate.streamRequest);
        Assert.assertSame(context, delegate.streamContext);
    }

    @Test
    public void shouldRejectNullDelegate() {
        try {
            new DelegatingAgentRuntime(null);
            Assert.fail("expected exception");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("delegate"));
        }
    }

    private AgentRequest request() {
        AgentRequest request = new AgentRequest();
        request.setSessionId("s001");
        request.setUserId("u001");
        request.setMessage("hello");
        return request;
    }

    private AgentContext context() {
        return new AgentContext(new UserContext("u001"));
    }

    private static class RecordingRuntime implements AgentRuntime {
        private AgentRequest runRequest;
        private AgentContext runContext;
        private AgentRequest streamRequest;
        private AgentContext streamContext;

        @Override
        public AgentResponse run(AgentRequest request, AgentContext context) {
            this.runRequest = request;
            this.runContext = context;
            return AgentResponse.ok("delegated", new ArrayList<ToolCallResult>());
        }

        @Override
        public void runStream(AgentRequest request, AgentContext context, AgentStreamListener listener) {
            this.streamRequest = request;
            this.streamContext = context;
            listener.onDelta("streamed");
            listener.onComplete();
        }
    }

    private static class RecordingStreamListener implements AgentStreamListener {
        private final StringBuilder answer = new StringBuilder();
        private boolean completed;

        @Override
        public void onDelta(String delta) {
            answer.append(delta);
        }

        @Override
        public void onToolCall(ToolCallResult result) {
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        @Override
        public void onError(String error) {
        }
    }
}
