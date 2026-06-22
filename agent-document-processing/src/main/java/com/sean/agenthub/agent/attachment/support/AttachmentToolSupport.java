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
 * <p>这些规则用于示例和验收，不试图替代真实 OCR、NLP 或规则引擎。把规则集中在这里，
 * 可以让多个 Tool 共享同一套分类、脱敏和年龄判断口径。</p>
 *
 * @author Sean
 */
public final class AttachmentToolSupport {

    /** 出生日期正则表达式，匹配 yyyy-MM-dd 格式 */
    private static final Pattern BIRTH_DATE_PATTERN = Pattern.compile("(19|20)\\d{2}-\\d{2}-\\d{2}");

    /** 私有构造器，防止实例化工具类 */
    private AttachmentToolSupport() {
    }

    /**
     * 构建通用的附件 ID 参数 Schema。
     * <p>所有附件分析 Tool 共用此 Schema，包含一个必填的 attachmentId 字符串参数。</p>
     *
     * @return Tool 参数 Schema
     */
    public static ToolSchema attachmentIdSchema() {
        ToolSchema schema = new ToolSchema();
        Map<String, ToolSchemaProperty> properties = new LinkedHashMap<String, ToolSchemaProperty>();
        properties.put("attachmentId", new ToolSchemaProperty("string", "附件 ID"));
        schema.setProperties(properties);
        schema.setRequired(Arrays.asList("attachmentId"));
        return schema;
    }

    /**
     * 从 Tool 参数 Map 中安全地获取字符串值。
     *
     * @param arguments 参数 Map
     * @param name      参数名
     * @return 参数值，不存在时返回 null
     */
    public static String argument(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 根据附件内容关键词识别证件类型。
     * <p>MVP 使用关键词分类，重点验证 Tool 编排链路；后续可以替换为模型分类或专用文档分类器。</p>
     *
     * @param record 附件记录
     * @return 证件类型标识：ID_CARD、CONTRACT 或 UNKNOWN
     */
    public static String classify(AttachmentRecord record) {
        String text = normalize(record.getText());
        // MVP 使用关键词分类，重点验证 Tool 编排链路；后续可以替换为模型分类或专用文档分类器。
        if (text.contains("身份证") || text.contains("居民身份证") || text.contains("公民身份号码")) {
            return "ID_CARD";
        }
        if (text.contains("合同") || text.contains("甲方") || text.contains("乙方")) {
            return "CONTRACT";
        }
        return "UNKNOWN";
    }

    /**
     * 从附件文本中提取出生日期。
     * <p>使用正则匹配 yyyy-MM-dd 格式的日期，返回第一个匹配项。</p>
     *
     * @param record 附件记录
     * @return 出生日期字符串，未找到时返回空字符串
     */
    public static String extractBirthDate(AttachmentRecord record) {
        Matcher matcher = BIRTH_DATE_PATTERN.matcher(record.getText() == null ? "" : record.getText());
        return matcher.find() ? matcher.group() : "";
    }

    /**
     * 从附件文本中提取并脱敏证件号码。
     * <p>识别 18 位身份证号格式，保留前 6 位和后 4 位，中间用星号替换。</p>
     *
     * @param record 附件记录
     * @return 脱敏后的证件号，未找到时返回空字符串
     */
    public static String maskIdNumber(AttachmentRecord record) {
        String text = record.getText() == null ? "" : record.getText();
        Matcher matcher = Pattern.compile("\\d{6}\\d{8}\\d{3}[0-9Xx]").matcher(text);
        if (!matcher.find()) {
            return "";
        }
        String idNumber = matcher.group();
        return idNumber.substring(0, 6) + "********" + idNumber.substring(14);
    }

    /**
     * 对文本中的证件号码进行脱敏处理。
     * <p>Tool 结果可能进入审计和模型上下文，返回文本预览前必须先做基础证件号脱敏。</p>
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public static String redactSensitiveText(String text) {
        if (text == null) {
            return "";
        }
        // Tool 结果可能进入审计和模型上下文，返回文本预览前必须先做基础证件号脱敏。
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

    /**
     * 生成附件分析结构化结果。
     * <p>汇总分类、出生日期提取和年龄计算，生成完整的分析结论。</p>
     *
     * @param attachmentId 附件 ID
     * @param record       附件记录
     * @return 结构化的分析结果
     */
    public static AttachmentAnalysisResult analysisResult(String attachmentId, AttachmentRecord record) {
        String documentType = classify(record);
        String birthDate = extractBirthDate(record);
        // 示例验收固定以 2026 年的日期计算年龄，保证测试数据和文档中的预期结论稳定。
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

    /**
     * 基于 2026 年 6 月 10 日计算年龄。
     * <p>示例验收固定以 2026 年的日期计算，保证测试数据和文档中的预期结论稳定。</p>
     *
     * @param birthDate 出生日期，格式 yyyy-MM-dd
     * @return 年龄，日期无效时返回 -1
     */
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

    /**
     * 安全地处理 null 字符串。
     *
     * @param value 原始字符串
     * @return 非 null 的字符串
     */
    public static String normalize(String value) {
        return value == null ? "" : value;
    }
}
