package com.meridiantrust.sentinel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Batch of transaction records for bulk ingestion. */
public record TransactionBatchRequest(
        @NotEmpty @Valid List<TransactionIngestRequest> transactions) {
}
