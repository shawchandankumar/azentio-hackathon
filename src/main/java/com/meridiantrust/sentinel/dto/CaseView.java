package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.CasePriority;
import com.meridiantrust.sentinel.domain.enums.CaseStatus;
import java.time.Instant;
import java.util.List;

/** Case detail view including linked alert references. */
public record CaseView(
        String caseRef,
        String customerRef,
        String customerNameMasked,
        CaseStatus status,
        CasePriority priority,
        String title,
        String summary,
        String assignedTo,
        String openedBy,
        String resolution,
        Instant openedAt,
        Instant closedAt,
        List<String> linkedAlertRefs) {
}
