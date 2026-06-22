package com.sean.agenthub.agent.attachment.domain;

/**
 * 附件分析结构化结果。
 *
 * @author Sean
 */
public class AttachmentAnalysisResult {

    /** 附件 ID */
    private String attachmentId;

    /** 识别出的证件类型，如 ID_CARD、CONTRACT、UNKNOWN */
    private String documentType;

    /** 出生日期，格式为 yyyy-MM-dd */
    private String birthDate;

    /** 年龄，基于 2026 年计算 */
    private int age;

    /** 是否成年（>=18 岁） */
    private boolean adult;

    /** 规则校验是否通过 */
    private boolean passed;

    /** 审核意见 */
    private String opinion;

    /**
     * 获取附件 ID。
     *
     * @return 附件 ID
     */
    public String getAttachmentId() {
        return attachmentId;
    }

    /**
     * 设置附件 ID。
     *
     * @param attachmentId 附件 ID
     */
    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    /**
     * 获取证件类型。
     *
     * @return 证件类型
     */
    public String getDocumentType() {
        return documentType;
    }

    /**
     * 设置证件类型。
     *
     * @param documentType 证件类型
     */
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    /**
     * 获取出生日期。
     *
     * @return 出生日期
     */
    public String getBirthDate() {
        return birthDate;
    }

    /**
     * 设置出生日期。
     *
     * @param birthDate 出生日期
     */
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * 获取年龄。
     *
     * @return 年龄
     */
    public int getAge() {
        return age;
    }

    /**
     * 设置年龄。
     *
     * @param age 年龄
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * 获取是否成年。
     *
     * @return 是否成年
     */
    public boolean isAdult() {
        return adult;
    }

    /**
     * 设置是否成年。
     *
     * @param adult 是否成年
     */
    public void setAdult(boolean adult) {
        this.adult = adult;
    }

    /**
     * 获取规则校验是否通过。
     *
     * @return 是否通过
     */
    public boolean isPassed() {
        return passed;
    }

    /**
     * 设置规则校验是否通过。
     *
     * @param passed 是否通过
     */
    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    /**
     * 获取审核意见。
     *
     * @return 审核意见
     */
    public String getOpinion() {
        return opinion;
    }

    /**
     * 设置审核意见。
     *
     * @param opinion 审核意见
     */
    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    /**
     * 返回结构化结果的字符串表示，用于日志和调试。
     *
     * @return 包含所有字段的字符串
     */
    @Override
    public String toString() {
        return "{attachmentId=" + attachmentId
                + ", documentType=" + documentType
                + ", birthDate=" + birthDate
                + ", age=" + age
                + ", adult=" + adult
                + ", passed=" + passed
                + ", opinion=" + opinion + "}";
    }
}
