package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.TransactionLimit;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, String> {
    Optional<TransactionLimit> findByAccountNumber(String accountNumber);
    List<TransactionLimit> findByUser(User user);
}