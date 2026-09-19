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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Batch of account records for bulk ingestion. */
public record AccountBatchRequest(
        @NotEmpty @Valid List<AccountIngestRequest> accounts) {
}
