package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.AlertStatus;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import java.time.Instant;

/** Alert row for the analyst queue. Customer name is masked (Business Rule 8). */
public record AlertSummaryView(
        String alertRef,
        String customerRef,
        String customerNameMasked,
        RuleType ruleType,
        String contributingRules,
        int riskScore,
        Severity severity,
        AlertStatus status,
        String title,
        int evidenceCount,
        String caseRef,
        Instant createdAt) {
}
