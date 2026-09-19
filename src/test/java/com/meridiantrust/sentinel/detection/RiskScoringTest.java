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
package com.meridiantrust.sentinel.detection;

import static org.assertj.core.api.Assertions.assertThat;

import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.Direction;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskScoringTest {

    private final RiskScoring scoring = new RiskScoring();

    private RuleHit hit(int weight, Severity severity) {
        Customer c = TestFixtures.customer(1, "C1", RiskRating.LOW);
        Transaction t = TestFixtures.txn(TestFixtures.account(1, "A1", c), Direction.CREDIT, "1", Instant.now());
        return new RuleHit(RuleType.LARGE_TRANSACTION, "R1", weight, severity, "t", "e", "k", List.of(t));
    }

    @Test
    void addsRiskRatingModifier() {
        assertThat(scoring.scoreForHit(hit(60, Severity.MEDIUM), TestFixtures.customer(1, "C1", RiskRating.LOW)))
                .isEqualTo(60);
        assertThat(scoring.scoreForHit(hit(60, Severity.MEDIUM), TestFixtures.customer(1, "C1", RiskRating.MEDIUM)))
                .isEqualTo(67);
        assertThat(scoring.scoreForHit(hit(60, Severity.MEDIUM), TestFixtures.customer(1, "C1", RiskRating.HIGH)))
                .isEqualTo(75);
    }

    @Test
    void clampsAtHundred() {
        assertThat(scoring.scoreForHit(hit(95, Severity.CRITICAL), TestFixtures.customer(1, "C1", RiskRating.HIGH)))
                .isEqualTo(100);
    }

    @Test
    void combineTakesHigherPlusDiminishingLower() {
        assertThat(scoring.combine(80, 40)).isEqualTo(90); // 80 + 40/4
        assertThat(scoring.combine(40, 80)).isEqualTo(90); // order-independent
    }

    @Test
    void severityIsStrongerOfBandAndRule() {
        assertThat(scoring.severityFor(90, Severity.LOW)).isEqualTo(Severity.CRITICAL);
        assertThat(scoring.severityFor(30, Severity.HIGH)).isEqualTo(Severity.HIGH);
        assertThat(scoring.severityFor(70, Severity.LOW)).isEqualTo(Severity.HIGH);
    }
}
