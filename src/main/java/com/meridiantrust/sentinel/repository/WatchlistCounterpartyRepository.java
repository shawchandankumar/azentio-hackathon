package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.WatchlistCounterparty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistCounterpartyRepository extends JpaRepository<WatchlistCounterparty, Long> {
    List<WatchlistCounterparty> findByActiveTrue();
}
