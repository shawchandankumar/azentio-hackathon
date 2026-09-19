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

import com.meridiantrust.sentinel.domain.Alert;
import com.meridiantrust.sentinel.domain.CaseFile;
import com.meridiantrust.sentinel.domain.Customer;
import com.meridiantrust.sentinel.domain.enums.CasePriority;
import com.meridiantrust.sentinel.domain.enums.CaseStatus;
import com.meridiantrust.sentinel.repository.CaseRepository;
import com.meridiantrust.sentinel.repository.CustomerRepository;
import com.meridiantrust.sentinel.security.SecurityUtils;
import com.meridiantrust.sentinel.web.error.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Case management workflow: open, assign, progress and close investigation cases. */
@Service
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private final CaseRepository caseRepository;
    private final CustomerRepository customerRepository;
    private final AlertService alertService;
    private final AuditService auditService;
    private final com.meridiantrust.sentinel.repository.AlertRepository alertRepository;
    private final ViewMapper viewMapper;

    public CaseService(CaseRepository caseRepository, CustomerRepository customerRepository,
                       AlertService alertService, AuditService auditService,
                       com.meridiantrust.sentinel.repository.AlertRepository alertRepository,
                       ViewMapper viewMapper) {
        this.caseRepository = caseRepository;
        this.customerRepository = customerRepository;
        this.alertService = alertService;
        this.auditService = auditService;
        this.alertRepository = alertRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional
    public CaseFile open(String customerRef, String title, String summary,
                         CasePriority priority, List<String> alertRefsToLink) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerRef));

        CaseFile caseFile = new CaseFile();
        caseFile.setCaseRef("CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        caseFile.setCustomer(customer);
        caseFile.setTitle(title);
        caseFile.setSummary(summary);
        caseFile.setPriority(priority == null ? CasePriority.MEDIUM : priority);
        caseFile.setStatus(CaseStatus.OPEN);
        String actor = SecurityUtils.currentActor();
        caseFile.setOpenedBy(actor);

        CaseFile saved = caseRepository.save(caseFile);
        auditService.record("CASE", saved.getCaseRef(), "CREATED", actor,
                null, saved.getStatus().name(), "Case opened for customer " + customerRef);

        if (alertRefsToLink != null) {
            for (String alertRef : alertRefsToLink) {
                Alert alert = alertService.getByRef(alertRef);
                alertService.linkToCase(alert, saved);
            }
        }
        log.info("Case {} opened by {} for customer {}", saved.getCaseRef(), actor, customerRef);
        return saved;
    }

    @Transactional(readOnly = true)
    public CaseFile getByRef(String caseRef) {
        return caseRepository.findByCaseRef(caseRef)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseRef));
    }

    @Transactional(readOnly = true)
    public com.meridiantrust.sentinel.dto.CaseView view(String caseRef) {
        CaseFile caseFile = getByRef(caseRef);
        List<String> linked = alertRepository.findByCaseFileId(caseFile.getId()).stream()
                .map(com.meridiantrust.sentinel.domain.Alert::getAlertRef)
                .toList();
        return viewMapper.toCaseView(caseFile, linked);
    }

    @Transactional(readOnly = true)
    public List<com.meridiantrust.sentinel.dto.AuditEventView> auditTrail(String caseRef) {
        getByRef(caseRef); // 404 if missing
        return auditService.trail("CASE", caseRef).stream().map(viewMapper::toAuditView).toList();
    }

    @Transactional
    public CaseFile assign(String caseRef, String assignee) {
        CaseFile caseFile = getByRef(caseRef);
        caseFile.setAssignedTo(assignee);
        CaseFile saved = caseRepository.save(caseFile);
        auditService.record("CASE", caseRef, "ASSIGNED", SecurityUtils.currentActor(),
                null, null, "Assigned to " + assignee);
        return saved;
    }

    @Transactional
    public CaseFile changeStatus(String caseRef, CaseStatus newStatus, String note) {
        CaseFile caseFile = getByRef(caseRef);
        CaseStatus prev = caseFile.getStatus();
        caseFile.setStatus(newStatus);
        if (newStatus == CaseStatus.CLOSED) {
            caseFile.setClosedAt(Instant.now());
        }
        CaseFile saved = caseRepository.save(caseFile);
        auditService.record("CASE", caseRef, "STATUS_CHANGED", SecurityUtils.currentActor(),
                prev.name(), newStatus.name(), note);
        return saved;
    }

    @Transactional
    public CaseFile close(String caseRef, String resolution) {
        CaseFile caseFile = getByRef(caseRef);
        CaseStatus prev = caseFile.getStatus();
        caseFile.setStatus(CaseStatus.CLOSED);
        caseFile.setResolution(resolution);
        caseFile.setClosedAt(Instant.now());
        CaseFile saved = caseRepository.save(caseFile);
        auditService.record("CASE", caseRef, "CLOSED", SecurityUtils.currentActor(),
                prev.name(), CaseStatus.CLOSED.name(), "Resolution: " + resolution);
        return saved;
    }

    @Transactional
    public CaseFile linkAlert(String caseRef, String alertRef) {
        CaseFile caseFile = getByRef(caseRef);
        Alert alert = alertService.getByRef(alertRef);
        alertService.linkToCase(alert, caseFile);
        return caseFile;
    }
}
