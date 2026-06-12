package com.sean.agenthub.agent.attachment.support;

import com.sean.agenthub.agent.attachment.domain.AttachmentRecord;
import com.sean.agenthub.agent.attachment.domain.AttachmentAnalysisResult;
import com.sean.agenthub.agent.core.tool.ToolSchema;
import com.sean.agenthub.agent.core.tool.ToolSchemaProperty;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 附件分析 Tool 的公共辅助逻辑。
 *
 * @author Sean
 */
public final class AttachmentToolSupport {
    private static final Pattern BIRTH_DATE_PATTERN = Pattern.compile("(19|20)\\d{2}-\\d{2}-\\d{2}");

    private AttachmentToolSupport() {
    }

    public static ToolSchema attachmentIdSchema() {
        ToolSchema schema = new ToolSchema();
        Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
        properties.put("attachmentId", new ToolSchemaProperty("string", "附件 ID"));
        schema.setProperties(properties);
        schema.setRequired(Arrays.asList("attachmentId"));
        return schema;
    }

    public static String argument(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null ? null : String.valueOf(value);
    }

    public static String classify(AttachmentRecord record) {
        String text = normalize(record.getText());
        if (text.contains("身份证") || text.contains("居民身份证") || text.contains("公民身份号码")) {
            return "ID_CARD";
        }
        if (text.contains("合同") || text.contains("甲方") || text.contains("乙方")) {
            return "CONTRACT";
        }
        return "UNKNOWN";
    }

    public static String extractBirthDate(AttachmentRecord record) {
        Matcher matcher = BIRTH_DATE_PATTERN.matcher(record.getText() == null ? "" : record.getText());
        return matcher.find() ? matcher.group() : "";
    }

    public static String maskIdNumber(AttachmentRecord record) {
        String text = record.getText() == null ? "" : record.getText();
        Matcher matcher = Pattern.compile("\\d{6}\\d{8}\\d{3}[0-9Xx]").matcher(text);
        if (!matcher.find()) {
            return "";
        }
        String idNumber = matcher.group();
        return idNumber.substring(0, 6) + "********" + idNumber.substring(14);
    }

    public static String redactSensitiveText(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("\\d{6}\\d{8}\\d{3}[0-9Xx]").matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String idNumber = matcher.group();
            String masked = idNumber.substring(0, 6) + "********" + idNumber.substring(14);
            matcher.appendReplacement(result, masked);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static AttachmentAnalysisResult analysisResult(String attachmentId, AttachmentRecord record) {
        String documentType = classify(record);
        String birthDate = extractBirthDate(record);
        int age = ageOn2026(birthDate);
        boolean adult = age >= 18;
        boolean passed = adult || age < 0;

        AttachmentAnalysisResult result = new AttachmentAnalysisResult();
        result.setAttachmentId(attachmentId);
        result.setDocumentType(documentType);
        result.setBirthDate(birthDate);
        result.setAge(age);
        result.setAdult(adult);
        result.setPassed(passed);
        result.setOpinion(passed ? "建议通过附件基础规则检查" : "建议驳回，原因是申请人未满 18 周岁");
        return result;
    }

    public static int ageOn2026(String birthDate) {
        if (birthDate == null || birthDate.length() < 10) {
            return -1;
        }
        int birthYear = Integer.parseInt(birthDate.substring(0, 4));
        int birthMonth = Integer.parseInt(birthDate.substring(5, 7));
        int birthDay = Integer.parseInt(birthDate.substring(8, 10));
        int age = 2026 - birthYear;
        if (birthMonth > 6 || (birthMonth == 6 && birthDay > 10)) {
            age--;
        }
        return age;
    }

    public static String normalize(String value) {
        return value == null ? "" : value;
    }
}
