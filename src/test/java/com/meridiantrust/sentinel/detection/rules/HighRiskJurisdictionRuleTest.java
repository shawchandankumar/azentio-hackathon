package com.meridiantrust.sentinel.detection.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.detection.TestFixtures;
import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.HighRiskJurisdiction;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.WatchlistCounterparty;
import com.meridiantrust.sentinel.domain.enums.Direction;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import com.meridiantrust.sentinel.service.ReferenceDataService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HighRiskJurisdictionRuleTest {

    private final ReferenceDataService refData = Mockito.mock(ReferenceDataService.class);
    private final HighRiskJurisdictionRule rule = new HighRiskJurisdictionRule(refData);
    private final RuleConfig config = TestFixtures.config(
            "R4_HIGH_RISK_JURISDICTION", RuleType.HIGH_RISK_JURISDICTION, 90, Severity.CRITICAL, "{}");

    private Transaction txn;

    @BeforeEach
    void setUp() {
        Customer customer = TestFixtures.customer(1, "C1", RiskRating.MEDIUM);
        Account account = TestFixtures.account(1, "A1", customer);
        txn = TestFixtures.txn(account, Direction.DEBIT, "200000", Instant.now());
        when(refData.matchJurisdiction(any())).thenReturn(Optional.empty());
        when(refData.matchCounterparty(any())).thenReturn(Optional.empty());
    }

    @Test
    void firesOnHighRiskCounterpartyCountry() {
        txn.setCounterpartyCountry("IR");
        HighRiskJurisdiction iran = new HighRiskJurisdiction();
        iran.setCountryCode("IR");
        iran.setCountryName("Iran");
        iran.setCategory("SANCTIONED");
        when(refData.matchJurisdiction("IR")).thenReturn(Optional.of(iran));

        var hits = rule.evaluate(txn, config);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRuleType()).isEqualTo(RuleType.HIGH_RISK_JURISDICTION);
        assertThat(hits.get(0).getEvidence()).containsExactly(txn);
    }

    @Test
    void firesOnWatchlistedCounterparty() {
        txn.setCounterpartyName("Crescent Trading FZE");
        WatchlistCounterparty w = new WatchlistCounterparty();
        w.setName("Crescent Trading FZE");
        w.setCategory("SANCTIONED");
        when(refData.matchCounterparty("Crescent Trading FZE")).thenReturn(Optional.of(w));

        assertThat(rule.evaluate(txn, config)).hasSize(1);
    }

    @Test
    void doesNotFireForCleanTransaction() {
        // clean txn: no matches configured in setUp
        assertThat(rule.evaluate(txn, config)).isEmpty();
    }
}
