package com.sean.agenthub.agent.provider.http.transport;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * HTTP 请求辅助方法测试。
 *
 * @author Sean
 */
public class HttpRequestSupportTest {
    @Test
    public void shouldReadBodyAsUtf8Text() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("你好\n世界".getBytes(StandardCharsets.UTF_8));

        String body = HttpRequestSupport.readBody(inputStream);

        Assert.assertEquals("你好世界", body);
    }

    @Test
    public void shouldReadLinesInOrder() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("a\nb\n".getBytes(StandardCharsets.UTF_8));
        final List<String> lines = new ArrayList<String>();

        HttpRequestSupport.readLines(inputStream, new HttpRequestSupport.LineConsumer() {
            @Override
            public void onLine(String line) {
                lines.add(line);
            }
        });

        Assert.assertEquals(2, lines.size());
        Assert.assertEquals("a", lines.get(0));
        Assert.assertEquals("b", lines.get(1));
    }

    @Test
    public void shouldExtractSseDataPayload() {
        Assert.assertEquals("{\"ok\":true}", HttpRequestSupport.sseData("data: {\"ok\":true}"));
        Assert.assertNull(HttpRequestSupport.sseData("event: message"));
        Assert.assertNull(HttpRequestSupport.sseData("data:"));
        Assert.assertNull(HttpRequestSupport.sseData("data: [DONE]"));
    }
}
