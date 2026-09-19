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
