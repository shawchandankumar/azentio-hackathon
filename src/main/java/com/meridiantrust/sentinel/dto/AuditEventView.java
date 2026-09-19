package com.meridiantrust.sentinel.dto;

import java.time.Instant;

/** Immutable audit trail entry as returned by the API. */
public record AuditEventView(
        String entityType,
        String entityId,
        String action,
        String actor,
        String previousState,
        String newState,
        String details,
        Instant occurredAt) {
}
