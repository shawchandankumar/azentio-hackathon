package com.meridiantrust.sentinel.detection;

import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import com.meridiantrust.sentinel.domain.enums.Severity;
import org.springframework.stereotype.Component;

/**
 * Derives a 0–100 risk score from a rule hit and combines scores when multiple
 * rules reinforce the same alert (Business Rule 7). Higher scores sort to the
 * top of the analyst queue.
 */
@Component
public class RiskScoring {

    /** Base score for a single hit, adjusted by the customer's KYC risk rating. */
    public int scoreForHit(RuleHit hit, Customer customer) {
        int score = hit.getBaseWeight();
        score += riskModifier(customer == null ? null : customer.getRiskRating());
        return clamp(score);
    }

    /**
     * Combine an existing alert score with a reinforcing hit's score: take the
     * higher of the two and add a diminishing fraction of the lower, so multiple
     * typologies raise the score without trivially saturating at 100.
     */
    public int combine(int existing, int addition) {
        int higher = Math.max(existing, addition);
        int lower = Math.min(existing, addition);
        return clamp(higher + (lower / 4));
    }

    /** Final severity is the stronger of the rule's declared severity and the score band. */
    public Severity severityFor(int score, Severity ruleSeverity) {
        Severity band = severityForScore(score);
        return band.ordinal() >= ruleSeverity.ordinal() ? band : ruleSeverity;
    }

    private Severity severityForScore(int score) {
        if (score >= 85) {
            return Severity.CRITICAL;
        }
        if (score >= 65) {
            return Severity.HIGH;
        }
        if (score >= 40) {
            return Severity.MEDIUM;
        }
        return Severity.LOW;
    }

    private int riskModifier(RiskRating rating) {
        if (rating == null) {
            return 0;
        }
        return switch (rating) {
            case HIGH -> 15;
            case MEDIUM -> 7;
            case LOW -> 0;
        };
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
