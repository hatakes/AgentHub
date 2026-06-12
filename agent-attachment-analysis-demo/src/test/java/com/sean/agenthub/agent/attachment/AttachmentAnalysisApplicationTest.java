package com.sean.agenthub.agent.attachment;

import com.sean.agenthub.agent.attachment.infrastructure.AttachmentAuditService;
import com.sean.agenthub.agent.attachment.infrastructure.AttachmentRepository;
import com.sean.agenthub.agent.core.model.AgentRequest;
import com.sean.agenthub.agent.core.model.AgentResponse;
import com.sean.agenthub.agent.core.model.AuditEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 智能附件分析样板集成测试。
 *
 * @author Sean
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AttachmentAnalysisApplicationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private AttachmentRepository repository;
    @Autowired
    private AttachmentAuditService auditService;

    @Before
    public void setUp() {
        repository.clear();
        auditService.clear();
    }

    @Test
    public void shouldUploadAttachmentAndAnalyzeWithToolAudit() {
        String attachmentId = uploadTextAttachment("id-card.txt",
                "居民身份证 姓名 张三 公民身份号码 110101200901011234 出生日期 2009-01-01");

        AgentRequest request = new AgentRequest();
        request.setSessionId("s-attachment");
        request.setUserId("attachment-reviewer");
        request.setMessage("请分析附件 " + attachmentId);

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isOk());
        Assert.assertTrue(response.getAnswer().contains("documentType=ID_CARD"));
        Assert.assertTrue(response.getAnswer().contains("age=17"));
        Assert.assertTrue(response.getAnswer().contains("adult=false"));
        Assert.assertTrue(response.getAnswer().contains("passed=false"));
        Assert.assertTrue(response.getAnswer().contains("建议驳回"));
        Assert.assertFalse(response.getAnswer().contains("110101200901011234"));
        Assert.assertEquals(5, response.getToolCalls().size());
        Assert.assertEquals("parse_attachment", response.getToolCalls().get(0).getTool());
        Assert.assertEquals("summarize_attachment_analysis", response.getToolCalls().get(4).getTool());

        List<AuditEvent> events = auditService.getEvents();
        Assert.assertEquals(5, events.size());
        Assert.assertEquals("attachment-reviewer", events.get(0).getUserId());
        Assert.assertTrue(events.get(0).isSuccess());
        Assert.assertFalse(events.get(0).getToolResultSummary().contains("110101200901011234"));
    }

    @Test
    public void shouldNotTriggerAttachmentToolForNormalChat() {
        AgentRequest request = new AgentRequest();
        request.setSessionId("s-normal");
        request.setUserId("attachment-reviewer");
        request.setMessage("请介绍一下智能附件分析");

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isOk());
        Assert.assertTrue(response.getAnswer().contains("附件分析样板收到"));
        Assert.assertTrue(response.getToolCalls().isEmpty());
        Assert.assertTrue(auditService.getEvents().isEmpty());
    }

    @Test
    public void shouldDenyAttachmentAnalysisWithoutPermission() {
        String attachmentId = uploadTextAttachment("contract.txt", "合同 甲方 A 乙方 B");

        AgentRequest request = new AgentRequest();
        request.setSessionId("s-denied");
        request.setUserId("u001");
        request.setMessage("请分析附件 " + attachmentId);

        AgentResponse response = restTemplate.postForObject("/agent/chat", request, AgentResponse.class);

        Assert.assertNotNull(response);
        Assert.assertFalse(response.isOk());
        Assert.assertTrue(response.getErrorMessage().contains("Tool permission denied"));

        List<AuditEvent> events = auditService.getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("parse_attachment", events.get(0).getToolName());
        Assert.assertFalse(events.get(0).isSuccess());
        Assert.assertTrue(events.get(0).getErrorMessage().contains("Only attachment-reviewer"));
    }

    @Test
    public void shouldAnalyzeExistingAttachmentThroughBusinessApi() {
        String attachmentId = uploadTextAttachment("id-card.txt",
                "居民身份证 姓名 李四 公民身份号码 110101199901011234 出生日期 1999-01-01");

        Map<String, Object> request = new java.util.LinkedHashMap<String, Object>();
        request.put("attachmentId", attachmentId);
        request.put("userId", "attachment-reviewer");
        request.put("sessionId", "s-business-analyze");

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachment-analysis/analyze", request, Map.class);

        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(attachmentId, response.getBody().get("attachmentId"));
        Assert.assertEquals(Boolean.TRUE, response.getBody().get("ok"));
        Assert.assertTrue(String.valueOf(response.getBody().get("answer")).contains("documentType=ID_CARD"));
        Assert.assertTrue(String.valueOf(response.getBody().get("answer")).contains("adult=true"));
        Assert.assertFalse(String.valueOf(response.getBody().get("answer")).contains("110101199901011234"));
        Map analysis = (Map) response.getBody().get("analysis");
        Assert.assertNotNull(analysis);
        Assert.assertEquals("ID_CARD", analysis.get("documentType"));
        Assert.assertEquals("1999-01-01", analysis.get("birthDate"));
        Assert.assertEquals(27, ((Number) analysis.get("age")).intValue());
        Assert.assertEquals(Boolean.TRUE, analysis.get("adult"));
        Assert.assertEquals(Boolean.TRUE, analysis.get("passed"));
        Assert.assertEquals(5, auditService.getEvents().size());
    }

    @Test
    public void shouldUploadAndAnalyzeFileThroughBusinessApi() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", filePart("id-card.txt",
                "居民身份证 姓名 王五 公民身份号码 110101200901011234 出生日期 2009-01-01"));
        body.add("userId", "attachment-reviewer");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachment-analysis/analyze-file", request, Map.class);

        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertNotNull(response.getBody());
        Assert.assertNotNull(response.getBody().get("attachmentId"));
        Assert.assertEquals(Boolean.TRUE, response.getBody().get("ok"));
        Assert.assertTrue(String.valueOf(response.getBody().get("answer")).contains("age=17"));
        Assert.assertTrue(String.valueOf(response.getBody().get("answer")).contains("passed=false"));
        Assert.assertFalse(String.valueOf(response.getBody().get("answer")).contains("110101200901011234"));
        Map analysis = (Map) response.getBody().get("analysis");
        Assert.assertNotNull(analysis);
        Assert.assertEquals("ID_CARD", analysis.get("documentType"));
        Assert.assertEquals(17, ((Number) analysis.get("age")).intValue());
        Assert.assertEquals(Boolean.FALSE, analysis.get("adult"));
        Assert.assertEquals(Boolean.FALSE, analysis.get("passed"));
        Assert.assertTrue(String.valueOf(analysis.get("opinion")).contains("未满 18 周岁"));
        Assert.assertEquals(5, auditService.getEvents().size());
    }

    @Test
    public void shouldAnalyzeImageThroughImageParserExtensionPoint() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", filePart("id-card.png",
                "居民身份证 姓名 赵六 公民身份号码 110101200901011234 出生日期 2009-01-01",
                MediaType.IMAGE_PNG));
        body.add("userId", "attachment-reviewer");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachment-analysis/analyze-file", request, Map.class);

        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(Boolean.TRUE, response.getBody().get("ok"));
        Map analysis = (Map) response.getBody().get("analysis");
        Assert.assertNotNull(analysis);
        Assert.assertEquals("ID_CARD", analysis.get("documentType"));
        Assert.assertEquals(17, ((Number) analysis.get("age")).intValue());
        Assert.assertEquals(Boolean.FALSE, analysis.get("passed"));
    }

    @Test
    public void shouldExtractOutlineFromMarkdownDocument() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", filePart("policy.md",
                "# 招生政策解读\n"
                        + "## 报名条件\n"
                        + "考生需要关注报名时间、资格审核和材料提交要求。\n"
                        + "## 录取重点\n"
                        + "重点关注专业组、选科要求、往年位次和风险兜底方案。",
                MediaType.parseMediaType("text/markdown")));
        body.add("userId", "attachment-reviewer");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachment-analysis/outline-file", request, Map.class);

        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(Boolean.TRUE, response.getBody().get("ok"));
        Assert.assertNotNull(response.getBody().get("attachmentId"));
        Map outline = (Map) response.getBody().get("outline");
        Assert.assertNotNull(outline);
        Assert.assertEquals("markdown", outline.get("parserName"));
        Assert.assertEquals("招生政策解读", outline.get("title"));
        Assert.assertTrue(((List) outline.get("outline")).contains("报名条件"));
        Assert.assertTrue(String.valueOf(outline.get("summary")).contains("提炼出"));
        Assert.assertTrue(auditService.getEvents().isEmpty());
    }

    @Test
    public void shouldExtractOutlineFromPdfDocument() throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", filePart("policy.pdf", pdfBytes(
                "1. Policy Background",
                "This policy focuses on AI, digital economy, and green industry.",
                "2. Application Requirements",
                "Applicants must submit materials, implementation plans, and risk controls."
        ), MediaType.APPLICATION_PDF));
        body.add("userId", "attachment-reviewer");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachment-analysis/outline-file", request, Map.class);

        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(Boolean.TRUE, response.getBody().get("ok"));
        Map outline = (Map) response.getBody().get("outline");
        Assert.assertNotNull(outline);
        Assert.assertEquals("pdfbox", outline.get("parserName"));
        Assert.assertTrue(((List) outline.get("outline")).contains("1. Policy Background"));
        Assert.assertTrue(String.valueOf(outline.get("keyPoints")).contains("digital economy"));
        Map metadata = (Map) outline.get("metadata");
        Assert.assertEquals(1, ((Number) metadata.get("pageCount")).intValue());
    }

    @Test
    public void shouldDenyOutlineAnalysisWithoutPermission() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", filePart("policy.md", "# 政策\n重点内容", MediaType.parseMediaType("text/markdown")));
        body.add("userId", "u001");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachment-analysis/outline-file", request, Map.class);

        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(Boolean.FALSE, response.getBody().get("ok"));
        Assert.assertTrue(String.valueOf(response.getBody().get("errorMessage"))
                .contains("Only attachment-reviewer"));
        Assert.assertNull(response.getBody().get("attachmentId"));
        Assert.assertTrue(auditService.getEvents().isEmpty());
    }

    @Test
    public void shouldReturnReadableErrorForUnsupportedFileType() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", filePart("payload.bin", "binary", MediaType.APPLICATION_OCTET_STREAM));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachments", request, Map.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(Boolean.FALSE, response.getBody().get("ok"));
        Assert.assertTrue(String.valueOf(response.getBody().get("errorMessage"))
                .contains("Unsupported attachment content type"));
    }

    @Test
    public void shouldReturnReadableErrorForEmptyParsedAttachment() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", filePart("empty.txt", "   "));
        body.add("userId", "attachment-reviewer");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachment-analysis/analyze-file", request, Map.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals(Boolean.FALSE, response.getBody().get("ok"));
        Assert.assertTrue(String.valueOf(response.getBody().get("errorMessage"))
                .contains("Attachment content is empty after parsing"));
    }

    @Test
    public void shouldReturnStructuredErrorForMissingAttachmentId() {
        Map<String, Object> request = new java.util.LinkedHashMap<String, Object>();
        request.put("attachmentId", "att-missing");
        request.put("userId", "attachment-reviewer");
        request.put("sessionId", "s-missing");

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachment-analysis/analyze", request, Map.class);

        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals("att-missing", response.getBody().get("attachmentId"));
        Assert.assertEquals(Boolean.FALSE, response.getBody().get("ok"));
        Assert.assertTrue(String.valueOf(response.getBody().get("errorMessage"))
                .contains("Attachment not found: att-missing"));
        Assert.assertNull(response.getBody().get("analysis"));
        Assert.assertTrue(auditService.getEvents().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private String uploadTextAttachment(String filename, String text) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", filePart(filename, text));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/attachments", request, Map.class);
        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertNotNull(response.getBody());
        Assert.assertNotNull(response.getBody().get("attachmentId"));
        return String.valueOf(response.getBody().get("attachmentId"));
    }

    private HttpEntity<ByteArrayResource> filePart(final String filename, String text) {
        return filePart(filename, text, MediaType.TEXT_PLAIN);
    }

    private HttpEntity<ByteArrayResource> filePart(final String filename, String text, MediaType contentType) {
        return filePart(filename, text.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private HttpEntity<ByteArrayResource> filePart(final String filename, byte[] bytes, MediaType contentType) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        return new HttpEntity<ByteArrayResource>(resource, headers);
    }

    private byte[] pdfBytes(String... lines) throws IOException {
        PDDocument document = new PDDocument();
        try {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(72, 720);
                for (String line : lines) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -18);
                }
                contentStream.endText();
            } finally {
                contentStream.close();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } finally {
            document.close();
        }
    }
}
