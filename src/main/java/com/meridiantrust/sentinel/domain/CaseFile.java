package com.meridiantrust.sentinel.domain;

import com.meridiantrust.sentinel.domain.enums.CasePriority;
import com.meridiantrust.sentinel.domain.enums.CaseStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * An investigation case grouping one or more alerts for a customer. Named
 * {@code CaseFile} to avoid clashing with the reserved SQL/JPQL {@code CASE} keyword.
 */
@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
public class CaseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_ref", nullable = false, unique = true, length = 40)
    private String caseRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CasePriority priority = CasePriority.MEDIUM;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "assigned_to", length = 80)
    private String assignedTo;

    @Column(name = "opened_by", nullable = false, length = 80)
    private String openedBy;

    @Column(columnDefinition = "text")
    private String resolution;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
