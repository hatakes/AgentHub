package com.sean.agenthub.agent.attachment.domain;

/**
 * 附件分析结构化结果。
 *
 * @author Sean
 */
public class AttachmentAnalysisResult {
    private String attachmentId;
    private String documentType;
    private String birthDate;
    private int age;
    private boolean adult;
    private boolean passed;
    private String opinion;

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isAdult() {
        return adult;
    }

    public void setAdult(boolean adult) {
        this.adult = adult;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

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
