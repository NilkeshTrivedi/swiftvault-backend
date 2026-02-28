package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DepositRequest {
    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum deposit is 1")
    private BigDecimal amount;

    private String description;

    public String    getAccountNumber() { return accountNumber; }
    public BigDecimal getAmount()       { return amount; }
    public String    getDescription()   { return description; }
    public void setAccountNumber(String v) { this.accountNumber = v; }
    public void setAmount(BigDecimal v)    { this.amount = v; }
    public void setDescription(String v)   { this.description = v; }
}