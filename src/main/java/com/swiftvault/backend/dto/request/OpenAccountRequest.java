package com.swiftvault.backend.dto.request;

import com.swiftvault.backend.entity.Account;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class OpenAccountRequest {
    @NotNull(message = "Account type is required")
    private Account.AccountType type;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;

    public Account.AccountType getType()           { return type; }
    public BigDecimal          getInitialDeposit() { return initialDeposit; }
    public void setType(Account.AccountType v)     { this.type = v; }
    public void setInitialDeposit(BigDecimal v)    { this.initialDeposit = v; }
}