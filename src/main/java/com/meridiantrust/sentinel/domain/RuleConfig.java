package com.meridiantrust.sentinel.domain;

import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Tunable configuration for a detection rule. Thresholds and windows live in
 * {@link #paramsJson} so compliance can retune rules without a redeploy
 * (Detection Engine requirement + Business Rules).
 */
@Entity
@Table(name = "rule_config")
@Getter
@Setter
@NoArgsConstructor
public class RuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 40)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private RuleType ruleType;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "base_weight", nullable = false)
    private int baseWeight = 50;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity = Severity.MEDIUM;

    @Column(name = "params_json", nullable = false, columnDefinition = "text")
    private String paramsJson = "{}";

    @Column(name = "updated_by", length = 80)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
