package com.sean.agenthub.agent.attachment.application;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 附件分析配置绑定测试。
 *
 * @author Sean
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("mimo")
public class AttachmentAnalysisPropertiesTest {
    @Autowired
    private AttachmentAnalysisProperties properties;

    @Test
    public void shouldBindMimoProfileProperties() {
        Assert.assertEquals(AttachmentAnalysisProperties.ImageParserMode.MIMO,
                properties.getParser().getImage().getMode());
        Assert.assertEquals("mimo-v2-omni", properties.getParser().getImage().getMimo().getModel());
        Assert.assertEquals(10000, properties.getParser().getImage().getMimo().getConnectTimeoutMs());
        Assert.assertEquals(120000, properties.getParser().getImage().getMimo().getReadTimeoutMs());

        Assert.assertEquals(AttachmentAnalysisProperties.OutlineMode.MIMO, properties.getOutline().getMode());
        Assert.assertEquals("mimo-v2.5-pro", properties.getOutline().getMimo().getModel());
        Assert.assertEquals("https://api.xiaomimimo.com/v1", properties.getOutline().getMimo().getBaseUrl());
    }
}
