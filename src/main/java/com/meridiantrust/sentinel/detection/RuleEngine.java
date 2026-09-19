package com.meridiantrust.sentinel.detection;

import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.service.RuleConfigService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the configured {@link DetectionRule}s against a transaction.
 * Only rules that are enabled in {@code rule_config} are evaluated, so rules can
 * be toggled and retuned at runtime. A failure in one rule never aborts the others.
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<DetectionRule> rules;
    private final RuleConfigService ruleConfigService;

    public RuleEngine(List<DetectionRule> rules, RuleConfigService ruleConfigService) {
        this.rules = rules;
        this.ruleConfigService = ruleConfigService;
    }

    /** Evaluate every enabled rule against a transaction and collect all hits. */
    public List<RuleHit> evaluate(Transaction txn) {
        List<RuleHit> hits = new ArrayList<>();
        for (DetectionRule rule : rules) {
            Optional<RuleConfig> config = ruleConfigService.findEnabledByType(rule.type());
            if (config.isEmpty()) {
                continue; // rule disabled or unconfigured
            }
            try {
                hits.addAll(rule.evaluate(txn, config.get()));
            } catch (Exception e) {
                log.error("Rule {} failed evaluating txn {}: {}",
                        rule.type(), txn.getTxnRef(), e.getMessage(), e);
            }
        }
        return hits;
    }
}
