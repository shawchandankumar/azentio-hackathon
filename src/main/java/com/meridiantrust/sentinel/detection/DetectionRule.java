package com.meridiantrust.sentinel.detection;

import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import java.util.List;

/**
 * A configurable AML detection rule. Implementations are stateless Spring beans;
 * the engine invokes {@link #evaluate} for each incoming transaction with the
 * rule's live configuration so thresholds can change without a redeploy.
 */
public interface DetectionRule {

    /** The typology this rule detects. */
    RuleType type();

    /**
     * Evaluate a transaction against this rule.
     *
     * @param txn    the transaction under evaluation
     * @param config the live, enabled configuration for this rule
     * @return zero or more triggered hits (empty if nothing fired)
     */
    List<RuleHit> evaluate(Transaction txn, RuleConfig config);
}
