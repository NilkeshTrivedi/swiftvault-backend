package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.Account;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {
    private String accountNumber;
    private String displayName;
    private String userId;
    private String userFullName;
    private BigDecimal balance;
    private Account.AccountType type;
    private Account.AccountStatus status;
    private String nickname;
    private BigDecimal remainingDailyLimit;
    private LocalDateTime createdAt;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .displayName(account.getDisplayName())
                .userId(account.getUser().getUserId())
                .userFullName(account.getUser().getFullName())
                .balance(account.getBalance())
                .type(account.getType())
                .status(account.getStatus())
                .nickname(account.getNickname())
                .remainingDailyLimit(account.getRemainingDailyLimit())
                .createdAt(account.getCreatedAt())
                .build();
    }
}