package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT t FROM Transaction t WHERE " +
            "t.fromAccount = :accountNumber OR t.toAccount = :accountNumber " +
            "ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.fromAccount = :accountNumber OR t.toAccount = :accountNumber) " +
            "AND t.type = :type ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountNumberAndType(
            @Param("accountNumber") String accountNumber,
            @Param("type") Transaction.TransactionType type);

    @Query(value = "SELECT * FROM transactions WHERE " +
            "from_account = :accountNumber OR to_account = :accountNumber " +
            "ORDER BY timestamp DESC LIMIT 5",
            nativeQuery = true)
    List<Transaction> findTop5ByAccountNumber(@Param("accountNumber") String accountNumber);
}