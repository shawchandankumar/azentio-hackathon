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
