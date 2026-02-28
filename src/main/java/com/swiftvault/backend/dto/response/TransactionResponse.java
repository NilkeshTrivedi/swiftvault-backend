package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private Transaction.TransactionType type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;

    public TransactionResponse() {}

    public static TransactionResponse from(Transaction t) {
        TransactionResponse r = new TransactionResponse();
        r.transactionId = t.getTransactionId();
        r.fromAccount   = t.getFromAccount();
        r.toAccount     = t.getToAccount();
        r.type          = t.getType();
        r.amount        = t.getAmount();
        r.description   = t.getDescription();
        r.timestamp     = t.getTimestamp();
        return r;
    }

    public String                       getTransactionId() { return transactionId; }
    public String                       getFromAccount()   { return fromAccount; }
    public String                       getToAccount()     { return toAccount; }
    public Transaction.TransactionType  getType()          { return type; }
    public BigDecimal                   getAmount()        { return amount; }
    public String                       getDescription()   { return description; }
    public LocalDateTime                getTimestamp()     { return timestamp; }
}