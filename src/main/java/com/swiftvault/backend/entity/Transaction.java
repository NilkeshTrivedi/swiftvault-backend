package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_txn_from_account", columnList = "from_account"),
                @Index(name = "idx_txn_timestamp",    columnList = "timestamp")
        }
)
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false, length = 30, updatable = false)
    private String transactionId;

    @Column(name = "from_account", nullable = false, length = 20, updatable = false)
    private String fromAccount;

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

    public enum TransactionType { DEPOSIT, WITHDRAW, TRANSFER }

    public Transaction() {}

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String          getTransactionId() { return transactionId; }
    public String          getFromAccount()   { return fromAccount; }
    public String          getToAccount()     { return toAccount; }
    public TransactionType getType()          { return type; }
    public BigDecimal      getAmount()        { return amount; }
    public String          getDescription()   { return description; }
    public LocalDateTime   getTimestamp()     { return timestamp; }

    // Setters
    public void setTransactionId(String v) { this.transactionId = v; }
    public void setFromAccount(String v)   { this.fromAccount = v; }
    public void setToAccount(String v)     { this.toAccount = v; }
    public void setType(TransactionType v) { this.type = v; }
    public void setAmount(BigDecimal v)    { this.amount = v; }
    public void setDescription(String v)   { this.description = v; }
    public void setTimestamp(LocalDateTime v) { this.timestamp = v; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String transactionId;
        private String fromAccount;
        private String toAccount;
        private TransactionType type;
        private BigDecimal amount;
        private String description;

        public Builder transactionId(String v)  { this.transactionId = v; return this; }
        public Builder fromAccount(String v)    { this.fromAccount = v; return this; }
        public Builder toAccount(String v)      { this.toAccount = v; return this; }
        public Builder type(TransactionType v)  { this.type = v; return this; }
        public Builder amount(BigDecimal v)     { this.amount = v; return this; }
        public Builder description(String v)    { this.description = v; return this; }

        public Transaction build() {
            Transaction t = new Transaction();
            t.transactionId = this.transactionId;
            t.fromAccount   = this.fromAccount;
            t.toAccount     = this.toAccount;
            t.type          = this.type;
            t.amount        = this.amount;
            t.description   = this.description;
            return t;
        }
    }
}