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
package com.meridiantrust.sentinel.domain;

import com.meridiantrust.sentinel.domain.enums.AlertStatus;
import com.meridiantrust.sentinel.domain.enums.Disposition;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A risk-scored alert produced by the detection engine. Alerts are never
 * physically deleted; resolution is recorded via {@link #disposition} and
 * {@link #status} to preserve the audit trail (Business Rule 6).
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_ref", nullable = false, unique = true, length = 40)
    private String alertRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private RuleType ruleType;

    @Column(name = "contributing_rules", length = 300)
    private String contributingRules;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(name = "dedup_key", nullable = false, unique = true, length = 200)
    private String dedupKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Disposition disposition;

    @Column(name = "disposition_reason", columnDefinition = "text")
    private String dispositionReason;

    @Column(name = "disposed_by", length = 80)
    private String disposedBy;

    @Column(name = "disposed_at")
    private Instant disposedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private CaseFile caseFile;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlertEvidence> evidence = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Attaches a supporting transaction as evidence if not already present. */
    public void addEvidence(Transaction txn, String note) {
        boolean exists = evidence.stream()
                .anyMatch(e -> e.getTransaction() != null
                        && e.getTransaction().getId() != null
                        && e.getTransaction().getId().equals(txn.getId()));
        if (!exists) {
            AlertEvidence e = new AlertEvidence();
            e.setAlert(this);
            e.setTransaction(txn);
            e.setNote(note);
            evidence.add(e);
        }
    }
}
