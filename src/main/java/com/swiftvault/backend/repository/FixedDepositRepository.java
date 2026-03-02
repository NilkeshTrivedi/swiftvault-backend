package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.FixedDeposit;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface FixedDepositRepository extends JpaRepository<FixedDeposit, String> {
    List<FixedDeposit> findByUser(User user);
    List<FixedDeposit> findByUserAndStatus(User user, FixedDeposit.FdStatus status);
    @Query("SELECT fd FROM FixedDeposit fd WHERE fd.maturityDate <= :today AND fd.status = 'ACTIVE'")
    List<FixedDeposit> findMaturedFds(@Param("today") LocalDate today);
    long countByUserAndStatus(User user, FixedDeposit.FdStatus status);
}
