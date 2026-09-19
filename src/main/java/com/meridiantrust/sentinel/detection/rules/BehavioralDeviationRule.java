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
package com.meridiantrust.sentinel.detection.rules;

import com.meridiantrust.sentinel.detection.DetectionRule;
import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.detection.RuleParams;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.service.RuleConfigService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Business Rule 5 — Behavioral deviation. A customer's daily transaction value
 * that exceeds a configured multiple (default 3x) of their rolling
 * (default 90-day) daily average is flagged.
 */
@Component
public class BehavioralDeviationRule implements DetectionRule {

    private static final BigDecimal DEFAULT_MIN_BASELINE = new BigDecimal("41598.00");

    private final RuleConfigService ruleConfigService;
    private final TransactionRepository transactionRepository;

    public BehavioralDeviationRule(RuleConfigService ruleConfigService,
                                   TransactionRepository transactionRepository) {
        this.ruleConfigService = ruleConfigService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RuleType type() {
        return RuleType.BEHAVIORAL_DEVIATION;
    }

    @Override
    public List<RuleHit> evaluate(Transaction txn, RuleConfig config) {
        RuleParams params = ruleConfigService.params(config);
        long lookbackDays = params.getLong("lookbackDays", 90);
        double multiplier = params.getDouble("multiplier", 3.0);
        BigDecimal minBaseline = params.getDecimal("minBaselineBase", DEFAULT_MIN_BASELINE);

        Long customerId = txn.getAccount().getCustomer().getId();
        LocalDate today = txn.getBookedAt().atZone(ZoneOffset.UTC).toLocalDate();
        Instant windowStart = txn.getBookedAt().minus(Duration.ofDays(lookbackDays));

        List<Transaction> history = transactionRepository
                .findByCustomerAndWindow(customerId, windowStart, txn.getBookedAt());

        // Aggregate value per UTC day.
        Map<LocalDate, BigDecimal> dailyTotals = new HashMap<>();
        List<Transaction> todaysTxns = new ArrayList<>();
        for (Transaction t : history) {
            LocalDate day = t.getBookedAt().atZone(ZoneOffset.UTC).toLocalDate();
            dailyTotals.merge(day, t.getBaseAmount(), BigDecimal::add);
            if (day.equals(today)) {
                todaysTxns.add(t);
            }
        }

        BigDecimal todayTotal = dailyTotals.getOrDefault(today, BigDecimal.ZERO);

        // Baseline = average daily value over prior active days (excluding today).
        BigDecimal priorSum = BigDecimal.ZERO;
        int priorActiveDays = 0;
        for (Map.Entry<LocalDate, BigDecimal> e : dailyTotals.entrySet()) {
            if (!e.getKey().equals(today)) {
                priorSum = priorSum.add(e.getValue());
                priorActiveDays++;
            }
        }
        if (priorActiveDays == 0) {
            return List.of(); // no baseline yet
        }

        BigDecimal baseline = priorSum.divide(BigDecimal.valueOf(priorActiveDays), 2, RoundingMode.HALF_UP);
        if (baseline.compareTo(minBaseline) < 0) {
            return List.of(); // baseline too small to be meaningful — avoid noise
        }

        BigDecimal threshold = baseline.multiply(BigDecimal.valueOf(multiplier));
        if (todayTotal.compareTo(threshold) <= 0) {
            return List.of();
        }

        double ratio = todayTotal.doubleValue() / baseline.doubleValue();
        String explanation = String.format(
                "Customer %s transacted %s base on %s versus a %d-day average of %s base (%.1fx, threshold %.1fx) "
                        + "— a significant behavioral deviation.",
                txn.getAccount().getCustomer().getCustomerRef(), todayTotal.toPlainString(), today,
                lookbackDays, baseline.toPlainString(), ratio, multiplier);

        RuleHit hit = new RuleHit(
                type(), config.getRuleCode(), config.getBaseWeight(), config.getSeverity(),
                "Unusual daily volume vs customer baseline",
                explanation,
                "BEHAV:" + customerId + ":" + today,
                todaysTxns);
        return List.of(hit);
    }
}
