package com.meridiantrust.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Sentinel AML — Real-Time Money Laundering Detection Platform.
 *
 * <p>Entry point for the Transaction Monitoring System (TMS). Ingests customer,
 * account and transaction data, runs a configurable rule engine over it, and
 * surfaces risk-scored alerts into an analyst case-management workflow.
 */
@SpringBootApplication
@EnableAsync
public class SentinelApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelApplication.class, args);
    }
}
