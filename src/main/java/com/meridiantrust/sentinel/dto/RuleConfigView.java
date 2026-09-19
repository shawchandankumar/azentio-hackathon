package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import java.time.Instant;

/** Rule configuration as returned by the admin API. */
public record RuleConfigView(
        String ruleCode,
        RuleType ruleType,
        String displayName,
        String description,
        boolean enabled,
        int baseWeight,
        Severity severity,
        String paramsJson,
        String updatedBy,
        Instant updatedAt) {
}
