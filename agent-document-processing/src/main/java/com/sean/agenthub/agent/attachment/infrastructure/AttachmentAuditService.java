package com.sean.agenthub.agent.attachment.infrastructure;

import com.sean.agenthub.agent.core.api.AuditService;
import com.sean.agenthub.agent.core.model.AuditEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 文档处理能力审计服务。
 *
 * @author Sean
 */
@Component
public class AttachmentAuditService implements AuditService {

    /** 线程安全的审计事件列表 */
    private final List<AuditEvent> events = Collections.synchronizedList(new ArrayList<AuditEvent>());

    /**
     * 记录一条审计事件。
     *
     * @param event 审计事件
     */
    @Override
    public void record(AuditEvent event) {
        events.add(event);
    }

    /**
     * 获取所有审计事件的快照。
     * <p>返回的是副本，避免并发修改问题。</p>
     *
     * @return 审计事件列表副本
     */
    public List<AuditEvent> getEvents() {
        synchronized (events) {
            return new ArrayList<AuditEvent>(events);
        }
    }

    /**
     * 清空所有审计事件。
     */
    public void clear() {
        events.clear();
    }
}
