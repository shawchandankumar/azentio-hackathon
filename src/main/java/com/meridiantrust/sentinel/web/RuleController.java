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
package com.meridiantrust.sentinel.web;

import com.meridiantrust.sentinel.dto.RuleConfigView;
import com.meridiantrust.sentinel.dto.RuleUpdateRequest;
import com.meridiantrust.sentinel.security.SecurityUtils;
import com.meridiantrust.sentinel.service.DetectionService;
import com.meridiantrust.sentinel.service.RuleConfigService;
import com.meridiantrust.sentinel.service.ViewMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Rule configuration API. All authenticated users can view rules; only ADMIN can
 * retune or toggle them (thresholds/windows change take effect without redeploy).
 * Also exposes a bulk detection trigger for re-evaluating all transactions.
 */
@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Rules", description = "Detection rule configuration and bulk detection")
public class RuleController {

    private final RuleConfigService ruleConfigService;
    private final DetectionService detectionService;
    private final ViewMapper viewMapper;

    public RuleController(RuleConfigService ruleConfigService, DetectionService detectionService,
                          ViewMapper viewMapper) {
        this.ruleConfigService = ruleConfigService;
        this.detectionService = detectionService;
        this.viewMapper = viewMapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR','ADMIN')")
    @Operation(summary = "List all detection rules and their configuration")
    public List<RuleConfigView> list() {
        return ruleConfigService.findAll().stream().map(viewMapper::toRuleView).toList();
    }

    @GetMapping("/{ruleCode}")
    @PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR','ADMIN')")
    @Operation(summary = "Get a single rule's configuration")
    public RuleConfigView get(@PathVariable String ruleCode) {
        return viewMapper.toRuleView(ruleConfigService.getByCode(ruleCode));
    }

    @PutMapping("/{ruleCode}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retune or toggle a rule (thresholds/windows/weight) without redeploy")
    public RuleConfigView update(@PathVariable String ruleCode, @Valid @RequestBody RuleUpdateRequest request) {
        var updated = ruleConfigService.update(ruleCode, request.enabled(), request.baseWeight(),
                request.severity(), request.paramsJson(), SecurityUtils.currentActor());
        return viewMapper.toRuleView(updated);
    }

    @PostMapping("/detect/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-evaluate every transaction through the rule engine (bulk)")
    public Map<String, Object> runBulkDetection() {
        int processed = detectionService.detectAll();
        return Map.of("processed", processed, "status", "completed");
    }
}
