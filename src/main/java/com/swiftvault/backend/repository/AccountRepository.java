package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.Account;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByUser(User user);

    List<Account> findByUserAndStatus(User user, Account.AccountStatus status);

    Optional<Account> findByAccountNumber(String accountNumber);

    // Pass enum as parameter — safest approach with EnumType.STRING
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.status = :status")
    BigDecimal getTotalBankBalanceByStatus(@Param("status") Account.AccountStatus status);

    long countByStatus(Account.AccountStatus status);

    List<Account> findByTypeAndStatus(Account.AccountType type, Account.AccountStatus status);
}