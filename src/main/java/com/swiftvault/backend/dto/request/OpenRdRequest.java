package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class OpenRdRequest {
    @NotBlank(message = "Account number is required")
    private String accountNumber;
    @NotNull @DecimalMin("500.00")
    private BigDecimal monthlyInstallment;
    @NotNull @Min(6) @Max(120)
    private Integer tenureMonths;
    private String nomineeName;

    public String getAccountNumber()            { return accountNumber; }
    public BigDecimal getMonthlyInstallment()   { return monthlyInstallment; }
    public Integer getTenureMonths()            { return tenureMonths; }
    public String getNomineeName()              { return nomineeName; }
    public void setAccountNumber(String v)      { this.accountNumber = v; }
    public void setMonthlyInstallment(BigDecimal v){ this.monthlyInstallment = v; }
    public void setTenureMonths(Integer v)      { this.tenureMonths = v; }
    public void setNomineeName(String v)        { this.nomineeName = v; }
}
