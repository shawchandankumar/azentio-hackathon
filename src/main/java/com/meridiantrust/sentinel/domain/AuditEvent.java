package com.meridiantrust.sentinel.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Immutable audit record for alert/case state transitions (Auditability NFR).
 * Rows are only ever inserted, never updated or deleted.
 */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 40)
    private String entityId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false, length = 80)
    private String actor;

    @Column(name = "previous_state", length = 40)
    private String previousState;

    @Column(name = "new_state", length = 40)
    private String newState;

    @Column(columnDefinition = "text")
    private String details;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
