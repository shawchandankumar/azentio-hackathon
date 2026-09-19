package com.meridiantrust.sentinel.dto;

/** Partial update of a rule's tunable configuration. Null fields are left unchanged. */
public record RuleUpdateRequest(
        Boolean enabled,
        Integer baseWeight,
        String severity,
        String paramsJson) {
}
