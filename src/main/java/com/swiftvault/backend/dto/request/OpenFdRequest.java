package com.swiftvault.backend.dto.request;

import com.swiftvault.backend.entity.FixedDeposit;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class OpenFdRequest {
    @NotBlank(message = "Account number is required")
    private String accountNumber;
    @NotNull @DecimalMin("1000.00")
    private BigDecimal amount;
    @NotNull @Min(7) @Max(3650)
    private Integer tenureDays;
    private FixedDeposit.PayoutOption payoutOption = FixedDeposit.PayoutOption.ON_MATURITY;
    private String nomineeName;

    public String getAccountNumber()            { return accountNumber; }
    public BigDecimal getAmount()               { return amount; }
    public Integer getTenureDays()              { return tenureDays; }
    public FixedDeposit.PayoutOption getPayoutOption() { return payoutOption; }
    public String getNomineeName()              { return nomineeName; }
    public void setAccountNumber(String v)      { this.accountNumber = v; }
    public void setAmount(BigDecimal v)         { this.amount = v; }
    public void setTenureDays(Integer v)        { this.tenureDays = v; }
    public void setPayoutOption(FixedDeposit.PayoutOption v) { this.payoutOption = v; }
    public void setNomineeName(String v)        { this.nomineeName = v; }
}
