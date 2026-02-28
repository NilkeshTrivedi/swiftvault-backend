package com.swiftvault.backend.repository;

import com.swiftvault.backend.entity.Account;
import com.swiftvault.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByUser(User user);

    List<Account> findByUserAndStatus(User user, Account.AccountStatus status);

    Optional<Account> findByAccountNumber(String accountNumber);

    // Sum all active account balances — used for admin dashboard
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.status = 'ACTIVE'")
    BigDecimal getTotalBankBalance();

    // Count active accounts
    long countByStatus(Account.AccountStatus status);

    // Find all SAVINGS accounts for monthly interest processing
    List<Account> findByTypeAndStatus(Account.AccountType type, Account.AccountStatus status);
}