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
