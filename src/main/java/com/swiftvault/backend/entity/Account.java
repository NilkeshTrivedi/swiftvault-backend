package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Account entity — maps to the 'accounts' table in MySQL.
 *
 * Uses BigDecimal instead of double for money.
 * NEVER use double/float for currency — rounding errors cause real financial bugs.
 * BigDecimal is exact. 0.1 + 0.2 = 0.3 (not 0.30000000000000004)
 *
 * @ManyToOne — many accounts can belong to one user (FK relationship)
 * @JoinColumn — specifies the foreign key column name in DB
 */
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    // ─── Constants ────────────────────────────────────────────────────────────
    public static final BigDecimal SAVINGS_INTEREST_RATE   = new BigDecimal("0.04");  // 4% p.a.
    public static final BigDecimal MINIMUM_BALANCE_SAVINGS = new BigDecimal("1000.00");
    public static final BigDecimal DAILY_WITHDRAW_LIMIT    = new BigDecimal("50000.00");
    public static final BigDecimal LOW_BALANCE_FEE         = new BigDecimal("100.00");

    @Id
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /**
     * @ManyToOne — this account belongs to ONE user
     * @JoinColumn — the FK column in 'accounts' table is 'user_id'
     * fetch = LAZY — don't load User data unless explicitly requested (performance)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "today_withdrawn", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal todayWithdrawn = BigDecimal.ZERO;

    @Column(name = "withdraw_date")
    private LocalDate withdrawDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_interest_applied")
    private LocalDateTime lastInterestApplied;

    // ─── Enums ───────────────────────────────────────────────────────────────

    public enum AccountType   { SAVINGS, CHECKING }
    public enum AccountStatus { ACTIVE, FROZEN, CLOSED }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.balance       == null) this.balance       = BigDecimal.ZERO;
        if (this.todayWithdrawn == null) this.todayWithdrawn = BigDecimal.ZERO;
        if (this.status        == null) this.status        = AccountStatus.ACTIVE;
    }

    // ─── Business Logic ───────────────────────────────────────────────────────

    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Withdraw with all business rule checks.
     *
     * Returns:
     *   "OK"            → success
     *   "INSUFFICIENT"  → not enough balance
     *   "DAILY_LIMIT"   → daily withdrawal limit exceeded
     *   "MIN_BALANCE"   → SAVINGS minimum balance violation
     */
    public String withdrawWithChecks(BigDecimal amount) {
        // Reset daily counter if it's a new day
        if (withdrawDate == null || !withdrawDate.equals(LocalDate.now())) {
            todayWithdrawn = BigDecimal.ZERO;
            withdrawDate   = LocalDate.now();
        }

        // Check daily limit
        if (todayWithdrawn.add(amount).compareTo(DAILY_WITHDRAW_LIMIT) > 0) {
            return "DAILY_LIMIT";
        }

        // Check minimum balance for SAVINGS
        if (type == AccountType.SAVINGS &&
                balance.subtract(amount).compareTo(MINIMUM_BALANCE_SAVINGS) < 0) {
            return "MIN_BALANCE";
        }

        // Check sufficient funds
        if (amount.compareTo(balance) > 0) {
            return "INSUFFICIENT";
        }

        this.balance        = balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        this.todayWithdrawn = todayWithdrawn.add(amount);
        return "OK";
    }

    public BigDecimal getRemainingDailyLimit() {
        if (withdrawDate == null || !withdrawDate.equals(LocalDate.now())) {
            return DAILY_WITHDRAW_LIMIT;
        }
        return DAILY_WITHDRAW_LIMIT.subtract(todayWithdrawn);
    }

    public String getDisplayName() {
        return (nickname != null && !nickname.isBlank())
                ? nickname + " (" + accountNumber + ")"
                : accountNumber;
    }
}