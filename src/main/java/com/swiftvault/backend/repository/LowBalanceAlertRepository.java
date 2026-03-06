package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.LowBalanceAlert;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LowBalanceAlertRepository extends JpaRepository<LowBalanceAlert, String> {
    List<LowBalanceAlert> findByUserOrderByCreatedAtDesc(User user);
    List<LowBalanceAlert> findByUserAndReadFalseOrderByCreatedAtDesc(User user);
    long countByUserAndReadFalse(User user);
}