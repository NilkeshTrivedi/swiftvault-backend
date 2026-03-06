// ═══════════════════════════════════════════════════════════════════════════
// FILE: entity/TransactionLimit.java
// Path: src/main/java/com/swiftvault/backend/entity/
// ═══════════════════════════════════════════════════════════════════════════
//
// FEATURE: Transaction Limit Controls
//
// Users configure per-account limits:
//   - maxSingleTransaction : max amount for a single transfer/withdrawal
//   - dailyLimit          : max total debit per calendar day
//   - monthlyLimit        : max total debit per calendar month
//
// Checked in AccountServiceImpl before every transfer/withdrawal.
// If any limit is exceeded → transaction is rejected with a clear error.
//
// ═══════════════════════════════════════════════════════════════════════════

package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_limits")
public class TransactionLimit {

    /** One row per account — accountNumber is the primary key */
    @Id
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Default: ₹50,000. 0 = no limit. */
    @Column(name = "max_single_transaction", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxSingleTransaction = new BigDecimal("50000.00");

    /** Default: ₹1,00,000 per day. 0 = no limit. */
    @Column(name = "daily_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit = new BigDecimal("100000.00");

    /** Default: ₹5,00,000 per month. 0 = no limit. */
    @Column(name = "monthly_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyLimit = new BigDecimal("500000.00");

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public TransactionLimit() {}

    public String        getAccountNumber()        { return accountNumber; }
    public User          getUser()                 { return user; }
    public BigDecimal    getMaxSingleTransaction() { return maxSingleTransaction; }
    public BigDecimal    getDailyLimit()           { return dailyLimit; }
    public BigDecimal    getMonthlyLimit()         { return monthlyLimit; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }

    public void setAccountNumber(String v)            { this.accountNumber = v; }
    public void setUser(User v)                       { this.user = v; }
    public void setMaxSingleTransaction(BigDecimal v) { this.maxSingleTransaction = v; }
    public void setDailyLimit(BigDecimal v)           { this.dailyLimit = v; }
    public void setMonthlyLimit(BigDecimal v)         { this.monthlyLimit = v; }
    public void setUpdatedAt(LocalDateTime v)         { this.updatedAt = v; }
}