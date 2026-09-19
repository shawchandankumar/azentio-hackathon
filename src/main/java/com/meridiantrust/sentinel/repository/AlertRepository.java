package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.Alert;
import com.meridiantrust.sentinel.domain.enums.AlertStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByAlertRef(String alertRef);

    Optional<Alert> findByDedupKey(String dedupKey);

    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    List<Alert> findByCustomerId(Long customerId);

    List<Alert> findByCaseFileId(Long caseId);

    long countByStatus(AlertStatus status);
}
