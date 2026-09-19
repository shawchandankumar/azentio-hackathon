/*
 * Sentinel AML — Real-Time Money Laundering Detection Platform
 * Copyright (C) 2026 Chandan Kumar Shaw <shawchandankumar20@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.Alert;
import com.meridiantrust.sentinel.domain.AuditEvent;
import com.meridiantrust.sentinel.domain.CaseFile;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.dto.AlertDetailView;
import com.meridiantrust.sentinel.dto.AlertSummaryView;
import com.meridiantrust.sentinel.dto.AuditEventView;
import com.meridiantrust.sentinel.dto.CaseView;
import com.meridiantrust.sentinel.dto.RuleConfigView;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pure entity → view-DTO conversion. Applies PII masking for list projections.
 * Callers must invoke these inside a transaction so lazy associations resolve.
 */
@Component
public class ViewMapper {

    private final PiiMaskingService masking;

    public ViewMapper(PiiMaskingService masking) {
        this.masking = masking;
    }

    public AlertSummaryView toSummary(Alert a) {
        return new AlertSummaryView(
                a.getAlertRef(),
                a.getCustomer().getCustomerRef(),
                masking.maskName(a.getCustomer().getFullName()),
                a.getRuleType(),
                a.getContributingRules(),
                a.getRiskScore(),
                a.getSeverity(),
                a.getStatus(),
                a.getTitle(),
                a.getEvidence().size(),
                a.getCaseFile() == null ? null : a.getCaseFile().getCaseRef(),
                a.getCreatedAt());
    }

    public AlertDetailView toDetail(Alert a, boolean unmaskPii) {
        List<AlertDetailView.EvidenceView> evidence = a.getEvidence().stream()
                .map(e -> {
                    var t = e.getTransaction();
                    return new AlertDetailView.EvidenceView(
                            t.getTxnRef(),
                            t.getDirection().name(),
                            t.getTxnType().name(),
                            t.getAmount(),
                            t.getCurrency(),
                            t.getBaseAmount(),
                            t.getCounterpartyName(),
                            t.getCounterpartyCountry(),
                            t.getChannel().name(),
                            t.getJurisdiction(),
                            t.getBookedAt(),
                            e.getNote());
                })
                .toList();

        String name = unmaskPii
                ? a.getCustomer().getFullName()
                : masking.maskName(a.getCustomer().getFullName());

        return new AlertDetailView(
                a.getAlertRef(),
                a.getCustomer().getCustomerRef(),
                name,
                a.getAccount() == null ? null : a.getAccount().getAccountNumber(),
                a.getRuleType(),
                a.getContributingRules(),
                a.getRiskScore(),
                a.getSeverity(),
                a.getStatus(),
                a.getTitle(),
                a.getExplanation(),
                a.getDisposition(),
                a.getDispositionReason(),
                a.getDisposedBy(),
                a.getDisposedAt(),
                a.getCaseFile() == null ? null : a.getCaseFile().getCaseRef(),
                a.getCreatedAt(),
                evidence);
    }

    public CaseView toCaseView(CaseFile c, List<String> linkedAlertRefs) {
        return new CaseView(
                c.getCaseRef(),
                c.getCustomer().getCustomerRef(),
                masking.maskName(c.getCustomer().getFullName()),
                c.getStatus(),
                c.getPriority(),
                c.getTitle(),
                c.getSummary(),
                c.getAssignedTo(),
                c.getOpenedBy(),
                c.getResolution(),
                c.getOpenedAt(),
                c.getClosedAt(),
                linkedAlertRefs);
    }

    public AuditEventView toAuditView(AuditEvent e) {
        return new AuditEventView(
                e.getEntityType(),
                e.getEntityId(),
                e.getAction(),
                e.getActor(),
                e.getPreviousState(),
                e.getNewState(),
                e.getDetails(),
                e.getOccurredAt());
    }

    public RuleConfigView toRuleView(RuleConfig r) {
        return new RuleConfigView(
                r.getRuleCode(),
                r.getRuleType(),
                r.getDisplayName(),
                r.getDescription(),
                r.isEnabled(),
                r.getBaseWeight(),
                r.getSeverity(),
                r.getParamsJson(),
                r.getUpdatedBy(),
                r.getUpdatedAt());
    }
}
