package com.sean.agenthub.agent.attachment;

import com.sean.agenthub.agent.attachment.application.AttachmentAnalysisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 文档处理能力模块应用。
 *
 * @author Sean
 */
@SpringBootApplication
@EnableConfigurationProperties(AttachmentAnalysisProperties.class)
public class AttachmentAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(AttachmentAnalysisApplication.class, args);
    }
}
