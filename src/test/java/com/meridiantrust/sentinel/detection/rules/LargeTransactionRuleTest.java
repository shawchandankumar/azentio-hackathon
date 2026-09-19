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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.detection.TestFixtures;
import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.Direction;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import com.meridiantrust.sentinel.repository.RuleConfigRepository;
import com.meridiantrust.sentinel.service.RuleConfigService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LargeTransactionRuleTest {

    private final RuleConfigService ruleConfigService =
            new RuleConfigService(Mockito.mock(RuleConfigRepository.class), new ObjectMapper());
    private final LargeTransactionRule rule = new LargeTransactionRule(ruleConfigService);
    private final RuleConfig config = TestFixtures.config(
            "R1_LARGE_TXN", RuleType.LARGE_TRANSACTION, 60, Severity.MEDIUM, "{\"thresholdBase\": 831960.00}");

    private final Customer customer = TestFixtures.customer(1, "C1", com.meridiantrust.sentinel.domain.enums.RiskRating.LOW);
    private final Account account = TestFixtures.account(1, "A1", customer);

    @Test
    void firesAtOrAboveThreshold() {
        Transaction txn = TestFixtures.txn(account, Direction.CREDIT, "900000", Instant.now());
        List<RuleHit> hits = rule.evaluate(txn, config);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRuleType()).isEqualTo(RuleType.LARGE_TRANSACTION);
        assertThat(hits.get(0).getEvidence()).containsExactly(txn);
        assertThat(hits.get(0).getDedupKey()).isEqualTo("LARGE:" + txn.getTxnRef());
    }

    @Test
    void doesNotFireBelowThreshold() {
        Transaction txn = TestFixtures.txn(account, Direction.CREDIT, "500000", Instant.now());
        assertThat(rule.evaluate(txn, config)).isEmpty();
    }

    @Test
    void firesExactlyAtThreshold() {
        Transaction txn = TestFixtures.txn(account, Direction.CREDIT, "831960.00", Instant.now());
        assertThat(rule.evaluate(txn, config)).hasSize(1);
    }
}
