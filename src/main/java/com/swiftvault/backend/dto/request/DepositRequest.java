package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// accountNumber now comes from the URL path variable in AccountController
// POST /api/accounts/{accountNumber}/deposit
public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum deposit is ₹1")
    private BigDecimal amount;

    private String description;

    public BigDecimal getAmount()       { return amount; }
    public String     getDescription()  { return description; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public void setDescription(String v){ this.description = v; }
}