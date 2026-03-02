package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.Loan;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, String> {
    List<Loan> findByUser(User user);
    List<Loan> findByUserAndStatus(User user, Loan.LoanStatus status);
    List<Loan> findByStatus(Loan.LoanStatus status);
    @Query("SELECT l FROM Loan l WHERE l.nextEmiDate <= :today AND l.status = 'ACTIVE'")
    List<Loan> findDueEmis(@Param("today") LocalDate today);
    long countByStatus(Loan.LoanStatus status);
}
