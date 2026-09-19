package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.ExchangeRate;
import com.meridiantrust.sentinel.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Normalizes monetary amounts to the configured base currency (Business Rule 9)
 * using the tunable {@code exchange_rates} table.
 */
@Service
public class CurrencyService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final String baseCurrency;

    public CurrencyService(ExchangeRateRepository exchangeRateRepository,
                           @Value("${sentinel.base-currency:INR}") String baseCurrency) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.baseCurrency = baseCurrency.toUpperCase();
    }

    public String baseCurrency() {
        return baseCurrency;
    }

    /**
     * Convert an amount in {@code currency} to the base currency.
     *
     * @throws IllegalArgumentException if no exchange rate is configured for the currency
     */
    @Transactional(readOnly = true)
    public BigDecimal toBase(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        String code = currency == null ? baseCurrency : currency.toUpperCase();
        if (code.equals(baseCurrency)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        ExchangeRate rate = exchangeRateRepository.findByCurrencyCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No exchange rate configured for currency: " + code));
        return amount.multiply(rate.getRateToBase()).setScale(2, RoundingMode.HALF_UP);
    }
}
