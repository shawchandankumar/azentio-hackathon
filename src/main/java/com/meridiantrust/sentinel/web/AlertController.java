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
package com.meridiantrust.sentinel.web;

import com.meridiantrust.sentinel.domain.enums.AlertStatus;
import com.meridiantrust.sentinel.dto.AlertDetailView;
import com.meridiantrust.sentinel.dto.AlertSummaryView;
import com.meridiantrust.sentinel.dto.AuditEventView;
import com.meridiantrust.sentinel.dto.DispositionRequest;
import com.meridiantrust.sentinel.dto.StatusChangeRequest;
import com.meridiantrust.sentinel.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Analyst-facing alert queue and workflow. Higher-risk alerts sort to the top
 * (Business Rule 7). Full PII in detail views is limited to SUPERVISOR/ADMIN
 * (Business Rule 8); list views are always masked.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR','ADMIN')")
@Tag(name = "Alerts", description = "Alert queue, detail, disposition and audit")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "List alerts (queue), sorted by risk score by default")
    public Page<AlertSummaryView> queue(
            @RequestParam(required = false) AlertStatus status,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(size = 20, sort = "riskScore", direction = Sort.Direction.DESC) Pageable pageable) {
        return alertService.queueView(status, pageable);
    }

    @GetMapping("/{alertRef}")
    @Operation(summary = "Get full alert detail with supporting evidence")
    public AlertDetailView detail(@PathVariable String alertRef, Authentication auth) {
        return alertService.detailView(alertRef, canSeePii(auth));
    }

    @GetMapping("/{alertRef}/audit")
    @Operation(summary = "Immutable audit trail for an alert")
    public List<AuditEventView> audit(@PathVariable String alertRef) {
        return alertService.auditTrail(alertRef);
    }

    @PostMapping("/{alertRef}/disposition")
    @Operation(summary = "Clear or confirm an alert (retained for audit, never deleted)")
    public AlertDetailView disposition(@PathVariable String alertRef,
                                       @Valid @RequestBody DispositionRequest request,
                                       Authentication auth) {
        alertService.disposition(alertRef, request.disposition(), request.reason());
        return alertService.detailView(alertRef, canSeePii(auth));
    }

    @PatchMapping("/{alertRef}/status")
    @Operation(summary = "Transition an alert's workflow status")
    public AlertDetailView status(@PathVariable String alertRef,
                                  @Valid @RequestBody StatusChangeRequest request,
                                  Authentication auth) {
        alertService.changeStatus(alertRef, AlertStatus.valueOf(request.status()), request.note());
        return alertService.detailView(alertRef, canSeePii(auth));
    }

    private boolean canSeePii(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR")
                        || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
