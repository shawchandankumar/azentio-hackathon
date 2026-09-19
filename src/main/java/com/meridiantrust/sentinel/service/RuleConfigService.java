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
package com.meridiantrust.sentinel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridiantrust.sentinel.detection.RuleParams;
import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import com.meridiantrust.sentinel.repository.RuleConfigRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides live, tunable rule configuration to the detection engine and the
 * admin API. Thresholds/windows are read from the DB on each evaluation so
 * changes take effect without a redeploy.
 */
@Service
public class RuleConfigService {

    private static final Logger log = LoggerFactory.getLogger(RuleConfigService.class);

    private final RuleConfigRepository repository;
    private final ObjectMapper objectMapper;

    public RuleConfigService(RuleConfigRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<RuleConfig> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<RuleConfig> findEnabledByType(RuleType type) {
        return repository.findByRuleType(type).filter(RuleConfig::isEnabled);
    }

    @Transactional(readOnly = true)
    public RuleConfig getByCode(String ruleCode) {
        return repository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown rule code: " + ruleCode));
    }

    /** Parse the {@code params_json} blob of a rule config into a typed view. */
    public RuleParams params(RuleConfig config) {
        try {
            JsonNode node = objectMapper.readTree(
                    config.getParamsJson() == null || config.getParamsJson().isBlank()
                            ? "{}" : config.getParamsJson());
            return new RuleParams(node);
        } catch (Exception e) {
            log.warn("Malformed params_json for rule {} — falling back to defaults: {}",
                    config.getRuleCode(), e.getMessage());
            return new RuleParams(objectMapper.createObjectNode());
        }
    }

    /** Update tunable fields of a rule from the admin API. */
    @Transactional
    public RuleConfig update(String ruleCode, Boolean enabled, Integer baseWeight,
                             String severity, String paramsJson, String actor) {
        RuleConfig config = getByCode(ruleCode);
        if (enabled != null) {
            config.setEnabled(enabled);
        }
        if (baseWeight != null) {
            config.setBaseWeight(baseWeight);
        }
        if (severity != null) {
            config.setSeverity(com.meridiantrust.sentinel.domain.enums.Severity.valueOf(severity));
        }
        if (paramsJson != null) {
            // validate it parses before persisting
            try {
                objectMapper.readTree(paramsJson);
            } catch (Exception e) {
                throw new IllegalArgumentException("paramsJson is not valid JSON: " + e.getMessage());
            }
            config.setParamsJson(paramsJson);
        }
        config.setUpdatedBy(actor);
        RuleConfig saved = repository.save(config);
        log.info("Rule {} reconfigured by {} (enabled={}, baseWeight={})",
                ruleCode, actor, saved.isEnabled(), saved.getBaseWeight());
        return saved;
    }
}
