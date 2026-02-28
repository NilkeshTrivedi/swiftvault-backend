package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private Transaction.TransactionType type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .fromAccount(t.getFromAccount())
                .toAccount(t.getToAccount())
                .type(t.getType())
                .amount(t.getAmount())
                .description(t.getDescription())
                .timestamp(t.getTimestamp())
                .build();
    }
}