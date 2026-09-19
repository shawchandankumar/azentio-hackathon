/*
 * Sentinel AML — Real-Time Money Laundering Detection Platform
 * Copyright (C) 2026 Chandan Kumar Shaw <shawchandankumar20@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.Account;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.dto.AccountView;
import com.meridiantrust.sentinel.dto.AlertSummaryView;
import com.meridiantrust.sentinel.dto.CustomerDetailView;
import com.meridiantrust.sentinel.dto.TransactionView;
import com.meridiantrust.sentinel.repository.AccountRepository;
import com.meridiantrust.sentinel.repository.AlertRepository;
import com.meridiantrust.sentinel.repository.CustomerRepository;
import com.meridiantrust.sentinel.repository.TransactionRepository;
import com.meridiantrust.sentinel.web.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side service for customer detail, transaction timeline and linked alerts. */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final PiiMaskingService masking;
    private final ViewMapper viewMapper;

    public CustomerService(CustomerRepository customerRepository, AccountRepository accountRepository,
                           TransactionRepository transactionRepository, AlertRepository alertRepository,
                           PiiMaskingService masking, ViewMapper viewMapper) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.masking = masking;
        this.viewMapper = viewMapper;
    }

    @Transactional(readOnly = true)
    public CustomerDetailView detail(String customerRef, boolean unmaskPii) {
        Customer c = customerRepository.findByCustomerRef(customerRef)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerRef));

        List<AccountView> accounts = accountRepository.findByCustomerId(c.getId()).stream()
                .map(this::toAccountView)
                .toList();

        return new CustomerDetailView(
                c.getCustomerRef(),
                unmaskPii ? c.getFullName() : masking.maskName(c.getFullName()),
                unmaskPii ? c.getNationalId() : masking.maskId(c.getNationalId()),
                c.getCustomerType(),
                c.getRiskRating(),
                c.getResidenceCountry(),
                c.getDateOfBirth(),
                c.getEmail(),
                c.getPhone(),
                c.getOnboardedAt(),
                !unmaskPii,
                accounts);
    }

    @Transactional(readOnly = true)
    public List<TransactionView> timeline(String customerRef) {
        Customer c = customerRepository.findByCustomerRef(customerRef)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerRef));
        return transactionRepository.findByAccount_Customer_IdOrderByBookedAtDesc(c.getId()).stream()
                .map(t -> new TransactionView(
                        t.getTxnRef(),
                        t.getAccount().getAccountNumber(),
                        t.getDirection().name(),
                        t.getTxnType().name(),
                        t.getAmount(),
                        t.getCurrency(),
                        t.getBaseAmount(),
                        t.getCounterpartyName(),
                        t.getCounterpartyCountry(),
                        t.getChannel().name(),
                        t.getJurisdiction(),
                        t.getBookedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertSummaryView> alerts(String customerRef) {
        Customer c = customerRepository.findByCustomerRef(customerRef)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerRef));
        return alertRepository.findByCustomerId(c.getId()).stream()
                .map(viewMapper::toSummary)
                .toList();
    }

    private AccountView toAccountView(Account a) {
        return new AccountView(
                a.getAccountNumber(),
                a.getAccountType(),
                a.getCurrency(),
                a.getOpeningDate(),
                a.getRiskRating(),
                a.getStatus());
    }
}
