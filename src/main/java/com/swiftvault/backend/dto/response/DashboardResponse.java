package com.swiftvault.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalAccounts;
    private long activeAccounts;
    private long totalTransactions;
    private BigDecimal totalBankBalance;
}