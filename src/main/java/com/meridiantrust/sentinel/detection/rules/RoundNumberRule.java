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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Round-number / just-below-threshold pattern. Repeated suspiciously round-value
 * transactions (exact multiples of a configured unit) from the same account
 * within a rolling window are flagged as potential engineered amounts.
 */
@Component
public class RoundNumberRule implements DetectionRule {

    private static final BigDecimal DEFAULT_ROUND_UNIT = new BigDecimal("1000");

    private final RuleConfigService ruleConfigService;
    private final TransactionRepository transactionRepository;

    public RoundNumberRule(RuleConfigService ruleConfigService, TransactionRepository transactionRepository) {
        this.ruleConfigService = ruleConfigService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RuleType type() {
        return RuleType.ROUND_NUMBER;
    }

    @Override
    public List<RuleHit> evaluate(Transaction txn, RuleConfig config) {
        RuleParams params = ruleConfigService.params(config);
        long windowHours = params.getLong("windowHours", 72);
        int minCount = params.getInt("minCount", 3);
        BigDecimal roundUnit = params.getDecimal("roundUnit", DEFAULT_ROUND_UNIT);

        if (!isRound(txn.getAmount(), roundUnit)) {
            return List.of();
        }

        Instant windowStart = txn.getBookedAt().minus(Duration.ofHours(windowHours));
        List<Transaction> window = transactionRepository
                .findByAccountIdAndBookedAtBetweenOrderByBookedAtAsc(
                        txn.getAccount().getId(), windowStart, txn.getBookedAt());

        List<Transaction> roundTxns = new ArrayList<>();
        for (Transaction t : window) {
            if (isRound(t.getAmount(), roundUnit)) {
                roundTxns.add(t);
            }
        }

        if (roundTxns.size() < minCount) {
            return List.of();
        }

        String explanation = String.format(
                "%d round-value transactions (exact multiples of %s) from account %s within %d hours — "
                        + "amounts appear engineered rather than organic.",
                roundTxns.size(), roundUnit.toPlainString(), txn.getAccount().getAccountNumber(), windowHours);

        String bucket = txn.getBookedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
        RuleHit hit = new RuleHit(
                type(), config.getRuleCode(), config.getBaseWeight(), config.getSeverity(),
                "Repeated round-number transactions",
                explanation,
                "ROUND:" + txn.getAccount().getId() + ":" + bucket,
                roundTxns);
        return List.of(hit);
    }

    private static boolean isRound(BigDecimal amount, BigDecimal unit) {
        if (amount == null || amount.signum() <= 0 || unit.signum() <= 0) {
            return false;
        }
        return amount.remainder(unit).compareTo(BigDecimal.ZERO) == 0;
    }
}
