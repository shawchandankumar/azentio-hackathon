package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.AuditEvent;
import com.meridiantrust.sentinel.repository.AuditEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the immutable audit trail for alert/case state transitions
 * (Auditability NFR). Records are append-only; there is no update or delete path.
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String entityType, String entityId, String action, String actor,
                       String previousState, String newState, String details) {
        AuditEvent event = new AuditEvent();
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setAction(action);
        event.setActor(actor);
        event.setPreviousState(previousState);
        event.setNewState(newState);
        event.setDetails(details);
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> trail(String entityType, String entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(entityType, entityId);
    }
}
