package com.meridiantrust.sentinel.detection.rules;

import com.meridiantrust.sentinel.detection.DetectionRule;
import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.detection.RuleParams;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.service.RuleConfigService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Business Rule 2 — Structuring / smurfing. Three or more transactions from the
 * same account within a rolling 24-hour window that each fall just below the
 * reporting threshold (e.g. $9,000–$9,999 equivalent) trigger a structuring alert.
 */
@Component
public class StructuringRule implements DetectionRule {

    private static final BigDecimal DEFAULT_LOWER = new BigDecimal("748764.00");
    private static final BigDecimal DEFAULT_UPPER = new BigDecimal("831876.00");

    private final RuleConfigService ruleConfigService;
    private final TransactionRepository transactionRepository;

    public StructuringRule(RuleConfigService ruleConfigService, TransactionRepository transactionRepository) {
        this.ruleConfigService = ruleConfigService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RuleType type() {
        return RuleType.STRUCTURING;
    }

    @Override
    public List<RuleHit> evaluate(Transaction txn, RuleConfig config) {
        RuleParams params = ruleConfigService.params(config);
        long windowHours = params.getLong("windowHours", 24);
        int minCount = params.getInt("minCount", 3);
        BigDecimal lower = params.getDecimal("lowerBase", DEFAULT_LOWER);
        BigDecimal upper = params.getDecimal("upperBase", DEFAULT_UPPER);

        if (!inBand(txn.getBaseAmount(), lower, upper)) {
            return List.of();
        }

        Instant windowStart = txn.getBookedAt().minus(Duration.ofHours(windowHours));
        List<Transaction> window = transactionRepository
                .findByAccountIdAndBookedAtBetweenOrderByBookedAtAsc(
                        txn.getAccount().getId(), windowStart, txn.getBookedAt());

        List<Transaction> justBelow = new ArrayList<>();
        for (Transaction t : window) {
            if (inBand(t.getBaseAmount(), lower, upper)) {
                justBelow.add(t);
            }
        }

        if (justBelow.size() < minCount) {
            return List.of();
        }

        BigDecimal total = justBelow.stream()
                .map(Transaction::getBaseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String explanation = String.format(
                "%d transactions from account %s within %d hours each fell just below the reporting threshold "
                        + "(band %s–%s base), totalling %s base — a classic structuring/smurfing pattern.",
                justBelow.size(), txn.getAccount().getAccountNumber(), windowHours,
                lower.toPlainString(), upper.toPlainString(), total.toPlainString());

        String bucket = txn.getBookedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
        RuleHit hit = new RuleHit(
                type(), config.getRuleCode(), config.getBaseWeight(), config.getSeverity(),
                "Possible structuring across multiple sub-threshold transactions",
                explanation,
                "STRUCT:" + txn.getAccount().getId() + ":" + bucket,
                justBelow);
        return List.of(hit);
    }

    private static boolean inBand(BigDecimal value, BigDecimal lower, BigDecimal upper) {
        return value.compareTo(lower) >= 0 && value.compareTo(upper) <= 0;
    }
}
