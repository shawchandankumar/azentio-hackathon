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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.meridiantrust.sentinel.domain.ExchangeRate;
import com.meridiantrust.sentinel.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CurrencyServiceTest {

    private final ExchangeRateRepository repo = Mockito.mock(ExchangeRateRepository.class);
    private final CurrencyService service = new CurrencyService(repo, "INR");

    @Test
    void returnsSameAmountForBaseCurrency() {
        assertThat(service.toBase(new BigDecimal("1000"), "INR")).isEqualByComparingTo("1000.00");
    }

    @Test
    void convertsUsingConfiguredRate() {
        ExchangeRate usd = new ExchangeRate();
        usd.setCurrencyCode("USD");
        usd.setRateToBase(new BigDecimal("83.20"));
        when(repo.findByCurrencyCode("USD")).thenReturn(Optional.of(usd));

        assertThat(service.toBase(new BigDecimal("100"), "USD")).isEqualByComparingTo("8320.00");
    }

    @Test
    void throwsWhenRateMissing() {
        when(repo.findByCurrencyCode("ZZZ")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.toBase(new BigDecimal("100"), "ZZZ"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
