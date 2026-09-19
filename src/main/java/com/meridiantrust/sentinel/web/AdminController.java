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
package com.meridiantrust.sentinel.web;

import com.meridiantrust.sentinel.domain.ExchangeRate;
import com.meridiantrust.sentinel.domain.HighRiskJurisdiction;
import com.meridiantrust.sentinel.dto.ExchangeRateUpsertRequest;
import com.meridiantrust.sentinel.dto.JurisdictionUpsertRequest;
import com.meridiantrust.sentinel.repository.ExchangeRateRepository;
import com.meridiantrust.sentinel.repository.HighRiskJurisdictionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin API for configurable reference data: FX rates (Business Rule 9) and the
 * high-risk / sanctions jurisdiction list (Business Rule 4). ADMIN only.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Configurable reference data: FX rates and high-risk jurisdictions")
public class AdminController {

    private final ExchangeRateRepository exchangeRateRepository;
    private final HighRiskJurisdictionRepository jurisdictionRepository;

    public AdminController(ExchangeRateRepository exchangeRateRepository,
                           HighRiskJurisdictionRepository jurisdictionRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.jurisdictionRepository = jurisdictionRepository;
    }

    @GetMapping("/exchange-rates")
    @Operation(summary = "List all configured FX rates")
    public List<ExchangeRate> exchangeRates() {
        return exchangeRateRepository.findAll();
    }

    @PutMapping("/exchange-rates/{currencyCode}")
    @Operation(summary = "Create or update the FX rate for a currency")
    public ExchangeRate upsertRate(@PathVariable String currencyCode,
                                   @Valid @RequestBody ExchangeRateUpsertRequest request) {
        String code = currencyCode.toUpperCase();
        ExchangeRate rate = exchangeRateRepository.findByCurrencyCode(code).orElseGet(ExchangeRate::new);
        rate.setCurrencyCode(code);
        rate.setRateToBase(request.rateToBase());
        return exchangeRateRepository.save(rate);
    }

    @GetMapping("/jurisdictions")
    @Operation(summary = "List active high-risk / sanctioned jurisdictions")
    public List<HighRiskJurisdiction> jurisdictions() {
        return jurisdictionRepository.findByActiveTrue();
    }

    @PostMapping("/jurisdictions")
    @Operation(summary = "Add or update a high-risk / sanctioned jurisdiction")
    public HighRiskJurisdiction upsertJurisdiction(@Valid @RequestBody JurisdictionUpsertRequest request) {
        String code = request.countryCode().toUpperCase();
        HighRiskJurisdiction j = jurisdictionRepository.findByCountryCodeIgnoreCaseAndActiveTrue(code)
                .orElseGet(HighRiskJurisdiction::new);
        j.setCountryCode(code);
        j.setCountryName(request.countryName());
        if (request.category() != null) {
            j.setCategory(request.category());
        }
        if (request.active() != null) {
            j.setActive(request.active());
        }
        j.setNotes(request.notes());
        return jurisdictionRepository.save(j);
    }
}
