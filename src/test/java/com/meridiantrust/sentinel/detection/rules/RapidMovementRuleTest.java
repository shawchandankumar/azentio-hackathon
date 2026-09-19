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

class RapidMovementRuleTest {

    private final RuleConfigService ruleConfigService =
            new RuleConfigService(Mockito.mock(RuleConfigRepository.class), new ObjectMapper());
    private final TransactionRepository txnRepo = Mockito.mock(TransactionRepository.class);
    private final RapidMovementRule rule = new RapidMovementRule(ruleConfigService, txnRepo);
    private final RuleConfig config = TestFixtures.config(
            "R3_RAPID_MOVEMENT", RuleType.RAPID_MOVEMENT, 85, Severity.HIGH,
            "{\"windowHours\": 48, \"outflowRatio\": 0.80, \"minCreditBase\": 415980.00}");

    private Customer customer;
    private Account account;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.customer(1, "C1", RiskRating.HIGH);
        account = TestFixtures.account(1, "A1", customer);
    }

    @Test
    void firesWhenMostOfCreditMovedOutWithinWindow() {
        Instant now = Instant.now();
        Transaction credit = TestFixtures.txn(account, Direction.CREDIT, "1000000", now.minusSeconds(3600 * 10));
        Transaction out1 = TestFixtures.txn(account, Direction.DEBIT, "500000", now.minusSeconds(3600 * 5));
        Transaction out2 = TestFixtures.txn(account, Direction.DEBIT, "400000", now);

        when(txnRepo.findByAccountIdAndDirectionAndBookedAtBetweenOrderByBookedAtAsc(
                eq(1L), eq(Direction.CREDIT), any(), any())).thenReturn(List.of(credit));
        when(txnRepo.findByAccountIdAndDirectionAndBookedAtBetweenOrderByBookedAtAsc(
                eq(1L), eq(Direction.DEBIT), any(), any())).thenReturn(List.of(out1, out2));

        List<RuleHit> hits = rule.evaluate(out2, config);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRuleType()).isEqualTo(RuleType.RAPID_MOVEMENT);
        assertThat(hits.get(0).getEvidence()).contains(credit, out1, out2);
    }

    @Test
    void doesNotFireWhenOutflowBelowRatio() {
        Instant now = Instant.now();
        Transaction credit = TestFixtures.txn(account, Direction.CREDIT, "1000000", now.minusSeconds(3600 * 10));
        Transaction out1 = TestFixtures.txn(account, Direction.DEBIT, "300000", now);

        when(txnRepo.findByAccountIdAndDirectionAndBookedAtBetweenOrderByBookedAtAsc(
                eq(1L), eq(Direction.CREDIT), any(), any())).thenReturn(List.of(credit));
        when(txnRepo.findByAccountIdAndDirectionAndBookedAtBetweenOrderByBookedAtAsc(
                eq(1L), eq(Direction.DEBIT), any(), any())).thenReturn(List.of(out1));

        assertThat(rule.evaluate(out1, config)).isEmpty();
    }

    @Test
    void doesNotFireOnCreditTransactions() {
        Transaction credit = TestFixtures.txn(account, Direction.CREDIT, "1000000", Instant.now());
        assertThat(rule.evaluate(credit, config)).isEmpty();
    }

    @Test
    void doesNotFireWhenCreditBelowMinimum() {
        Instant now = Instant.now();
        Transaction smallCredit = TestFixtures.txn(account, Direction.CREDIT, "100000", now.minusSeconds(3600));
        Transaction out1 = TestFixtures.txn(account, Direction.DEBIT, "95000", now);

        when(txnRepo.findByAccountIdAndDirectionAndBookedAtBetweenOrderByBookedAtAsc(
                eq(1L), eq(Direction.CREDIT), any(), any())).thenReturn(List.of(smallCredit));

        assertThat(rule.evaluate(out1, config)).isEmpty();
    }
}
