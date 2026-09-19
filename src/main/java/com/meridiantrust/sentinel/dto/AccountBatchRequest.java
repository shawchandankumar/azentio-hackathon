package com.meridiantrust.sentinel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Batch of account records for bulk ingestion. */
public record AccountBatchRequest(
        @NotEmpty @Valid List<AccountIngestRequest> accounts) {
}
