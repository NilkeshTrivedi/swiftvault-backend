package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.RecurringDeposit;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface RecurringDepositRepository extends JpaRepository<RecurringDeposit, String> {
    List<RecurringDeposit> findByUser(User user);
    List<RecurringDeposit> findByUserAndStatus(User user, RecurringDeposit.RdStatus status);
    @Query("SELECT rd FROM RecurringDeposit rd WHERE rd.nextDueDate <= :today AND rd.status = 'ACTIVE'")
    List<RecurringDeposit> findDueRds(@Param("today") LocalDate today);
    @Query("SELECT rd FROM RecurringDeposit rd WHERE rd.maturityDate <= :today AND rd.status = 'ACTIVE'")
    List<RecurringDeposit> findMaturedRds(@Param("today") LocalDate today);
}
