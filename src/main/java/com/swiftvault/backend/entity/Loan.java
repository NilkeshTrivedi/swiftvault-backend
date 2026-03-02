package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    public static final BigDecimal PERSONAL_LOAN_RATE = new BigDecimal("12.00"); // 12% p.a.
    public static final BigDecimal HOME_LOAN_RATE     = new BigDecimal("8.50");  // 8.5% p.a.
    public static final BigDecimal CAR_LOAN_RATE      = new BigDecimal("9.50");  // 9.5% p.a.
    public static final BigDecimal EDUCATION_LOAN_RATE= new BigDecimal("10.00"); // 10% p.a.
    public static final BigDecimal MIN_LOAN_AMOUNT    = new BigDecimal("10000.00");
    public static final BigDecimal MAX_PERSONAL_LOAN  = new BigDecimal("500000.00");
    public static final BigDecimal MAX_HOME_LOAN      = new BigDecimal("10000000.00");

    @Id
    @Column(name = "loan_id", nullable = false, length = 25)
    private String loanId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "disbursal_account", nullable = false)
    private Account disbursalAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emi_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "total_payable", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "total_interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "outstanding_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "emis_paid", nullable = false)
    private Integer emisPaid = 0;

    @Column(name = "emis_missed", nullable = false)
    private Integer emisMissed = 0;

    @Column(name = "next_emi_date")
    private LocalDate nextEmiDate;

    @Column(name = "disbursal_date")
    private LocalDate disbursalDate;

    @Column(name = "purpose", length = 255)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum LoanType   { PERSONAL, HOME, CAR, EDUCATION }
    public enum LoanStatus { PENDING, APPROVED, ACTIVE, CLOSED, REJECTED, DEFAULTED }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = LoanStatus.PENDING;
        if (this.emisPaid == null) this.emisPaid = 0;
        if (this.emisMissed == null) this.emisMissed = 0;
    }

    // Calculate EMI using standard formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        double p = principal.doubleValue();
        double r = annualRate.doubleValue() / 100.0 / 12.0;
        double n = months;
        double emi = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        return new BigDecimal(emi).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal getInterestRate(LoanType type) {
        return switch (type) {
            case HOME      -> HOME_LOAN_RATE;
            case CAR       -> CAR_LOAN_RATE;
            case EDUCATION -> EDUCATION_LOAN_RATE;
            default        -> PERSONAL_LOAN_RATE;
        };
    }

    // Getters
    public String      getLoanId()            { return loanId; }
    public User        getUser()              { return user; }
    public Account     getDisbursalAccount()  { return disbursalAccount; }
    public LoanType    getLoanType()          { return loanType; }
    public BigDecimal  getLoanAmount()        { return loanAmount; }
    public BigDecimal  getInterestRate()      { return interestRate; }
    public Integer     getTenureMonths()      { return tenureMonths; }
    public BigDecimal  getEmiAmount()         { return emiAmount; }
    public BigDecimal  getTotalPayable()      { return totalPayable; }
    public BigDecimal  getTotalInterest()     { return totalInterest; }
    public BigDecimal  getOutstandingBalance(){ return outstandingBalance; }
    public Integer     getEmisPaid()          { return emisPaid; }
    public Integer     getEmisMissed()        { return emisMissed; }
    public LocalDate   getNextEmiDate()       { return nextEmiDate; }
    public LocalDate   getDisbursalDate()     { return disbursalDate; }
    public String      getPurpose()           { return purpose; }
    public LoanStatus  getStatus()            { return status; }
    public String      getRejectionReason()   { return rejectionReason; }
    public String      getAdminNotes()        { return adminNotes; }
    public LocalDateTime getClosedAt()        { return closedAt; }
    public LocalDateTime getCreatedAt()       { return createdAt; }

    // Setters
    public void setLoanId(String v)              { this.loanId = v; }
    public void setUser(User v)                  { this.user = v; }
    public void setDisbursalAccount(Account v)   { this.disbursalAccount = v; }
    public void setLoanType(LoanType v)          { this.loanType = v; }
    public void setLoanAmount(BigDecimal v)      { this.loanAmount = v; }
    public void setInterestRate(BigDecimal v)    { this.interestRate = v; }
    public void setTenureMonths(Integer v)       { this.tenureMonths = v; }
    public void setEmiAmount(BigDecimal v)       { this.emiAmount = v; }
    public void setTotalPayable(BigDecimal v)    { this.totalPayable = v; }
    public void setTotalInterest(BigDecimal v)   { this.totalInterest = v; }
    public void setOutstandingBalance(BigDecimal v){ this.outstandingBalance = v; }
    public void setEmisPaid(Integer v)           { this.emisPaid = v; }
    public void setEmisMissed(Integer v)         { this.emisMissed = v; }
    public void setNextEmiDate(LocalDate v)      { this.nextEmiDate = v; }
    public void setDisbursalDate(LocalDate v)    { this.disbursalDate = v; }
    public void setPurpose(String v)             { this.purpose = v; }
    public void setStatus(LoanStatus v)          { this.status = v; }
    public void setRejectionReason(String v)     { this.rejectionReason = v; }
    public void setAdminNotes(String v)          { this.adminNotes = v; }
    public void setClosedAt(LocalDateTime v)     { this.closedAt = v; }
    public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String loanId; private User user; private Account disbursalAccount;
        private LoanType loanType; private BigDecimal loanAmount;
        private BigDecimal interestRate; private Integer tenureMonths;
        private BigDecimal emiAmount; private BigDecimal totalPayable;
        private BigDecimal totalInterest; private BigDecimal outstandingBalance;
        private LocalDate nextEmiDate; private String purpose;
        private LoanStatus status = LoanStatus.PENDING;

        public Builder loanId(String v)               { this.loanId = v; return this; }
        public Builder user(User v)                   { this.user = v; return this; }
        public Builder disbursalAccount(Account v)    { this.disbursalAccount = v; return this; }
        public Builder loanType(LoanType v)           { this.loanType = v; return this; }
        public Builder loanAmount(BigDecimal v)       { this.loanAmount = v; return this; }
        public Builder interestRate(BigDecimal v)     { this.interestRate = v; return this; }
        public Builder tenureMonths(Integer v)        { this.tenureMonths = v; return this; }
        public Builder emiAmount(BigDecimal v)        { this.emiAmount = v; return this; }
        public Builder totalPayable(BigDecimal v)     { this.totalPayable = v; return this; }
        public Builder totalInterest(BigDecimal v)    { this.totalInterest = v; return this; }
        public Builder outstandingBalance(BigDecimal v){ this.outstandingBalance = v; return this; }
        public Builder nextEmiDate(LocalDate v)       { this.nextEmiDate = v; return this; }
        public Builder purpose(String v)              { this.purpose = v; return this; }
        public Builder status(LoanStatus v)           { this.status = v; return this; }

        public Loan build() {
            Loan l = new Loan();
            l.loanId = this.loanId; l.user = this.user;
            l.disbursalAccount = this.disbursalAccount;
            l.loanType = this.loanType; l.loanAmount = this.loanAmount;
            l.interestRate = this.interestRate; l.tenureMonths = this.tenureMonths;
            l.emiAmount = this.emiAmount; l.totalPayable = this.totalPayable;
            l.totalInterest = this.totalInterest; l.outstandingBalance = this.outstandingBalance;
            l.nextEmiDate = this.nextEmiDate; l.purpose = this.purpose;
            l.status = this.status; l.emisPaid = 0; l.emisMissed = 0;
            return l;
        }
    }
}