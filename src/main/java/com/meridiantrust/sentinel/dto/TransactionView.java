package com.meridiantrust.sentinel.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** A transaction as returned in a customer timeline. */
public record TransactionView(
        String txnRef,
        String accountNumber,
        String direction,
        String txnType,
        BigDecimal amount,
        String currency,
        BigDecimal baseAmount,
        String counterpartyName,
        String counterpartyCountry,
        String channel,
        String jurisdiction,
        Instant bookedAt) {
}
