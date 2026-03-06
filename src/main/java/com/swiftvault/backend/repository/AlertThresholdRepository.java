package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.AlertThreshold;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, String> {
    Optional<AlertThreshold> findByAccountNumber(String accountNumber);
    List<AlertThreshold> findByUser(User user);
}