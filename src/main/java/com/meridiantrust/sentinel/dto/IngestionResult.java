package com.meridiantrust.sentinel.dto;

import java.util.ArrayList;
import java.util.List;

/** Outcome of a (possibly batch) ingestion request. */
public record IngestionResult(
        int accepted,
        int rejected,
        List<String> errors,
        List<String> alertRefs) {

    /** Mutable accumulator used while processing a batch. */
    public static final class Builder {
        private int accepted;
        private int rejected;
        private final List<String> errors = new ArrayList<>();
        private final List<String> alertRefs = new ArrayList<>();

        public void accept() {
            accepted++;
        }

        public void reject(String reason) {
            rejected++;
            errors.add(reason);
        }

        public void addAlert(String ref) {
            alertRefs.add(ref);
        }

        public IngestionResult build() {
            return new IngestionResult(accepted, rejected, List.copyOf(errors), List.copyOf(alertRefs));
        }
    }
}
