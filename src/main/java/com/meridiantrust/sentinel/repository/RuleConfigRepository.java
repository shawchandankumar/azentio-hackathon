package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.RuleConfig;
import com.meridiantrust.sentinel.domain.enums.RuleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleConfigRepository extends JpaRepository<RuleConfig, Long> {
    Optional<RuleConfig> findByRuleCode(String ruleCode);

    Optional<RuleConfig> findByRuleType(RuleType ruleType);
}
