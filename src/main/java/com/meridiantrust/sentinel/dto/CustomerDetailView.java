package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.CustomerType;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Customer detail. Name and national ID are masked unless the caller is authorized. */
public record CustomerDetailView(
        String customerRef,
        String fullName,
        String nationalId,
        CustomerType customerType,
        RiskRating riskRating,
        String residenceCountry,
        LocalDate dateOfBirth,
        String email,
        String phone,
        Instant onboardedAt,
        boolean piiMasked,
        List<AccountView> accounts) {
}
