package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.AuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, String entityId);
}
