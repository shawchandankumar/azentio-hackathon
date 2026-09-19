package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.Channel;
import com.meridiantrust.sentinel.domain.enums.Direction;
import com.meridiantrust.sentinel.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Inbound transaction record for ingestion (bulk or streaming). */
public record TransactionIngestRequest(
        @NotBlank @Size(max = 60) String txnRef,
        @NotBlank @Size(max = 40) String accountNumber,
        @NotNull Direction direction,
        @NotNull TransactionType txnType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Size(max = 200) String counterpartyName,
        @Size(max = 60) String counterpartyAccount,
        @Size(max = 2) String counterpartyCountry,
        @NotNull Channel channel,
        @Size(max = 2) String jurisdiction,
        @NotNull Instant bookedAt) {
}
