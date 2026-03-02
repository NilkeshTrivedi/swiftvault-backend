package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.*;

public class FdWithdrawRequest {
    @NotBlank private String fdId;
    @NotBlank private String transactionPin;

    public String getFdId()             { return fdId; }
    public String getTransactionPin()   { return transactionPin; }
    public void setFdId(String v)       { this.fdId = v; }
    public void setTransactionPin(String v){ this.transactionPin = v; }
}
