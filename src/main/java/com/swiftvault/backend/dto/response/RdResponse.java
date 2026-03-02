package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.RecurringDeposit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RdResponse {
    private String rdId;
    private String sourceAccount;
    private BigDecimal monthlyInstallment;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private BigDecimal totalDeposited;
    private BigDecimal maturityAmount;
    private Integer installmentsPaid;
    private Integer installmentsMissed;
    private Integer installmentsRemaining;
    private LocalDate nextDueDate;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private RecurringDeposit.RdStatus status;
    private String nomineeName;
    private LocalDateTime createdAt;

    public static RdResponse from(RecurringDeposit rd) {
        RdResponse r = new RdResponse();
        r.rdId = rd.getRdId();
        r.sourceAccount = rd.getSourceAccount().getAccountNumber();
        r.monthlyInstallment = rd.getMonthlyInstallment();
        r.tenureMonths = rd.getTenureMonths();
        r.interestRate = rd.getInterestRate();
        r.totalDeposited = rd.getTotalDeposited();
        r.maturityAmount = rd.getMaturityAmount();
        r.installmentsPaid = rd.getInstallmentsPaid();
        r.installmentsMissed = rd.getInstallmentsMissed();
        r.installmentsRemaining = rd.getTenureMonths() - rd.getInstallmentsPaid() - rd.getInstallmentsMissed();
        r.nextDueDate = rd.getNextDueDate();
        r.startDate = rd.getStartDate();
        r.maturityDate = rd.getMaturityDate();
        r.status = rd.getStatus();
        r.nomineeName = rd.getNomineeName();
        r.createdAt = rd.getCreatedAt();
        return r;
    }

    public String getRdId()                         { return rdId; }
    public String getSourceAccount()                { return sourceAccount; }
    public BigDecimal getMonthlyInstallment()       { return monthlyInstallment; }
    public Integer getTenureMonths()                { return tenureMonths; }
    public BigDecimal getInterestRate()             { return interestRate; }
    public BigDecimal getTotalDeposited()           { return totalDeposited; }
    public BigDecimal getMaturityAmount()           { return maturityAmount; }
    public Integer getInstallmentsPaid()            { return installmentsPaid; }
    public Integer getInstallmentsMissed()          { return installmentsMissed; }
    public Integer getInstallmentsRemaining()       { return installmentsRemaining; }
    public LocalDate getNextDueDate()               { return nextDueDate; }
    public LocalDate getStartDate()                 { return startDate; }
    public LocalDate getMaturityDate()              { return maturityDate; }
    public RecurringDeposit.RdStatus getStatus()    { return status; }
    public String getNomineeName()                  { return nomineeName; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
}
