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
    private final List<AuditEvent> events = Collections.synchronizedList(new ArrayList<AuditEvent>());

    @Override
    public void record(AuditEvent event) {
        events.add(event);
    }

    public List<AuditEvent> getEvents() {
        synchronized (events) {
            return new ArrayList<AuditEvent>(events);
        }
    }

    public void clear() {
        events.clear();
    }
}
