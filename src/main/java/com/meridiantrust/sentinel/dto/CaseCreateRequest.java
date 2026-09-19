package com.meridiantrust.sentinel.dto;

import com.meridiantrust.sentinel.domain.enums.CasePriority;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Request to open a new investigation case. */
public record CaseCreateRequest(
        @NotBlank String customerRef,
        @NotBlank String title,
        String summary,
        CasePriority priority,
        List<String> alertRefs) {
}
