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
