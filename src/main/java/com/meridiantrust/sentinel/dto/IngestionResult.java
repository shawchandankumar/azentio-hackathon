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

import java.util.ArrayList;
import java.util.List;

/** Outcome of a (possibly batch) ingestion request. */
public record IngestionResult(
        int accepted,
        int rejected,
        List<String> errors,
        List<String> alertRefs) {

    /** Mutable accumulator used while processing a batch. */
    public static final class Builder {
        private int accepted;
        private int rejected;
        private final List<String> errors = new ArrayList<>();
        private final List<String> alertRefs = new ArrayList<>();

        public void accept() {
            accepted++;
        }

        public void reject(String reason) {
            rejected++;
            errors.add(reason);
        }

        public void addAlert(String ref) {
            alertRefs.add(ref);
        }

        public IngestionResult build() {
            return new IngestionResult(accepted, rejected, List.copyOf(errors), List.copyOf(alertRefs));
        }
    }
}
