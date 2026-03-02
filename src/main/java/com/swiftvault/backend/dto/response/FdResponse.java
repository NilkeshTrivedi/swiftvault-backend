package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.FixedDeposit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FdResponse {
    private String fdId;
    private String sourceAccount;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureDays;
    private BigDecimal maturityAmount;
    private BigDecimal interestEarned;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private FixedDeposit.FdStatus status;
    private FixedDeposit.PayoutOption payoutOption;
    private String nomineeName;
    private Boolean prematureWithdrawal;
    private LocalDateTime createdAt;

    public static FdResponse from(FixedDeposit fd) {
        FdResponse r = new FdResponse();
        r.fdId = fd.getFdId();
        r.sourceAccount = fd.getSourceAccount().getAccountNumber();
        r.principalAmount = fd.getPrincipalAmount();
        r.interestRate = fd.getInterestRate();
        r.tenureDays = fd.getTenureDays();
        r.maturityAmount = fd.getMaturityAmount();
        r.interestEarned = fd.getMaturityAmount().subtract(fd.getPrincipalAmount());
        r.startDate = fd.getStartDate();
        r.maturityDate = fd.getMaturityDate();
        r.status = fd.getStatus();
        r.payoutOption = fd.getPayoutOption();
        r.nomineeName = fd.getNomineeName();
        r.prematureWithdrawal = fd.getPrematureWithdrawal();
        r.createdAt = fd.getCreatedAt();
        return r;
    }

    public String getFdId()                     { return fdId; }
    public String getSourceAccount()            { return sourceAccount; }
    public BigDecimal getPrincipalAmount()      { return principalAmount; }
    public BigDecimal getInterestRate()         { return interestRate; }
    public Integer getTenureDays()              { return tenureDays; }
    public BigDecimal getMaturityAmount()       { return maturityAmount; }
    public BigDecimal getInterestEarned()       { return interestEarned; }
    public LocalDate getStartDate()             { return startDate; }
    public LocalDate getMaturityDate()          { return maturityDate; }
    public FixedDeposit.FdStatus getStatus()    { return status; }
    public FixedDeposit.PayoutOption getPayoutOption() { return payoutOption; }
    public String getNomineeName()              { return nomineeName; }
    public Boolean getPrematureWithdrawal()     { return prematureWithdrawal; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
}
