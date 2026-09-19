package com.meridiantrust.sentinel.detection.rules;

import com.meridiantrust.sentinel.detection.DetectionRule;
import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.detection.RuleParams;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.service.RuleConfigService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Business Rule 1 — CTR-style threshold. Any single transaction whose value
 * (normalized to base currency) is at or above the reporting threshold is flagged.
 */
@Component
public class LargeTransactionRule implements DetectionRule {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("831960.00");

    private final RuleConfigService ruleConfigService;

    public LargeTransactionRule(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @Override
    public RuleType type() {
        return RuleType.LARGE_TRANSACTION;
    }

    @Override
    public List<RuleHit> evaluate(Transaction txn, RuleConfig config) {
        RuleParams params = ruleConfigService.params(config);
        BigDecimal threshold = params.getDecimal("thresholdBase", DEFAULT_THRESHOLD);

        if (txn.getBaseAmount().compareTo(threshold) < 0) {
            return List.of();
        }

        String explanation = String.format(
                "Transaction %s of %s %s (≈ %s base) is at or above the reporting threshold of %s base currency, "
                        + "requiring automatic review (CTR-style).",
                txn.getTxnRef(), txn.getAmount().toPlainString(), txn.getCurrency(),
                txn.getBaseAmount().toPlainString(), threshold.toPlainString());

        RuleHit hit = new RuleHit(
                type(), config.getRuleCode(), config.getBaseWeight(), config.getSeverity(),
                "Large transaction above reporting threshold",
                explanation,
                "LARGE:" + txn.getTxnRef(),
                List.of(txn));
        return List.of(hit);
    }
}
