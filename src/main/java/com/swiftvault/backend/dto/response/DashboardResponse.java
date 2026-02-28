package com.swiftvault.backend.dto.response;

import java.math.BigDecimal;

public class DashboardResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalAccounts;
    private long activeAccounts;
    private long totalTransactions;
    private BigDecimal totalBankBalance;

    public DashboardResponse() {}

    public long       getTotalUsers()        { return totalUsers; }
    public long       getActiveUsers()       { return activeUsers; }
    public long       getTotalAccounts()     { return totalAccounts; }
    public long       getActiveAccounts()    { return activeAccounts; }
    public long       getTotalTransactions() { return totalTransactions; }
    public BigDecimal getTotalBankBalance()  { return totalBankBalance; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final DashboardResponse r = new DashboardResponse();
        public Builder totalUsers(long v)          { r.totalUsers = v; return this; }
        public Builder activeUsers(long v)         { r.activeUsers = v; return this; }
        public Builder totalAccounts(long v)       { r.totalAccounts = v; return this; }
        public Builder activeAccounts(long v)      { r.activeAccounts = v; return this; }
        public Builder totalTransactions(long v)   { r.totalTransactions = v; return this; }
        public Builder totalBankBalance(BigDecimal v){ r.totalBankBalance = v; return this; }
        public DashboardResponse build()           { return r; }
    }
}