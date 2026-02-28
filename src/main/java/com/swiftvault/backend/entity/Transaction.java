package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity — maps to the 'transactions' table.
 *
 * Transactions are IMMUTABLE — once created, never updated.
 * This is standard banking practice (audit trail integrity).
 * That's why there is no @PreUpdate and all fields use updatable = false.
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                // Indexes speed up the most common query: "all transactions for account X"
                @Index(name = "idx_txn_from_account", columnList = "from_account"),
                @Index(name = "idx_txn_timestamp",    columnList = "timestamp")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false, length = 30, updatable = false)
    private String transactionId;

    /**
     * from_account — the account that initiated the transaction.
     * For DEPOSIT: the account receiving money.
     * For WITHDRAW: the account losing money.
     * For TRANSFER: the account sending money.
     */
    @Column(name = "from_account", nullable = false, length = 20, updatable = false)
    private String fromAccount;

    /**
     * to_account — only set for TRANSFER transactions.
     * Null for DEPOSIT and WITHDRAW.
     */
    @Column(name = "to_account", length = 20, updatable = false)
    private String toAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "description", length = 255, updatable = false)
    private String description;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // ─── Enum ────────────────────────────────────────────────────────────────

    public enum TransactionType { DEPOSIT, WITHDRAW, TRANSFER }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}