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

class StructuringRuleTest {

    private final RuleConfigService ruleConfigService =
            new RuleConfigService(Mockito.mock(RuleConfigRepository.class), new ObjectMapper());
    private final TransactionRepository txnRepo = Mockito.mock(TransactionRepository.class);
    private final StructuringRule rule = new StructuringRule(ruleConfigService, txnRepo);
    private final RuleConfig config = TestFixtures.config(
            "R2_STRUCTURING", RuleType.STRUCTURING, 80, Severity.HIGH,
            "{\"windowHours\": 24, \"minCount\": 3, \"lowerBase\": 748764.00, \"upperBase\": 831876.00}");

    private Customer customer;
    private Account account;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.customer(1, "C1", RiskRating.MEDIUM);
        account = TestFixtures.account(1, "A1", customer);
    }

    @Test
    void firesWhenThreeOrMoreSubThresholdTxnsInWindow() {
        Instant now = Instant.now();
        Transaction t1 = TestFixtures.txn(account, Direction.CREDIT, "799500", now.minusSeconds(3600 * 6));
        Transaction t2 = TestFixtures.txn(account, Direction.CREDIT, "810250", now.minusSeconds(3600 * 3));
        Transaction t3 = TestFixtures.txn(account, Direction.CREDIT, "822100", now);
        when(txnRepo.findByAccountIdAndBookedAtBetweenOrderByBookedAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of(t1, t2, t3));

        List<RuleHit> hits = rule.evaluate(t3, config);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRuleType()).isEqualTo(RuleType.STRUCTURING);
        assertThat(hits.get(0).getEvidence()).containsExactly(t1, t2, t3);
    }

    @Test
    void doesNotFireWithFewerThanMinCount() {
        Instant now = Instant.now();
        Transaction t1 = TestFixtures.txn(account, Direction.CREDIT, "799500", now.minusSeconds(3600));
        Transaction t2 = TestFixtures.txn(account, Direction.CREDIT, "810250", now);
        when(txnRepo.findByAccountIdAndBookedAtBetweenOrderByBookedAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of(t1, t2));

        assertThat(rule.evaluate(t2, config)).isEmpty();
    }

    @Test
    void doesNotFireWhenTriggeringTxnOutsideBand() {
        Transaction big = TestFixtures.txn(account, Direction.CREDIT, "900000", Instant.now());
        assertThat(rule.evaluate(big, config)).isEmpty();
    }

    @Test
    void ignoresTxnsOutsideBandWhenCounting() {
        Instant now = Instant.now();
        Transaction inBand1 = TestFixtures.txn(account, Direction.CREDIT, "799500", now.minusSeconds(7200));
        Transaction outOfBand = TestFixtures.txn(account, Direction.CREDIT, "100000", now.minusSeconds(3600));
        Transaction inBand2 = TestFixtures.txn(account, Direction.CREDIT, "810250", now);
        when(txnRepo.findByAccountIdAndBookedAtBetweenOrderByBookedAtAsc(eq(1L), any(), any()))
                .thenReturn(List.of(inBand1, outOfBand, inBand2));

        // Only two in-band -> below minCount of 3
        assertThat(rule.evaluate(inBand2, config)).isEmpty();
    }
}
