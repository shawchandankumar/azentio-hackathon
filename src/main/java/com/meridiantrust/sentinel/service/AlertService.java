package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.detection.RiskScoring;
import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.domain.Alert;
import com.meridiantrust.sentinel.domain.CaseFile;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.AlertStatus;
import com.meridiantrust.sentinel.domain.enums.Disposition;
import com.meridiantrust.sentinel.repository.AlertRepository;
import com.meridiantrust.sentinel.security.SecurityUtils;
import com.meridiantrust.sentinel.web.error.ResourceNotFoundException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, aggregates and dispositions alerts. De-duplication is keyed on the
 * rule hit's {@code dedupKey} so one underlying pattern yields a single alert
 * that accumulates evidence rather than many redundant ones. Alerts are never
 * deleted (Business Rule 6).
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final Set<AlertStatus> OPEN_STATES =
            Set.of(AlertStatus.OPEN, AlertStatus.UNDER_REVIEW, AlertStatus.ESCALATED);

    private final AlertRepository alertRepository;
    private final RiskScoring riskScoring;
    private final AuditService auditService;
    private final ViewMapper viewMapper;

    public AlertService(AlertRepository alertRepository, RiskScoring riskScoring,
                        AuditService auditService, ViewMapper viewMapper) {
        this.alertRepository = alertRepository;
        this.riskScoring = riskScoring;
        this.auditService = auditService;
        this.viewMapper = viewMapper;
    }

    /**
     * Persist a rule hit as a new alert or merge it into an existing one sharing
     * the same dedup key. Thread-safe: relies on the unique constraint on
     * {@code dedup_key} and retries as a merge on a concurrent insert.
     */
    @Transactional
    public Alert applyHit(RuleHit hit, Transaction txn) {
        Customer customer = txn.getAccount().getCustomer();
        return alertRepository.findByDedupKey(hit.getDedupKey())
                .map(existing -> merge(existing, hit, txn))
                .orElseGet(() -> {
                    try {
                        return create(hit, txn, customer);
                    } catch (DataIntegrityViolationException race) {
                        // Concurrent insert won the race — merge into the winner.
                        Alert existing = alertRepository.findByDedupKey(hit.getDedupKey())
                                .orElseThrow(() -> race);
                        return merge(existing, hit, txn);
                    }
                });
    }

    private Alert create(RuleHit hit, Transaction txn, Customer customer) {
        Alert alert = new Alert();
        alert.setAlertRef("ALT-" + shortId());
        alert.setCustomer(customer);
        alert.setAccount(txn.getAccount());
        alert.setRuleType(hit.getRuleType());
        alert.setContributingRules(hit.getRuleCode());
        alert.setTitle(hit.getTitle());
        alert.setExplanation(hit.getExplanation());
        alert.setDedupKey(hit.getDedupKey());
        alert.setStatus(AlertStatus.OPEN);

        int score = riskScoring.scoreForHit(hit, customer);
        alert.setRiskScore(score);
        alert.setSeverity(riskScoring.severityFor(score, hit.getSeverity()));

        hit.getEvidence().forEach(e -> alert.addEvidence(e, hit.getRuleCode()));

        Alert saved = alertRepository.save(alert);
        auditService.record("ALERT", saved.getAlertRef(), "CREATED", SecurityUtils.currentActor(),
                null, saved.getStatus().name(),
                String.format("Rule %s fired (score %d, %s)", hit.getRuleCode(), score, saved.getSeverity()));
        log.info("Alert {} created for customer {} by rule {} (score {})",
                saved.getAlertRef(), customer.getCustomerRef(), hit.getRuleCode(), score);
        return saved;
    }

    private Alert merge(Alert existing, RuleHit hit, Transaction txn) {
        // Do not resurrect an already-dispositioned alert; just record the re-detection.
        if (!OPEN_STATES.contains(existing.getStatus())) {
            auditService.record("ALERT", existing.getAlertRef(), "REDETECTED", SecurityUtils.currentActor(),
                    existing.getStatus().name(), existing.getStatus().name(),
                    "Pattern re-observed on txn " + txn.getTxnRef() + " but alert already dispositioned");
            return existing;
        }

        hit.getEvidence().forEach(e -> existing.addEvidence(e, hit.getRuleCode()));

        // Track reinforcing rules and re-score.
        Set<String> rules = new LinkedHashSet<>(Arrays.asList(
                existing.getContributingRules() == null ? new String[0]
                        : existing.getContributingRules().split(",")));
        boolean newRule = rules.add(hit.getRuleCode());
        existing.setContributingRules(String.join(",", rules));

        int addition = riskScoring.scoreForHit(hit, existing.getCustomer());
        int combined = riskScoring.combine(existing.getRiskScore(), addition);
        existing.setRiskScore(combined);
        existing.setSeverity(riskScoring.severityFor(combined, hit.getSeverity()));

        Alert saved = alertRepository.save(existing);
        auditService.record("ALERT", saved.getAlertRef(), "AGGREGATED", SecurityUtils.currentActor(),
                null, saved.getStatus().name(),
                String.format("Merged hit from %s (new evidence; score now %d%s)",
                        hit.getRuleCode(), combined, newRule ? ", new reinforcing rule" : ""));
        return saved;
    }

    // ---- Query -------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Alert> queue(AlertStatus status, Pageable pageable) {
        return status == null
                ? alertRepository.findAll(pageable)
                : alertRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<com.meridiantrust.sentinel.dto.AlertSummaryView> queueView(AlertStatus status, Pageable pageable) {
        return queue(status, pageable).map(viewMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public com.meridiantrust.sentinel.dto.AlertDetailView detailView(String alertRef, boolean unmaskPii) {
        return viewMapper.toDetail(getByRef(alertRef), unmaskPii);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.meridiantrust.sentinel.dto.AuditEventView> auditTrail(String alertRef) {
        getByRef(alertRef); // 404 if missing
        return auditService.trail("ALERT", alertRef).stream().map(viewMapper::toAuditView).toList();
    }

    @Transactional(readOnly = true)
    public Alert getByRef(String alertRef) {
        return alertRepository.findByAlertRef(alertRef)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertRef));
    }

    // ---- State transitions (all audited; never deleted) --------------------

    @Transactional
    public Alert changeStatus(String alertRef, AlertStatus newStatus, String note) {
        Alert alert = getByRef(alertRef);
        AlertStatus prev = alert.getStatus();
        alert.setStatus(newStatus);
        Alert saved = alertRepository.save(alert);
        auditService.record("ALERT", alertRef, "STATUS_CHANGED", SecurityUtils.currentActor(),
                prev.name(), newStatus.name(), note);
        return saved;
    }

    /**
     * Disposition an alert (clear or confirm). The alert is retained with its
     * disposition reason and the analyst's identity for audit (Business Rule 6).
     */
    @Transactional
    public Alert disposition(String alertRef, Disposition disposition, String reason) {
        Alert alert = getByRef(alertRef);
        AlertStatus prev = alert.getStatus();
        AlertStatus newStatus = (disposition == Disposition.CONFIRMED_SUSPICIOUS
                || disposition == Disposition.ESCALATED_SAR)
                ? AlertStatus.CONFIRMED
                : AlertStatus.CLEARED;

        String actor = SecurityUtils.currentActor();
        alert.setStatus(newStatus);
        alert.setDisposition(disposition);
        alert.setDispositionReason(reason);
        alert.setDisposedBy(actor);
        alert.setDisposedAt(Instant.now());
        Alert saved = alertRepository.save(alert);

        auditService.record("ALERT", alertRef, "DISPOSED", actor,
                prev.name(), newStatus.name(),
                String.format("Disposition=%s; reason=%s", disposition, reason));
        log.info("Alert {} dispositioned {} by {}", alertRef, disposition, actor);
        return saved;
    }

    @Transactional
    public Alert linkToCase(Alert alert, CaseFile caseFile) {
        alert.setCaseFile(caseFile);
        Alert saved = alertRepository.save(alert);
        auditService.record("ALERT", alert.getAlertRef(), "LINKED_TO_CASE", SecurityUtils.currentActor(),
                null, null, "Linked to case " + caseFile.getCaseRef());
        return saved;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
