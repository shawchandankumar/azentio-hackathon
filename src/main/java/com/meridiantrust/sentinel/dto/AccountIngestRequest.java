package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.AccountStatus;
import com.meridiantrust.sentinel.domain.enums.AccountType;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Inbound account metadata for ingestion. */
public record AccountIngestRequest(
        @NotBlank @Size(max = 40) String accountNumber,
        @NotBlank @Size(max = 40) String customerRef,
        @NotNull AccountType accountType,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate openingDate,
        RiskRating riskRating,
        AccountStatus status) {
}
