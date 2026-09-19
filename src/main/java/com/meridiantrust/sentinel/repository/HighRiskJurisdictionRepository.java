package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.HighRiskJurisdiction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HighRiskJurisdictionRepository extends JpaRepository<HighRiskJurisdiction, Long> {
    Optional<HighRiskJurisdiction> findByCountryCodeIgnoreCaseAndActiveTrue(String countryCode);

    List<HighRiskJurisdiction> findByActiveTrue();
}
