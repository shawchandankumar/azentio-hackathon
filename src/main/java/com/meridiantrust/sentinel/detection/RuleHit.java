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

import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import java.util.ArrayList;
import java.util.List;

/**
 * A single triggered detection produced by a {@link DetectionRule}. Converted
 * into (or merged into) an {@code Alert} by the detection service.
 */
public class RuleHit {

    private final RuleType ruleType;
    private final String ruleCode;
    private final int baseWeight;
    private final Severity severity;
    private final String title;
    private final String explanation;
    private final String dedupKey;
    private final List<Transaction> evidence;

    public RuleHit(RuleType ruleType, String ruleCode, int baseWeight, Severity severity,
                   String title, String explanation, String dedupKey, List<Transaction> evidence) {
        this.ruleType = ruleType;
        this.ruleCode = ruleCode;
        this.baseWeight = baseWeight;
        this.severity = severity;
        this.title = title;
        this.explanation = explanation;
        this.dedupKey = dedupKey;
        this.evidence = new ArrayList<>(evidence);
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public int getBaseWeight() {
        return baseWeight;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public List<Transaction> getEvidence() {
        return evidence;
    }
}
