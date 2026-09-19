package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.AlertStatus;
import com.meridiantrust.sentinel.domain.enums.Disposition;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Full alert detail including supporting evidence. PII may be unmasked for authorized roles. */
public record AlertDetailView(
        String alertRef,
        String customerRef,
        String customerName,
        String accountNumber,
        RuleType ruleType,
        String contributingRules,
        int riskScore,
        Severity severity,
        AlertStatus status,
        String title,
        String explanation,
        Disposition disposition,
        String dispositionReason,
        String disposedBy,
        Instant disposedAt,
        String caseRef,
        Instant createdAt,
        List<EvidenceView> evidence) {

    /** A supporting transaction attached to an alert. */
    public record EvidenceView(
            String txnRef,
            String direction,
            String txnType,
            BigDecimal amount,
            String currency,
            BigDecimal baseAmount,
            String counterpartyName,
            String counterpartyCountry,
            String channel,
            String jurisdiction,
            Instant bookedAt,
            String note) {
    }
}
