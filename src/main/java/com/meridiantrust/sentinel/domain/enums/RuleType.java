package com.meridiantrust.sentinel.domain.enums;

/**
 * AML typologies supported by the detection engine. The name of each constant
 * matches the {@code rule_type} stored in {@code rule_config}.
 */
public enum RuleType {
    LARGE_TRANSACTION,
    STRUCTURING,
    RAPID_MOVEMENT,
    HIGH_RISK_JURISDICTION,
    BEHAVIORAL_DEVIATION,
    ROUND_NUMBER
}
