package com.meridiantrust.sentinel.repository;

import com.meridiantrust.sentinel.domain.Transaction;
import com.meridiantrust.sentinel.domain.enums.Direction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTxnRef(String txnRef);

    boolean existsByTxnRef(String txnRef);

    /** Full transaction timeline for a customer across all their accounts, newest first. */
    List<Transaction> findByAccount_Customer_IdOrderByBookedAtDesc(Long customerId);

    /** All transactions for an account within a time window, oldest first. */
    List<Transaction> findByAccountIdAndBookedAtBetweenOrderByBookedAtAsc(
            Long accountId, Instant start, Instant end);

    /** Transactions of a given direction for an account within a window. */
    List<Transaction> findByAccountIdAndDirectionAndBookedAtBetweenOrderByBookedAtAsc(
            Long accountId, Direction direction, Instant start, Instant end);

    /** All transactions across every account owned by a customer within a window. */
    @Query("""
            select t from Transaction t
            where t.account.customer.id = :customerId
              and t.bookedAt between :start and :end
            order by t.bookedAt asc
            """)
    List<Transaction> findByCustomerAndWindow(
            @Param("customerId") Long customerId,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
