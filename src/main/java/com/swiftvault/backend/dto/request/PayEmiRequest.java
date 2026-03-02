package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.*;

public class PayEmiRequest {
    @NotBlank private String loanId;
    @NotBlank private String accountNumber;
    @NotBlank private String transactionPin;

    public String getLoanId()           { return loanId; }
    public String getAccountNumber()    { return accountNumber; }
    public String getTransactionPin()   { return transactionPin; }
    public void setLoanId(String v)     { this.loanId = v; }
    public void setAccountNumber(String v){ this.accountNumber = v; }
    public void setTransactionPin(String v){ this.transactionPin = v; }
}
