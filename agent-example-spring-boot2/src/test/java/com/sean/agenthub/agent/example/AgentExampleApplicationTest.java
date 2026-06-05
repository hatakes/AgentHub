package com.sean.agenthub.agent.example;

import com.sean.agenthub.agent.core.model.AuditEvent;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 示例应用集成测试。
 *
 * @author Sean
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AgentExampleApplicationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ExampleAuditService auditService;

    @Before
    public void setUp() {
        auditService.clear();
    }

    @Test
    public void shouldCallChatApiAndExecuteUserInfoTool() {
        AgentRequest request = new AgentRequest();
        request.setSessionId("s001");
        request.setUserId("u001");
        request.setMessage("帮我查询用户信息");

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isOk());
        Assert.assertTrue(response.getAnswer().contains("测试用户"));
        Assert.assertEquals(1, response.getToolCalls().size());
        Assert.assertEquals("query_user_info_mock", response.getToolCalls().get(0).getTool());
        Assert.assertTrue(response.getToolCalls().get(0).isSuccess());
    }

    @Test
    public void shouldNotTriggerToolForNormalChat() {
        AgentRequest request = new AgentRequest();
        request.setSessionId("s-chat");
        request.setUserId("u001");
        request.setMessage("请介绍一下 AgentHub");

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isOk());
        Assert.assertTrue(response.getAnswer().contains("示例模型收到"));
        Assert.assertTrue(response.getToolCalls().isEmpty());
        Assert.assertTrue(auditService.getEvents().isEmpty());
    }

    @Test
    public void shouldExecuteBudgetBalanceToolWithPermissionAndAudit() {
        AgentRequest request = new AgentRequest();
        request.setSessionId("s-budget");
        request.setUserId("finance-admin");
        request.setMessage("帮我查询预算余额");

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isOk());
        Assert.assertTrue(response.getAnswer().contains("128000.00"));
        Assert.assertFalse(response.getAnswer().contains("token"));
        Assert.assertEquals(1, response.getToolCalls().size());
        Assert.assertEquals("query_budget_balance", response.getToolCalls().get(0).getTool());
        Assert.assertTrue(response.getToolCalls().get(0).isSuccess());

        List<AuditEvent> events = auditService.getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("s-budget", events.get(0).getSessionId());
        Assert.assertEquals("finance-admin", events.get(0).getUserId());
        Assert.assertEquals("query_budget_balance", events.get(0).getToolName());
        Assert.assertTrue(events.get(0).isSuccess());
        Assert.assertTrue(events.get(0).getToolResultSummary().contains("128000.00"));
        Assert.assertFalse(events.get(0).getToolResultSummary().contains("token"));
    }

    @Test
    public void shouldDenyBudgetBalanceToolWithoutPermission() {
        AgentRequest request = new AgentRequest();
        request.setSessionId("s-budget-denied");
        request.setUserId("u001");
        request.setMessage("帮我查询预算余额");

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("Tool permission denied"));

        List<AuditEvent> events = auditService.getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("query_budget_balance", events.get(0).getToolName());
        Assert.assertFalse(events.get(0).isSuccess());
        Assert.assertTrue(events.get(0).getErrorMessage().contains("Only finance-admin"));
    }

    @Test
    public void shouldFailWhenBudgetCodeIsMissing() {
        AgentRequest request = new AgentRequest();
        request.setSessionId("s-budget-missing");
        request.setUserId("finance-admin");
        request.setMessage("预算缺参数");

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("Missing required tool argument: budgetCode"));

        List<AuditEvent> events = auditService.getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertFalse(events.get(0).isSuccess());
        Assert.assertTrue(events.get(0).getErrorMessage().contains("Missing required tool argument"));
    }

    @Test
    public void shouldRecordAuditWhenBudgetServiceFails() {
        AgentRequest request = new AgentRequest();
        request.setSessionId("s-budget-fail");
        request.setUserId("finance-admin");
        request.setMessage("预算失败");

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("Budget service unavailable"));

        List<AuditEvent> events = auditService.getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("query_budget_balance", events.get(0).getToolName());
        Assert.assertFalse(events.get(0).isSuccess());
        Assert.assertTrue(events.get(0).getErrorMessage().contains("Budget service unavailable"));
    }
}
