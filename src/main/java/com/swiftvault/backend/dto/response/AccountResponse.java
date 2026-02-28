package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public AccountResponse() {}

    public static AccountResponse from(Account account) {
        AccountResponse r = new AccountResponse();
        r.accountNumber      = account.getAccountNumber();
        r.displayName        = account.getDisplayName();
        r.userId             = account.getUser().getUserId();
        r.userFullName       = account.getUser().getFullName();
        r.balance            = account.getBalance();
        r.type               = account.getType();
        r.status             = account.getStatus();
        r.nickname           = account.getNickname();
        r.remainingDailyLimit= account.getRemainingDailyLimit();
        r.createdAt          = account.getCreatedAt();
        return r;
    }

    public String                getAccountNumber()       { return accountNumber; }
    public String                getDisplayName()         { return displayName; }
    public String                getUserId()              { return userId; }
    public String                getUserFullName()        { return userFullName; }
    public BigDecimal            getBalance()             { return balance; }
    public Account.AccountType   getType()                { return type; }
    public Account.AccountStatus getStatus()              { return status; }
    public String                getNickname()            { return nickname; }
    public BigDecimal            getRemainingDailyLimit() { return remainingDailyLimit; }
    public LocalDateTime         getCreatedAt()           { return createdAt; }
}