package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Alert;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import com.meridiantrust.sentinel.dto.AccountIngestRequest;
import com.meridiantrust.sentinel.dto.CustomerIngestRequest;
import com.meridiantrust.sentinel.dto.IngestionResult;
import com.meridiantrust.sentinel.dto.TransactionIngestRequest;
import com.meridiantrust.sentinel.repository.AccountRepository;
import com.meridiantrust.sentinel.repository.CustomerRepository;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Loads customer, account and transaction data with per-record validation and
 * referential-integrity enforcement. Malformed or conflicting records are
 * rejected and logged without aborting the rest of the batch. Transaction
 * ingestion normalizes amounts to base currency and can trigger detection inline.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyService currencyService;
    private final DetectionService detectionService;

    public IngestionService(CustomerRepository customerRepository, AccountRepository accountRepository,
                            TransactionRepository transactionRepository, CurrencyService currencyService,
                            DetectionService detectionService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.currencyService = currencyService;
        this.detectionService = detectionService;
    }

    // ---- Customers ---------------------------------------------------------

    public IngestionResult ingestCustomers(List<CustomerIngestRequest> requests) {
        IngestionResult.Builder result = new IngestionResult.Builder();
        for (CustomerIngestRequest req : requests) {
            try {
                if (customerRepository.existsByCustomerRef(req.customerRef())) {
                    result.reject("customer " + req.customerRef() + ": already exists");
                    continue;
                }
                Customer c = new Customer();
                c.setCustomerRef(req.customerRef());
                c.setFullName(req.fullName());
                c.setDateOfBirth(req.dateOfBirth());
                c.setNationalId(req.nationalId());
                c.setCustomerType(req.customerType());
                c.setRiskRating(req.riskRating() == null ? RiskRating.LOW : req.riskRating());
                c.setResidenceCountry(req.residenceCountry().toUpperCase());
                c.setEmail(req.email());
                c.setPhone(req.phone());
                c.setOnboardedAt(req.onboardedAt());
                customerRepository.save(c);
                result.accept();
            } catch (Exception e) {
                log.warn("Customer ingest failed for {}: {}", req.customerRef(), e.getMessage());
                result.reject("customer " + req.customerRef() + ": " + e.getMessage());
            }
        }
        return result.build();
    }

    // ---- Accounts ----------------------------------------------------------

    public IngestionResult ingestAccounts(List<AccountIngestRequest> requests) {
        IngestionResult.Builder result = new IngestionResult.Builder();
        for (AccountIngestRequest req : requests) {
            try {
                Customer customer = customerRepository.findByCustomerRef(req.customerRef()).orElse(null);
                if (customer == null) {
                    result.reject("account " + req.accountNumber() + ": unknown customer " + req.customerRef());
                    continue;
                }
                if (accountRepository.existsByAccountNumber(req.accountNumber())) {
                    result.reject("account " + req.accountNumber() + ": already exists");
                    continue;
                }
                Account a = new Account();
                a.setAccountNumber(req.accountNumber());
                a.setCustomer(customer);
                a.setAccountType(req.accountType());
                a.setCurrency(req.currency().toUpperCase());
                a.setOpeningDate(req.openingDate());
                a.setRiskRating(req.riskRating() == null ? RiskRating.LOW : req.riskRating());
                if (req.status() != null) {
                    a.setStatus(req.status());
                }
                accountRepository.save(a);
                result.accept();
            } catch (Exception e) {
                log.warn("Account ingest failed for {}: {}", req.accountNumber(), e.getMessage());
                result.reject("account " + req.accountNumber() + ": " + e.getMessage());
            }
        }
        return result.build();
    }

    // ---- Transactions ------------------------------------------------------

    /**
     * Ingest transactions. When {@code detectNow} is true each accepted
     * transaction is evaluated by the rule engine immediately (streaming mode);
     * otherwise detection is deferred (bulk mode) and run separately.
     */
    public IngestionResult ingestTransactions(List<TransactionIngestRequest> requests, boolean detectNow) {
        IngestionResult.Builder result = new IngestionResult.Builder();
        for (TransactionIngestRequest req : requests) {
            try {
                Account account = accountRepository.findByAccountNumber(req.accountNumber()).orElse(null);
                if (account == null) {
                    result.reject("txn " + req.txnRef() + ": unknown account " + req.accountNumber());
                    continue;
                }
                if (transactionRepository.existsByTxnRef(req.txnRef())) {
                    result.reject("txn " + req.txnRef() + ": already exists");
                    continue;
                }

                BigDecimal baseAmount = currencyService.toBase(req.amount(), req.currency());

                Transaction t = new Transaction();
                t.setTxnRef(req.txnRef());
                t.setAccount(account);
                t.setDirection(req.direction());
                t.setTxnType(req.txnType());
                t.setAmount(req.amount());
                t.setCurrency(req.currency().toUpperCase());
                t.setBaseAmount(baseAmount);
                t.setCounterpartyName(req.counterpartyName());
                t.setCounterpartyAccount(req.counterpartyAccount());
                t.setCounterpartyCountry(normalizeCountry(req.counterpartyCountry()));
                t.setChannel(req.channel());
                t.setJurisdiction(normalizeCountry(req.jurisdiction()));
                t.setBookedAt(req.bookedAt());
                transactionRepository.save(t);
                result.accept();

                if (detectNow) {
                    List<Alert> alerts = detectionService.detectByRef(req.txnRef());
                    alerts.forEach(a -> result.addAlert(a.getAlertRef()));
                }
            } catch (Exception e) {
                log.warn("Transaction ingest failed for {}: {}", req.txnRef(), e.getMessage());
                result.reject("txn " + req.txnRef() + ": " + e.getMessage());
            }
        }
        return result.build();
    }

    private static String normalizeCountry(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase();
    }
}
