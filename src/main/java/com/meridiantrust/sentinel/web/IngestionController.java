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

import com.meridiantrust.sentinel.dto.AccountBatchRequest;
import com.meridiantrust.sentinel.dto.CustomerBatchRequest;
import com.meridiantrust.sentinel.dto.IngestionResult;
import com.meridiantrust.sentinel.dto.TransactionBatchRequest;
import com.meridiantrust.sentinel.dto.TransactionIngestRequest;
import com.meridiantrust.sentinel.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Ingestion API for customer, account and transaction data. Bulk endpoints load
 * data; the streaming endpoint ingests a single transaction and evaluates it
 * immediately. Restricted to ADMIN (system/data-loading role).
 */
@RestController
@RequestMapping("/api/v1/ingestion")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Ingestion", description = "Load customers, accounts and transactions")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/customers")
    @Operation(summary = "Bulk-ingest customer KYC records")
    public ResponseEntity<IngestionResult> customers(@Valid @RequestBody CustomerBatchRequest request) {
        return ResponseEntity.ok(ingestionService.ingestCustomers(request.customers()));
    }

    @PostMapping("/accounts")
    @Operation(summary = "Bulk-ingest account metadata")
    public ResponseEntity<IngestionResult> accounts(@Valid @RequestBody AccountBatchRequest request) {
        return ResponseEntity.ok(ingestionService.ingestAccounts(request.accounts()));
    }

    @PostMapping("/transactions")
    @Operation(summary = "Bulk-ingest transactions; set detect=true to run detection inline")
    public ResponseEntity<IngestionResult> transactions(
            @Valid @RequestBody TransactionBatchRequest request,
            @RequestParam(name = "detect", defaultValue = "false") boolean detect) {
        return ResponseEntity.ok(ingestionService.ingestTransactions(request.transactions(), detect));
    }

    @PostMapping("/transactions/stream")
    @Operation(summary = "Ingest a single transaction and evaluate it immediately (streaming)")
    public ResponseEntity<IngestionResult> stream(@Valid @RequestBody TransactionIngestRequest request) {
        IngestionResult result = ingestionService.ingestTransactions(List.of(request), true);
        HttpStatus status = result.rejected() > 0 ? HttpStatus.BAD_REQUEST : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }
}
