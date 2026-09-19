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

import com.meridiantrust.sentinel.domain.enums.CaseStatus;
import com.meridiantrust.sentinel.dto.AuditEventView;
import com.meridiantrust.sentinel.dto.CaseCreateRequest;
import com.meridiantrust.sentinel.dto.CaseView;
import com.meridiantrust.sentinel.dto.StatusChangeRequest;
import com.meridiantrust.sentinel.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Investigation case management workflow. */
@RestController
@RequestMapping("/api/v1/cases")
@PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR','ADMIN')")
@Tag(name = "Cases", description = "Open, progress, link and close investigation cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    @Operation(summary = "Open a new case, optionally linking existing alerts")
    public ResponseEntity<CaseView> open(@Valid @RequestBody CaseCreateRequest request) {
        var created = caseService.open(request.customerRef(), request.title(), request.summary(),
                request.priority(), request.alertRefs());
        return ResponseEntity.status(HttpStatus.CREATED).body(caseService.view(created.getCaseRef()));
    }

    @GetMapping("/{caseRef}")
    @Operation(summary = "Get case detail with linked alerts")
    public CaseView detail(@PathVariable String caseRef) {
        return caseService.view(caseRef);
    }

    @GetMapping("/{caseRef}/audit")
    @Operation(summary = "Immutable audit trail for a case")
    public List<AuditEventView> audit(@PathVariable String caseRef) {
        return caseService.auditTrail(caseRef);
    }

    @PostMapping("/{caseRef}/alerts/{alertRef}")
    @Operation(summary = "Link an alert to a case")
    public CaseView link(@PathVariable String caseRef, @PathVariable String alertRef) {
        caseService.linkAlert(caseRef, alertRef);
        return caseService.view(caseRef);
    }

    @PatchMapping("/{caseRef}/assign")
    @Operation(summary = "Assign a case to an analyst")
    public CaseView assign(@PathVariable String caseRef, @RequestParam String assignee) {
        caseService.assign(caseRef, assignee);
        return caseService.view(caseRef);
    }

    @PatchMapping("/{caseRef}/status")
    @Operation(summary = "Transition a case's workflow status")
    public CaseView status(@PathVariable String caseRef, @Valid @RequestBody StatusChangeRequest request) {
        caseService.changeStatus(caseRef, CaseStatus.valueOf(request.status()), request.note());
        return caseService.view(caseRef);
    }

    @PostMapping("/{caseRef}/close")
    @Operation(summary = "Close a case with a resolution")
    public CaseView close(@PathVariable String caseRef, @RequestParam String resolution) {
        caseService.close(caseRef, resolution);
        return caseService.view(caseRef);
    }
}
