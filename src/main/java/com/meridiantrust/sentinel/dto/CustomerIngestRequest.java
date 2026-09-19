package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.CustomerType;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** Inbound KYC record for customer ingestion. */
public record CustomerIngestRequest(
        @NotBlank @Size(max = 40) String customerRef,
        @NotBlank @Size(max = 200) String fullName,
        LocalDate dateOfBirth,
        @Size(max = 60) String nationalId,
        @NotNull CustomerType customerType,
        RiskRating riskRating,
        @NotBlank @Size(min = 2, max = 2) String residenceCountry,
        @Size(max = 200) String email,
        @Size(max = 40) String phone,
        Instant onboardedAt) {
}
