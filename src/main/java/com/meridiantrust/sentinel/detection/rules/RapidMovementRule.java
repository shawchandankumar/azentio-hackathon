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
import com.meridiantrust.sentinel.domain.enums.Direction;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.service.RuleConfigService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Business Rule 3 — Rapid movement of funds (layering). When a sizeable credit
 * lands and ≥ a configured ratio (default 80%) of it is moved out within a short
 * window (default 48h), the account is flagged.
 */
@Component
public class RapidMovementRule implements DetectionRule {

    private static final BigDecimal DEFAULT_MIN_CREDIT = new BigDecimal("415980.00");

    private final RuleConfigService ruleConfigService;
    private final TransactionRepository transactionRepository;

    public RapidMovementRule(RuleConfigService ruleConfigService, TransactionRepository transactionRepository) {
        this.ruleConfigService = ruleConfigService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RuleType type() {
        return RuleType.RAPID_MOVEMENT;
    }

    @Override
    public List<RuleHit> evaluate(Transaction txn, RuleConfig config) {
        // This pattern is only realised once value flows OUT; evaluate on debits.
        if (txn.getDirection() != Direction.DEBIT) {
            return List.of();
        }

        RuleParams params = ruleConfigService.params(config);
        long windowHours = params.getLong("windowHours", 48);
        double outflowRatio = params.getDouble("outflowRatio", 0.80);
        BigDecimal minCredit = params.getDecimal("minCreditBase", DEFAULT_MIN_CREDIT);

        Long accountId = txn.getAccount().getId();
        Instant windowStart = txn.getBookedAt().minus(Duration.ofHours(windowHours));

        List<Transaction> credits = transactionRepository
                .findByAccountIdAndDirectionAndBookedAtBetweenOrderByBookedAtAsc(
                        accountId, Direction.CREDIT, windowStart, txn.getBookedAt());

        List<RuleHit> hits = new ArrayList<>();
        for (Transaction credit : credits) {
            if (credit.getBaseAmount().compareTo(minCredit) < 0) {
                continue;
            }
            Instant outEnd = credit.getBookedAt().plus(Duration.ofHours(windowHours));
            Instant effectiveEnd = outEnd.isBefore(txn.getBookedAt()) ? outEnd : txn.getBookedAt();

            List<Transaction> outflows = transactionRepository
                    .findByAccountIdAndDirectionAndBookedAtBetweenOrderByBookedAtAsc(
                            accountId, Direction.DEBIT, credit.getBookedAt(), effectiveEnd);

            BigDecimal totalOut = outflows.stream()
                    .map(Transaction::getBaseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal required = credit.getBaseAmount()
                    .multiply(BigDecimal.valueOf(outflowRatio));

            if (totalOut.compareTo(required) >= 0) {
                double pct = totalOut.doubleValue() / credit.getBaseAmount().doubleValue() * 100.0;
                String explanation = String.format(
                        "Credit %s of %s base into account %s was followed by outbound transfers totalling %s base "
                                + "(%.0f%%) within %d hours — indicative of layering / pass-through activity.",
                        credit.getTxnRef(), credit.getBaseAmount().toPlainString(),
                        txn.getAccount().getAccountNumber(), totalOut.toPlainString(), pct, windowHours);

                List<Transaction> evidence = new ArrayList<>();
                evidence.add(credit);
                evidence.addAll(outflows);

                hits.add(new RuleHit(
                        type(), config.getRuleCode(), config.getBaseWeight(), config.getSeverity(),
                        "Rapid movement of funds (layering)",
                        explanation,
                        "RAPID:" + accountId + ":" + credit.getTxnRef(),
                        evidence));
            }
        }
        return hits;
    }
}
