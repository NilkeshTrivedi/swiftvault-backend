package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanResponse {
    private String loanId;
    private Loan.LoanType loanType;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal totalPayable;
    private BigDecimal totalInterest;
    private BigDecimal outstandingBalance;
    private Integer emisPaid;
    private Integer emisMissed;
    private Integer emisRemaining;
    private LocalDate nextEmiDate;
    private LocalDate disbursalDate;
    private String purpose;
    private Loan.LoanStatus status;
    private String rejectionReason;
    private String disbursalAccount;
    private LocalDateTime createdAt;

    public static LoanResponse from(Loan l) {
        LoanResponse r = new LoanResponse();
        r.loanId = l.getLoanId();
        r.loanType = l.getLoanType();
        r.loanAmount = l.getLoanAmount();
        r.interestRate = l.getInterestRate();
        r.tenureMonths = l.getTenureMonths();
        r.emiAmount = l.getEmiAmount();
        r.totalPayable = l.getTotalPayable();
        r.totalInterest = l.getTotalInterest();
        r.outstandingBalance = l.getOutstandingBalance();
        r.emisPaid = l.getEmisPaid();
        r.emisMissed = l.getEmisMissed();
        r.emisRemaining = l.getTenureMonths() - l.getEmisPaid() - l.getEmisMissed();
        r.nextEmiDate = l.getNextEmiDate();
        r.disbursalDate = l.getDisbursalDate();
        r.purpose = l.getPurpose();
        r.status = l.getStatus();
        r.rejectionReason = l.getRejectionReason();
        r.disbursalAccount = l.getDisbursalAccount().getAccountNumber();
        r.createdAt = l.getCreatedAt();
        return r;
    }

    public String getLoanId()                   { return loanId; }
    public Loan.LoanType getLoanType()          { return loanType; }
    public BigDecimal getLoanAmount()           { return loanAmount; }
    public BigDecimal getInterestRate()         { return interestRate; }
    public Integer getTenureMonths()            { return tenureMonths; }
    public BigDecimal getEmiAmount()            { return emiAmount; }
    public BigDecimal getTotalPayable()         { return totalPayable; }
    public BigDecimal getTotalInterest()        { return totalInterest; }
    public BigDecimal getOutstandingBalance()   { return outstandingBalance; }
    public Integer getEmisPaid()                { return emisPaid; }
    public Integer getEmisMissed()              { return emisMissed; }
    public Integer getEmisRemaining()           { return emisRemaining; }
    public LocalDate getNextEmiDate()           { return nextEmiDate; }
    public LocalDate getDisbursalDate()         { return disbursalDate; }
    public String getPurpose()                  { return purpose; }
    public Loan.LoanStatus getStatus()          { return status; }
    public String getRejectionReason()          { return rejectionReason; }
    public String getDisbursalAccount()         { return disbursalAccount; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
}
