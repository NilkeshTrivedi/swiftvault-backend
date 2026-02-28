package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TransferRequest {
    @NotBlank(message = "From account is required")
    private String fromAccount;
    private String toAccount;
    private String toEmail;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer is 1")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "Transaction PIN is required")
    private String transactionPin;

    public String    getFromAccount()    { return fromAccount; }
    public String    getToAccount()      { return toAccount; }
    public String    getToEmail()        { return toEmail; }
    public BigDecimal getAmount()        { return amount; }
    public String    getDescription()    { return description; }
    public String    getTransactionPin() { return transactionPin; }
    public void setFromAccount(String v)    { this.fromAccount = v; }
    public void setToAccount(String v)      { this.toAccount = v; }
    public void setToEmail(String v)        { this.toEmail = v; }
    public void setAmount(BigDecimal v)     { this.amount = v; }
    public void setDescription(String v)    { this.description = v; }
    public void setTransactionPin(String v) { this.transactionPin = v; }
}