package com.sean.agenthub.agent.attachment.infrastructure.parser;

import com.sean.agenthub.agent.attachment.domain.ParsedAttachmentContent;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件解析器选择器。
 *
 * <p>解析器按 Spring 注入顺序遍历，找到第一个支持当前 MultipartFile 的实现。
 * 业务侧新增格式时只需要新增 AttachmentContentParser Bean，不需要改上传 Controller。</p>
 *
 * @author Sean
 */
@Component
public class AttachmentContentParserRegistry {

    /** 所有已注册的附件解析器，按 Spring 注入顺序排列 */
    private final List<AttachmentContentParser> parsers;

    /**
     * 构造器注入所有解析器。
     *
     * @param parsers 解析器列表
     */
    public AttachmentContentParserRegistry(List<AttachmentContentParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * 解析上传的文件。
     * <p>遍历所有已注册的解析器，找到第一个支持当前文件的解析器执行解析。
     * 没有匹配的解析器时抛出 IllegalArgumentException。</p>
     *
     * @param file 上传的文件
     * @return 解析后的内容
     * @throws IOException              文件读取异常
     * @throws IllegalArgumentException 没有支持的解析器时抛出
     */
    public ParsedAttachmentContent parse(MultipartFile file) throws IOException {
        for (AttachmentContentParser parser : parsers) {
            if (parser.supports(file)) {
                // 选择逻辑集中在 registry，具体 parser 只关心自己的 content-type / 文件名判断和内容提取。
                return parser.parse(file);
            }
        }
        throw new IllegalArgumentException("Unsupported attachment content type: " + file.getContentType());
    }
}
