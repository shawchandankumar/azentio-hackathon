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

import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import java.util.List;

/**
 * A configurable AML detection rule. Implementations are stateless Spring beans;
 * the engine invokes {@link #evaluate} for each incoming transaction with the
 * rule's live configuration so thresholds can change without a redeploy.
 */
public interface DetectionRule {

    /** The typology this rule detects. */
    RuleType type();

    /**
     * Evaluate a transaction against this rule.
     *
     * @param txn    the transaction under evaluation
     * @param config the live, enabled configuration for this rule
     * @return zero or more triggered hits (empty if nothing fired)
     */
    List<RuleHit> evaluate(Transaction txn, RuleConfig config);
}
