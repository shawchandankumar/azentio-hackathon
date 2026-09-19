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
package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.HighRiskJurisdiction;
import com.meridiantrust.sentinel.domain.WatchlistCounterparty;
import com.meridiantrust.sentinel.repository.HighRiskJurisdictionRepository;
import com.meridiantrust.sentinel.repository.WatchlistCounterpartyRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lookups against the configurable high-risk jurisdiction and watchlist tables. */
@Service
public class ReferenceDataService {

    private final HighRiskJurisdictionRepository jurisdictionRepository;
    private final WatchlistCounterpartyRepository watchlistRepository;

    public ReferenceDataService(HighRiskJurisdictionRepository jurisdictionRepository,
                                WatchlistCounterpartyRepository watchlistRepository) {
        this.jurisdictionRepository = jurisdictionRepository;
        this.watchlistRepository = watchlistRepository;
    }

    /** Returns the matching high-risk jurisdiction record if the country is listed and active. */
    @Transactional(readOnly = true)
    public Optional<HighRiskJurisdiction> matchJurisdiction(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return Optional.empty();
        }
        return jurisdictionRepository.findByCountryCodeIgnoreCaseAndActiveTrue(countryCode.trim());
    }

    /** Returns the matching watchlist entry if the counterparty name is listed and active. */
    @Transactional(readOnly = true)
    public Optional<WatchlistCounterparty> matchCounterparty(String counterpartyName) {
        if (counterpartyName == null || counterpartyName.isBlank()) {
            return Optional.empty();
        }
        String needle = counterpartyName.trim();
        return watchlistRepository.findByActiveTrue().stream()
                .filter(w -> w.getName().equalsIgnoreCase(needle))
                .findFirst();
    }
}
