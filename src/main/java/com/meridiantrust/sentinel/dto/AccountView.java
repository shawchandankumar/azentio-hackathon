package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.AccountStatus;
import com.meridiantrust.sentinel.domain.enums.AccountType;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import java.time.LocalDate;

/** Account projection within a customer view. */
public record AccountView(
        String accountNumber,
        AccountType accountType,
        String currency,
        LocalDate openingDate,
        RiskRating riskRating,
        AccountStatus status) {
}
