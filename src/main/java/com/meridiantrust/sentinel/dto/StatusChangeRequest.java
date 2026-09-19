package com.meridiantrust.sentinel.dto;

import jakarta.validation.constraints.NotBlank;

/** Generic status transition request for alerts or cases. */
public record StatusChangeRequest(
        @NotBlank String status,
        String note) {
}
