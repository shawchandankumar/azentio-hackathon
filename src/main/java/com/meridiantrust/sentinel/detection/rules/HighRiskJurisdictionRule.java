package com.meridiantrust.sentinel.detection.rules;

import com.meridiantrust.sentinel.detection.DetectionRule;
import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.domain.HighRiskJurisdiction;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.WatchlistCounterparty;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.service.ReferenceDataService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Business Rule 4 — High-risk jurisdiction / sanctions. Any transaction whose
 * jurisdiction or counterparty country is on the high-risk/sanctions list, or
 * whose counterparty is watchlisted, is flagged regardless of amount.
 */
@Component
public class HighRiskJurisdictionRule implements DetectionRule {

    private final ReferenceDataService referenceDataService;

    public HighRiskJurisdictionRule(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @Override
    public RuleType type() {
        return RuleType.HIGH_RISK_JURISDICTION;
    }

    @Override
    public List<RuleHit> evaluate(Transaction txn, RuleConfig config) {
        Optional<HighRiskJurisdiction> jurisdiction = referenceDataService.matchJurisdiction(txn.getJurisdiction());
        Optional<HighRiskJurisdiction> cpCountry = referenceDataService.matchJurisdiction(txn.getCounterpartyCountry());
        Optional<WatchlistCounterparty> cpName = referenceDataService.matchCounterparty(txn.getCounterpartyName());

        if (jurisdiction.isEmpty() && cpCountry.isEmpty() && cpName.isEmpty()) {
            return List.of();
        }

        StringBuilder reason = new StringBuilder("Transaction ")
                .append(txn.getTxnRef()).append(" flagged: ");
        jurisdiction.ifPresent(j -> reason.append(String.format(
                "jurisdiction %s (%s, %s); ", j.getCountryCode(), j.getCountryName(), j.getCategory())));
        cpCountry.ifPresent(j -> reason.append(String.format(
                "counterparty country %s (%s, %s); ", j.getCountryCode(), j.getCountryName(), j.getCategory())));
        cpName.ifPresent(w -> reason.append(String.format(
                "counterparty '%s' on watchlist (%s); ", w.getName(), w.getCategory())));

        RuleHit hit = new RuleHit(
                type(), config.getRuleCode(), config.getBaseWeight(), config.getSeverity(),
                "Transaction involving high-risk jurisdiction or sanctioned party",
                reason.toString().trim(),
                "HRJ:" + txn.getTxnRef(),
                List.of(txn));
        return List.of(hit);
    }
}
