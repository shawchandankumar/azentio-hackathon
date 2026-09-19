package com.meridiantrust.sentinel.detection;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.AccountType;
import com.meridiantrust.sentinel.domain.enums.Channel;
import com.meridiantrust.sentinel.domain.enums.CustomerType;
import com.meridiantrust.sentinel.domain.enums.Direction;
import com.meridiantrust.sentinel.domain.enums.RiskRating;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.domain.enums.Severity;
import com.meridiantrust.sentinel.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/** Builders for detection-rule unit tests. */
public final class TestFixtures {

    private static final AtomicLong TXN_IDS = new AtomicLong(1);

    private TestFixtures() {
    }

    public static Customer customer(long id, String ref, RiskRating risk) {
        Customer c = new Customer();
        c.setId(id);
        c.setCustomerRef(ref);
        c.setFullName("Test Customer " + id);
        c.setCustomerType(CustomerType.RETAIL);
        c.setRiskRating(risk);
        c.setResidenceCountry("IN");
        return c;
    }

    public static Account account(long id, String number, Customer customer) {
        Account a = new Account();
        a.setId(id);
        a.setAccountNumber(number);
        a.setCustomer(customer);
        a.setAccountType(AccountType.CHECKING);
        a.setCurrency("INR");
        a.setRiskRating(RiskRating.LOW);
        return a;
    }

    public static Transaction txn(Account account, Direction direction, String baseAmount, Instant bookedAt) {
        Transaction t = new Transaction();
        t.setId(TXN_IDS.getAndIncrement());
        t.setTxnRef("T-" + t.getId());
        t.setAccount(account);
        t.setDirection(direction);
        t.setTxnType(direction == Direction.CREDIT ? TransactionType.DEPOSIT : TransactionType.TRANSFER);
        BigDecimal amt = new BigDecimal(baseAmount);
        t.setAmount(amt);
        t.setCurrency("INR");
        t.setBaseAmount(amt);
        t.setChannel(Channel.ONLINE);
        t.setJurisdiction("IN");
        t.setBookedAt(bookedAt);
        return t;
    }

    public static RuleConfig config(String code, RuleType type, int weight, Severity severity, String paramsJson) {
        RuleConfig c = new RuleConfig();
        c.setRuleCode(code);
        c.setRuleType(type);
        c.setDisplayName(code);
        c.setBaseWeight(weight);
        c.setSeverity(severity);
        c.setParamsJson(paramsJson);
        c.setEnabled(true);
        return c;
    }
}
