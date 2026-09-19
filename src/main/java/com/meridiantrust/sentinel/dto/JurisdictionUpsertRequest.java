package com.meridiantrust.sentinel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Upsert a high-risk / sanctioned jurisdiction (Business Rule 4). */
public record JurisdictionUpsertRequest(
        @NotBlank @Size(min = 2, max = 2) String countryCode,
        @NotBlank String countryName,
        String category,
        Boolean active,
        String notes) {
}
