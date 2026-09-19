package com.meridiantrust.sentinel.domain.enums;

/**
 * Alert workflow states. Alerts are never deleted; a resolved alert transitions
 * to CLEARED or CONFIRMED and retains its disposition for audit (Business Rule 6).
 */
public enum AlertStatus {
    OPEN,
    UNDER_REVIEW,
    ESCALATED,
    CLEARED,
    CONFIRMED
}
