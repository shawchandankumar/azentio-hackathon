package com.meridiantrust.sentinel.dto;

import java.time.Instant;
import java.util.List;

/** Consistent JSON error envelope returned for all non-2xx responses. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {

    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, path, details);
    }
}
