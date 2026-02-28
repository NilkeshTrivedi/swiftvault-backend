package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    public static final BigDecimal SAVINGS_INTEREST_RATE   = new BigDecimal("0.04");
    public static final BigDecimal MINIMUM_BALANCE_SAVINGS = new BigDecimal("1000.00");
    public static final BigDecimal DAILY_WITHDRAW_LIMIT    = new BigDecimal("50000.00");
    public static final BigDecimal LOW_BALANCE_FEE         = new BigDecimal("100.00");

    @Id
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "today_withdrawn", precision = 15, scale = 2)
    private BigDecimal todayWithdrawn = BigDecimal.ZERO;

    @Column(name = "withdraw_date")
    private LocalDate withdrawDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_interest_applied")
    private LocalDateTime lastInterestApplied;

    public enum AccountType   { SAVINGS, CHECKING }
    public enum AccountStatus { ACTIVE, FROZEN, CLOSED }

    public Account() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.balance        == null) this.balance        = BigDecimal.ZERO;
        if (this.todayWithdrawn == null) this.todayWithdrawn = BigDecimal.ZERO;
        if (this.status         == null) this.status         = AccountStatus.ACTIVE;
    }

    // Getters
    public String        getAccountNumber()      { return accountNumber; }
    public User          getUser()               { return user; }
    public BigDecimal    getBalance()            { return balance; }
    public AccountType   getType()               { return type; }
    public AccountStatus getStatus()             { return status; }
    public String        getNickname()           { return nickname; }
    public BigDecimal    getTodayWithdrawn()     { return todayWithdrawn; }
    public LocalDate     getWithdrawDate()       { return withdrawDate; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getLastInterestApplied(){ return lastInterestApplied; }

    // Setters
    public void setAccountNumber(String v)       { this.accountNumber = v; }
    public void setUser(User v)                  { this.user = v; }
    public void setBalance(BigDecimal v)         { this.balance = v; }
    public void setType(AccountType v)           { this.type = v; }
    public void setStatus(AccountStatus v)       { this.status = v; }
    public void setNickname(String v)            { this.nickname = v; }
    public void setTodayWithdrawn(BigDecimal v)  { this.todayWithdrawn = v; }
    public void setWithdrawDate(LocalDate v)     { this.withdrawDate = v; }
    public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }
    public void setLastInterestApplied(LocalDateTime v) { this.lastInterestApplied = v; }

    // Business logic
    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean withdraw(BigDecimal amount) {
        if (amount.compareTo(balance) > 0) return false;
        this.balance = this.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        return true;
    }

    public String withdrawWithChecks(BigDecimal amount) {
        if (withdrawDate == null || !withdrawDate.equals(LocalDate.now())) {
            todayWithdrawn = BigDecimal.ZERO;
            withdrawDate   = LocalDate.now();
        }
        if (todayWithdrawn.add(amount).compareTo(DAILY_WITHDRAW_LIMIT) > 0) return "DAILY_LIMIT";
        if (type == AccountType.SAVINGS &&
                balance.subtract(amount).compareTo(MINIMUM_BALANCE_SAVINGS) < 0) return "MIN_BALANCE";
        if (amount.compareTo(balance) > 0) return "INSUFFICIENT";
        this.balance        = balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        this.todayWithdrawn = todayWithdrawn.add(amount);
        return "OK";
    }

    public BigDecimal getRemainingDailyLimit() {
        if (withdrawDate == null || !withdrawDate.equals(LocalDate.now())) return DAILY_WITHDRAW_LIMIT;
        return DAILY_WITHDRAW_LIMIT.subtract(todayWithdrawn);
    }

    public String getDisplayName() {
        return (nickname != null && !nickname.isBlank())
                ? nickname + " (" + accountNumber + ")" : accountNumber;
    }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String accountNumber;
        private User user;
        private BigDecimal balance = BigDecimal.ZERO;
        private AccountType type;
        private AccountStatus status = AccountStatus.ACTIVE;
        private String nickname;

        public Builder accountNumber(String v) { this.accountNumber = v; return this; }
        public Builder user(User v)            { this.user = v; return this; }
        public Builder balance(BigDecimal v)   { this.balance = v; return this; }
        public Builder type(AccountType v)     { this.type = v; return this; }
        public Builder status(AccountStatus v) { this.status = v; return this; }
        public Builder nickname(String v)      { this.nickname = v; return this; }

        public Account build() {
            Account a = new Account();
            a.accountNumber  = this.accountNumber;
            a.user           = this.user;
            a.balance        = this.balance != null ? this.balance : BigDecimal.ZERO;
            a.type           = this.type;
            a.status         = this.status != null ? this.status : AccountStatus.ACTIVE;
            a.nickname       = this.nickname;
            a.todayWithdrawn = BigDecimal.ZERO;
            return a;
        }
    }
}