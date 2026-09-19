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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.detection.TestFixtures;
import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.Direction;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import com.meridiantrust.sentinel.repository.RuleConfigRepository;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.service.RuleConfigService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RoundNumberRuleTest {

    private final RuleConfigService ruleConfigService =
            new RuleConfigService(Mockito.mock(RuleConfigRepository.class), new ObjectMapper());
    private final TransactionRepository txnRepo = Mockito.mock(TransactionRepository.class);
    private final RoundNumberRule rule = new RoundNumberRule(ruleConfigService, txnRepo);
    private final RuleConfig config = TestFixtures.config(
            "R6_ROUND_NUMBER", RuleType.ROUND_NUMBER, 40, Severity.LOW,
            "{\"windowHours\": 72, \"minCount\": 3, \"roundUnit\": 1000}");

    private Customer customer;
    private Account account;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.customer(1, "C1", RiskRating.MEDIUM);
        account = TestFixtures.account(1, "A1", customer);
    }

    @Test
    void firesOnThreeOrMoreRoundTransactions() {
        Instant now = Instant.now();
        Transaction t1 = TestFixtures.txn(account, Direction.DEBIT, "500000", now.minusSeconds(3600 * 40));
        Transaction t2 = TestFixtures.txn(account, Direction.DEBIT, "500000", now.minusSeconds(3600 * 20));
        Transaction t3 = TestFixtures.txn(account, Direction.DEBIT, "500000", now);
        when(txnRepo.findByAccountIdAndBookedAtBetweenOrderByBookedAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of(t1, t2, t3));

        List<RuleHit> hits = rule.evaluate(t3, config);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRuleType()).isEqualTo(RuleType.ROUND_NUMBER);
        assertThat(hits.get(0).getEvidence()).containsExactly(t1, t2, t3);
    }

    @Test
    void doesNotFireWhenTriggeringTxnNotRound() {
        Transaction notRound = TestFixtures.txn(account, Direction.DEBIT, "499500", Instant.now());
        assertThat(rule.evaluate(notRound, config)).isEmpty();
    }

    @Test
    void doesNotFireBelowMinCount() {
        Instant now = Instant.now();
        Transaction t1 = TestFixtures.txn(account, Direction.DEBIT, "500000", now.minusSeconds(3600));
        Transaction t2 = TestFixtures.txn(account, Direction.DEBIT, "500000", now);
        when(txnRepo.findByAccountIdAndBookedAtBetweenOrderByBookedAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of(t1, t2));
        assertThat(rule.evaluate(t2, config)).isEmpty();
    }
}
