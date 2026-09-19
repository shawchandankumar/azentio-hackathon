package com.meridiantrust.sentinel.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Upsert an FX rate for a currency (1 unit = rateToBase base-currency units). */
public record ExchangeRateUpsertRequest(
        @NotNull @Positive BigDecimal rateToBase) {
}
