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
