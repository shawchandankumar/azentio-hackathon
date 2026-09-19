package com.meridiantrust.sentinel.domain.enums;

/** Disposition reason recorded when an alert is cleared or confirmed. */
public enum Disposition {
    FALSE_POSITIVE,
    CONFIRMED_SUSPICIOUS,
    ESCALATED_SAR,
    CLEARED_NO_ACTION,
    DUPLICATE
}
