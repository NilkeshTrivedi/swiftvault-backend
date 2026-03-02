package com.swiftvault.backend.dto.request;

import com.swiftvault.backend.entity.Loan;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class LoanApplicationRequest {
    @NotNull
    private Loan.LoanType loanType;
    @NotNull @DecimalMin("10000.00")
    private BigDecimal loanAmount;
    @NotNull @Min(6) @Max(360)
    private Integer tenureMonths;
    @NotBlank
    private String accountNumber;
    @NotBlank @Size(min = 10, max = 500)
    private String purpose;

    public Loan.LoanType getLoanType()          { return loanType; }
    public BigDecimal getLoanAmount()           { return loanAmount; }
    public Integer getTenureMonths()            { return tenureMonths; }
    public String getAccountNumber()            { return accountNumber; }
    public String getPurpose()                  { return purpose; }
    public void setLoanType(Loan.LoanType v)    { this.loanType = v; }
    public void setLoanAmount(BigDecimal v)     { this.loanAmount = v; }
    public void setTenureMonths(Integer v)      { this.tenureMonths = v; }
    public void setAccountNumber(String v)      { this.accountNumber = v; }
    public void setPurpose(String v)            { this.purpose = v; }
}
