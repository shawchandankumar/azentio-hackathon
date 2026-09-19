package com.meridiantrust.sentinel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Batch of customer records for bulk ingestion. */
public record CustomerBatchRequest(
        @NotEmpty @Valid List<CustomerIngestRequest> customers) {
}
