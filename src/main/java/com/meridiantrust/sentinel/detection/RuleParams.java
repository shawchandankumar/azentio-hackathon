/*
 * Sentinel AML — Real-Time Money Laundering Detection Platform
 * Copyright (C) 2026 Chandan Kumar Shaw <shawchandankumar20@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
