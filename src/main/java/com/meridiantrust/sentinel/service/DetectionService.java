package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.detection.RuleEngine;
import com.meridiantrust.sentinel.detection.RuleHit;
import com.meridiantrust.sentinel.domain.Alert;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.web.error.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the rule engine over transactions and materializes the resulting alerts.
 * Used both for per-transaction streaming detection and for bulk re-evaluation.
 */
@Service
public class DetectionService {

    private static final Logger log = LoggerFactory.getLogger(DetectionService.class);

    private final RuleEngine ruleEngine;
    private final AlertService alertService;
    private final TransactionRepository transactionRepository;

    public DetectionService(RuleEngine ruleEngine, AlertService alertService,
                            TransactionRepository transactionRepository) {
        this.ruleEngine = ruleEngine;
        this.alertService = alertService;
        this.transactionRepository = transactionRepository;
    }

    /** Evaluate a single already-persisted transaction and apply any hits. */
    @Transactional
    public List<Alert> detect(Transaction txn) {
        List<RuleHit> hits = ruleEngine.evaluate(txn);
        List<Alert> alerts = new ArrayList<>(hits.size());
        for (RuleHit hit : hits) {
            alerts.add(alertService.applyHit(hit, txn));
        }
        return alerts;
    }

    @Transactional
    public List<Alert> detectByRef(String txnRef) {
        Transaction txn = transactionRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + txnRef));
        return detect(txn);
    }

    /**
     * Re-evaluate every transaction in chronological order. Returns the number of
     * transactions processed. Used for bulk load scenarios.
     */
    @Transactional
    public int detectAll() {
        List<Transaction> all = transactionRepository.findAll(
                org.springframework.data.domain.Sort.by("bookedAt").ascending());
        for (Transaction txn : all) {
            detect(txn);
        }
        log.info("Bulk detection complete over {} transactions", all.size());
        return all.size();
    }
}
