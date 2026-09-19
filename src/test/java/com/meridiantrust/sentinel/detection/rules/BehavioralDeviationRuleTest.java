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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BehavioralDeviationRuleTest {

    private final RuleConfigService ruleConfigService =
            new RuleConfigService(Mockito.mock(RuleConfigRepository.class), new ObjectMapper());
    private final TransactionRepository txnRepo = Mockito.mock(TransactionRepository.class);
    private final BehavioralDeviationRule rule = new BehavioralDeviationRule(ruleConfigService, txnRepo);
    private final RuleConfig config = TestFixtures.config(
            "R5_BEHAVIORAL_DEVIATION", RuleType.BEHAVIORAL_DEVIATION, 70, Severity.HIGH,
            "{\"lookbackDays\": 90, \"multiplier\": 3.0, \"minBaselineBase\": 41598.00}");

    private Customer customer;
    private Account account;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.customer(1, "C1", RiskRating.LOW);
        account = TestFixtures.account(1, "A1", customer);
    }

    @Test
    void firesWhenTodayExceedsThreeTimesBaseline() {
        Instant now = Instant.now();
        List<Transaction> history = new ArrayList<>();
        for (int d = 2; d <= 8; d++) { // seven baseline days
            history.add(TestFixtures.txn(account, Direction.DEBIT, "61234", now.minus(Duration.ofDays(d))));
        }
        Transaction spike = TestFixtures.txn(account, Direction.DEBIT, "254321", now);
        history.add(spike);

        when(txnRepo.findByCustomerAndWindow(eq(1L), any(), any())).thenReturn(history);

        List<RuleHit> hits = rule.evaluate(spike, config);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRuleType()).isEqualTo(RuleType.BEHAVIORAL_DEVIATION);
        assertThat(hits.get(0).getEvidence()).contains(spike);
    }

    @Test
    void doesNotFireWithoutBaseline() {
        Instant now = Instant.now();
        Transaction only = TestFixtures.txn(account, Direction.DEBIT, "254321", now);
        when(txnRepo.findByCustomerAndWindow(eq(1L), any(), any())).thenReturn(List.of(only));
        assertThat(rule.evaluate(only, config)).isEmpty();
    }

    @Test
    void doesNotFireWhenWithinNormalRange() {
        Instant now = Instant.now();
        List<Transaction> history = new ArrayList<>();
        for (int d = 2; d <= 8; d++) {
            history.add(TestFixtures.txn(account, Direction.DEBIT, "61234", now.minus(Duration.ofDays(d))));
        }
        Transaction normal = TestFixtures.txn(account, Direction.DEBIT, "70000", now);
        history.add(normal);
        when(txnRepo.findByCustomerAndWindow(eq(1L), any(), any())).thenReturn(history);
        assertThat(rule.evaluate(normal, config)).isEmpty();
    }
}
