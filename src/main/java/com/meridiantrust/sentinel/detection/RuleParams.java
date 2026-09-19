package com.meridiantrust.sentinel.detection;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/**
 * Read-only view over a rule's {@code params_json} configuration blob with
 * typed accessors and sensible fallbacks.
 */
public class RuleParams {

    private final JsonNode node;

    public RuleParams(JsonNode node) {
        this.node = node;
    }

    public BigDecimal getDecimal(String key, BigDecimal fallback) {
        JsonNode v = node.get(key);
        return v == null || v.isNull() ? fallback : new BigDecimal(v.asText());
    }

    public int getInt(String key, int fallback) {
        JsonNode v = node.get(key);
        return v == null || v.isNull() ? fallback : v.asInt();
    }

    public long getLong(String key, long fallback) {
        JsonNode v = node.get(key);
        return v == null || v.isNull() ? fallback : v.asLong();
    }

    public double getDouble(String key, double fallback) {
        JsonNode v = node.get(key);
        return v == null || v.isNull() ? fallback : v.asDouble();
    }
}
