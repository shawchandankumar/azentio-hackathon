package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.CaseFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<CaseFile, Long> {
    Optional<CaseFile> findByCaseRef(String caseRef);
}
