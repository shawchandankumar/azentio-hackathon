package com.meridiantrust.sentinel.bootstrap;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.ExchangeRate;
import com.meridiantrust.sentinel.domain.HighRiskJurisdiction;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.WatchlistCounterparty;
import com.meridiantrust.sentinel.domain.enums.AccountType;
import com.meridiantrust.sentinel.domain.enums.Channel;
import com.meridiantrust.sentinel.domain.enums.CustomerType;
import com.meridiantrust.sentinel.domain.enums.Direction;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import com.meridiantrust.sentinel.domain.enums.TransactionType;
import com.meridiantrust.sentinel.repository.CustomerRepository;
import com.meridiantrust.sentinel.repository.ExchangeRateRepository;
import com.meridiantrust.sentinel.repository.HighRiskJurisdictionRepository;
import com.meridiantrust.sentinel.repository.RuleConfigRepository;
import com.meridiantrust.sentinel.repository.WatchlistCounterpartyRepository;
import com.meridiantrust.sentinel.service.CurrencyService;
import com.meridiantrust.sentinel.service.DetectionService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds configurable reference data and realistic synthetic customers/accounts/
 * transactions on first startup (empty DB), then runs detection so the demo has
 * alerts across every typology immediately. Idempotent and toggleable via
 * {@code sentinel.seed.*}. All data is synthetic — no real PII (Data Privacy NFR).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CustomerRepository customerRepository;
    private final com.meridiantrust.sentinel.repository.AccountRepository accountRepository;
    private final com.meridiantrust.sentinel.repository.TransactionRepository transactionRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final HighRiskJurisdictionRepository jurisdictionRepository;
    private final WatchlistCounterpartyRepository watchlistRepository;
    private final CurrencyService currencyService;
    private final DetectionService detectionService;

    private final boolean seedEnabled;
    private final boolean seedSynthetic;

    private int txnSeq = 0;

    public DataSeeder(CustomerRepository customerRepository,
                      com.meridiantrust.sentinel.repository.AccountRepository accountRepository,
                      com.meridiantrust.sentinel.repository.TransactionRepository transactionRepository,
                      RuleConfigRepository ruleConfigRepository,
                      ExchangeRateRepository exchangeRateRepository,
                      HighRiskJurisdictionRepository jurisdictionRepository,
                      WatchlistCounterpartyRepository watchlistRepository,
                      CurrencyService currencyService,
                      DetectionService detectionService,
                      @Value("${sentinel.seed.enabled:true}") boolean seedEnabled,
                      @Value("${sentinel.seed.synthetic:true}") boolean seedSynthetic) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ruleConfigRepository = ruleConfigRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.jurisdictionRepository = jurisdictionRepository;
        this.watchlistRepository = watchlistRepository;
        this.currencyService = currencyService;
        this.detectionService = detectionService;
        this.seedEnabled = seedEnabled;
        this.seedSynthetic = seedSynthetic;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Seeding disabled (sentinel.seed.enabled=false)");
            return;
        }
        seedReferenceData();
        if (seedSynthetic && customerRepository.count() == 0) {
            seedSyntheticData();
            int processed = detectionService.detectAll();
            log.info("Synthetic seed complete; ran detection over {} transactions", processed);
        }
    }

    // ---- Reference data ----------------------------------------------------

    private void seedReferenceData() {
        if (exchangeRateRepository.count() == 0) {
            rate("INR", "1.0");
            rate("USD", "83.2");
            rate("EUR", "90.1");
            rate("GBP", "105.4");
            rate("AED", "22.65");
            rate("SGD", "61.8");
            rate("CHF", "94.3");
            rate("RUB", "0.91");
            log.info("Seeded exchange rates");
        }
        if (jurisdictionRepository.count() == 0) {
            jurisdiction("IR", "Iran", "SANCTIONED");
            jurisdiction("KP", "North Korea", "SANCTIONED");
            jurisdiction("SY", "Syria", "SANCTIONED");
            jurisdiction("MM", "Myanmar", "HIGH_RISK");
            jurisdiction("PA", "Panama", "HIGH_RISK");
            jurisdiction("KY", "Cayman Islands", "HIGH_RISK");
            log.info("Seeded high-risk jurisdictions");
        }
        if (watchlistRepository.count() == 0) {
            watchlist("Nordwind Holdings Ltd", "KY");
            watchlist("Zarubezh Import Export", "RU");
            watchlist("Crescent Trading FZE", "IR");
            log.info("Seeded watchlist counterparties");
        }
        if (ruleConfigRepository.count() == 0) {
            rule("R1_LARGE_TXN", RuleType.LARGE_TRANSACTION, "Large Transaction (CTR)", 60, Severity.MEDIUM,
                    "{\"thresholdBase\": 831960.00}");
            rule("R2_STRUCTURING", RuleType.STRUCTURING, "Structuring / Smurfing", 80, Severity.HIGH,
                    "{\"windowHours\": 24, \"minCount\": 3, \"lowerBase\": 748764.00, \"upperBase\": 831876.00}");
            rule("R3_RAPID_MOVEMENT", RuleType.RAPID_MOVEMENT, "Rapid Movement of Funds", 85, Severity.HIGH,
                    "{\"windowHours\": 48, \"outflowRatio\": 0.80, \"minCreditBase\": 415980.00}");
            rule("R4_HIGH_RISK_JURISDICTION", RuleType.HIGH_RISK_JURISDICTION,
                    "High-Risk Jurisdiction / Sanctions", 90, Severity.CRITICAL, "{}");
            rule("R5_BEHAVIORAL_DEVIATION", RuleType.BEHAVIORAL_DEVIATION, "Behavioral Deviation", 70, Severity.HIGH,
                    "{\"lookbackDays\": 90, \"multiplier\": 3.0, \"minBaselineBase\": 41598.00}");
            rule("R6_ROUND_NUMBER", RuleType.ROUND_NUMBER, "Round-Number Pattern", 40, Severity.LOW,
                    "{\"windowHours\": 72, \"minCount\": 3, \"roundUnit\": 1000}");
            log.info("Seeded detection rule configuration");
        }
    }

    // ---- Synthetic transactional data (covers all six typologies) ----------

    private void seedSyntheticData() {
        Instant now = Instant.now();

        // 1) Structuring: four sub-threshold INR deposits same day (Business Rule 2).
        Customer c2 = customer("CUST-1002", "Aisha Khan", CustomerType.RETAIL, RiskRating.MEDIUM, "IN");
        Account a2 = account("ACC-2002", c2, AccountType.SAVINGS, "INR", RiskRating.MEDIUM);
        credit(a2, "799500", "INR", TransactionType.DEPOSIT, Channel.BRANCH, "IN", now.minus(20, ChronoUnit.HOURS));
        credit(a2, "810250", "INR", TransactionType.DEPOSIT, Channel.ATM, "IN", now.minus(16, ChronoUnit.HOURS));
        credit(a2, "795750", "INR", TransactionType.DEPOSIT, Channel.BRANCH, "IN", now.minus(8, ChronoUnit.HOURS));
        credit(a2, "822100", "INR", TransactionType.DEPOSIT, Channel.ATM, "IN", now.minus(2, ChronoUnit.HOURS));

        // 2) Rapid movement + large transaction (Business Rules 1 & 3).
        Customer c3 = customer("CUST-1003", "Wei Chen", CustomerType.BUSINESS, RiskRating.HIGH, "SG");
        Account a3 = account("ACC-2003", c3, AccountType.BUSINESS, "INR", RiskRating.HIGH);
        credit(a3, "5000000", "INR", TransactionType.DEPOSIT, Channel.WIRE, "SG", now.minus(40, ChronoUnit.HOURS));
        debit(a3, "2600000", "INR", TransactionType.TRANSFER, Channel.WIRE, "AE",
                "Sunrise Trading LLC", "AE", now.minus(30, ChronoUnit.HOURS));
        debit(a3, "1900000", "INR", TransactionType.TRANSFER, Channel.WIRE, "AE",
                "Delta Logistics", "AE", now.minus(20, ChronoUnit.HOURS));

        // 3) High-risk jurisdiction + sanctioned counterparty (Business Rule 4).
        Customer c4 = customer("CUST-1004", "Carlos Mendez", CustomerType.RETAIL, RiskRating.MEDIUM, "PA");
        Account a4 = account("ACC-2004", c4, AccountType.CHECKING, "USD", RiskRating.MEDIUM);
        debit(a4, "3000", "USD", TransactionType.TRANSFER, Channel.WIRE, "KP",
                "Pyongyang Metals Co", "KP", now.minus(5, ChronoUnit.HOURS));
        debit(a4, "4500", "USD", TransactionType.PAYMENT, Channel.ONLINE, "IR",
                "Crescent Trading FZE", "IR", now.minus(3, ChronoUnit.HOURS));

        // 4) Round-number pattern below thresholds (round-number typology).
        Customer c5 = customer("CUST-1005", "Elena Petrova", CustomerType.BUSINESS, RiskRating.HIGH, "AE");
        Account a5 = account("ACC-2005", c5, AccountType.BUSINESS, "INR", RiskRating.MEDIUM);
        debit(a5, "500000", "INR", TransactionType.TRANSFER, Channel.ONLINE, "AE",
                "Vector Consulting", "AE", now.minus(60, ChronoUnit.HOURS));
        debit(a5, "500000", "INR", TransactionType.TRANSFER, Channel.ONLINE, "AE",
                "Vector Consulting", "AE", now.minus(40, ChronoUnit.HOURS));
        debit(a5, "500000", "INR", TransactionType.TRANSFER, Channel.ONLINE, "AE",
                "Vector Consulting", "AE", now.minus(20, ChronoUnit.HOURS));
        debit(a5, "500000", "INR", TransactionType.TRANSFER, Channel.ONLINE, "AE",
                "Vector Consulting", "AE", now.minus(6, ChronoUnit.HOURS));

        // 5) Behavioral deviation: steady baseline then a >3x spike (Business Rule 5).
        Customer c1 = customer("CUST-1001", "Ravi Sharma", CustomerType.RETAIL, RiskRating.LOW, "IN");
        Account a1 = account("ACC-2001", c1, AccountType.CHECKING, "INR", RiskRating.LOW);
        for (int d = 30; d >= 3; d -= 2) { // ~14 baseline days, non-round amounts
            debit(a1, "61234", "INR", TransactionType.PAYMENT, Channel.ONLINE, "IN",
                    "Merchant " + d, "IN", now.minus(Duration.ofDays(d)));
        }
        debit(a1, "254321", "INR", TransactionType.PAYMENT, Channel.ONLINE, "IN",
                "Luxury Motors", "IN", now.minus(1, ChronoUnit.HOURS)); // today's spike

        // 6) Benign customer — normal activity, should raise no alerts.
        Customer c6 = customer("CUST-1006", "Meera Nair", CustomerType.RETAIL, RiskRating.LOW, "IN");
        Account a6 = account("ACC-2006", c6, AccountType.SAVINGS, "INR", RiskRating.LOW);
        debit(a6, "2450", "INR", TransactionType.PAYMENT, Channel.MOBILE, "IN",
                "Grocery Mart", "IN", now.minus(50, ChronoUnit.HOURS));
        debit(a6, "1875", "INR", TransactionType.PAYMENT, Channel.MOBILE, "IN",
                "Fuel Station", "IN", now.minus(26, ChronoUnit.HOURS));
        credit(a6, "45000", "INR", TransactionType.DEPOSIT, Channel.BRANCH, "IN", now.minus(10, ChronoUnit.HOURS));

        log.info("Seeded {} customers and {} transactions",
                customerRepository.count(), transactionRepository.count());
    }

    // ---- Builders ----------------------------------------------------------

    private void rate(String code, String rate) {
        ExchangeRate r = new ExchangeRate();
        r.setCurrencyCode(code);
        r.setRateToBase(new BigDecimal(rate));
        exchangeRateRepository.save(r);
    }

    private void jurisdiction(String code, String name, String category) {
        HighRiskJurisdiction j = new HighRiskJurisdiction();
        j.setCountryCode(code);
        j.setCountryName(name);
        j.setCategory(category);
        jurisdictionRepository.save(j);
    }

    private void watchlist(String name, String country) {
        WatchlistCounterparty w = new WatchlistCounterparty();
        w.setName(name);
        w.setCountryCode(country);
        w.setCategory("SANCTIONED");
        watchlistRepository.save(w);
    }

    private void rule(String code, RuleType type, String name, int weight, Severity severity, String params) {
        RuleConfig r = new RuleConfig();
        r.setRuleCode(code);
        r.setRuleType(type);
        r.setDisplayName(name);
        r.setBaseWeight(weight);
        r.setSeverity(severity);
        r.setParamsJson(params);
        r.setEnabled(true);
        r.setUpdatedBy("system");
        ruleConfigRepository.save(r);
    }

    private Customer customer(String ref, String name, CustomerType type, RiskRating risk, String country) {
        Customer c = new Customer();
        c.setCustomerRef(ref);
        c.setFullName(name);
        c.setCustomerType(type);
        c.setRiskRating(risk);
        c.setResidenceCountry(country);
        c.setNationalId("SYN" + ref.replace("CUST-", ""));
        c.setEmail(ref.toLowerCase() + "@example.test");
        c.setOnboardedAt(Instant.now().minus(Duration.ofDays(400)));
        return customerRepository.save(c);
    }

    private Account account(String number, Customer customer, AccountType type, String currency, RiskRating risk) {
        Account a = new Account();
        a.setAccountNumber(number);
        a.setCustomer(customer);
        a.setAccountType(type);
        a.setCurrency(currency);
        a.setOpeningDate(java.time.LocalDate.now().minusYears(1));
        a.setRiskRating(risk);
        return accountRepository.save(a);
    }

    private void credit(Account account, String amount, String currency, TransactionType type,
                        Channel channel, String jurisdiction, Instant bookedAt) {
        txn(account, Direction.CREDIT, type, amount, currency, channel, jurisdiction, null, null, bookedAt);
    }

    private void debit(Account account, String amount, String currency, TransactionType type, Channel channel,
                       String jurisdiction, String cpName, String cpCountry, Instant bookedAt) {
        txn(account, Direction.DEBIT, type, amount, currency, channel, jurisdiction, cpName, cpCountry, bookedAt);
    }

    private void txn(Account account, Direction direction, TransactionType type, String amount, String currency,
                     Channel channel, String jurisdiction, String cpName, String cpCountry, Instant bookedAt) {
        Transaction t = new Transaction();
        t.setTxnRef("TXN-" + String.format("%06d", ++txnSeq));
        t.setAccount(account);
        t.setDirection(direction);
        t.setTxnType(type);
        BigDecimal amt = new BigDecimal(amount);
        t.setAmount(amt);
        t.setCurrency(currency);
        t.setBaseAmount(currencyService.toBase(amt, currency));
        t.setChannel(channel);
        t.setJurisdiction(jurisdiction);
        t.setCounterpartyName(cpName);
        t.setCounterpartyCountry(cpCountry);
        t.setBookedAt(bookedAt);
        transactionRepository.save(t);
    }
}
