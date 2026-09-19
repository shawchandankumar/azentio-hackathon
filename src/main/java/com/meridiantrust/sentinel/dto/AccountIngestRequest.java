/*
 * Sentinel AML — Real-Time Money Laundering Detection Platform
 * Copyright (C) 2026 Chandan Kumar Shaw <shawchandankumar20@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
