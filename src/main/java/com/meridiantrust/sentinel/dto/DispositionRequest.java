package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.Disposition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Analyst disposition of an alert (clear/confirm). Reason is mandatory for audit. */
public record DispositionRequest(
        @NotNull Disposition disposition,
        @NotBlank String reason) {
}
