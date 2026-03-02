package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_deposits")
public class RecurringDeposit {

    public static final BigDecimal INTEREST_RATE       = new BigDecimal("6.50");
    public static final BigDecimal MISSED_EMI_PENALTY  = new BigDecimal("50.00"); // ₹50 per missed
    public static final BigDecimal MIN_INSTALLMENT     = new BigDecimal("500.00");

    @Id
    @Column(name = "rd_id", nullable = false, length = 25)
    private String rdId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_account", nullable = false)
    private Account sourceAccount;

    @Column(name = "monthly_installment", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyInstallment;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "total_deposited", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeposited = BigDecimal.ZERO;

    @Column(name = "maturity_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Column(name = "installments_paid", nullable = false)
    private Integer installmentsPaid = 0;

    @Column(name = "installments_missed", nullable = false)
    private Integer installmentsMissed = 0;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RdStatus status = RdStatus.ACTIVE;

    @Column(name = "nominee_name", length = 100)
    private String nomineeName;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum RdStatus { ACTIVE, MATURED, CLOSED, DEFAULTED }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = RdStatus.ACTIVE;
        if (this.totalDeposited == null) this.totalDeposited = BigDecimal.ZERO;
        if (this.installmentsPaid == null) this.installmentsPaid = 0;
        if (this.installmentsMissed == null) this.installmentsMissed = 0;
    }

    // Calculate maturity amount for RD
    public static BigDecimal calculateMaturityAmount(BigDecimal monthlyInstallment, int months) {
        double p = monthlyInstallment.doubleValue();
        double r = INTEREST_RATE.doubleValue() / 100.0 / 12.0;
        double n = months;
        double maturity = p * (Math.pow(1 + r, n) - 1) / r * (1 + r);
        return new BigDecimal(maturity).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRemainingInstallments() {
        return new BigDecimal(tenureMonths - installmentsPaid - installmentsMissed)
                .multiply(monthlyInstallment).setScale(2, RoundingMode.HALF_UP);
    }

    // Getters
    public String      getRdId()               { return rdId; }
    public User        getUser()               { return user; }
    public Account     getSourceAccount()      { return sourceAccount; }
    public BigDecimal  getMonthlyInstallment() { return monthlyInstallment; }
    public Integer     getTenureMonths()       { return tenureMonths; }
    public BigDecimal  getInterestRate()       { return interestRate; }
    public BigDecimal  getTotalDeposited()     { return totalDeposited; }
    public BigDecimal  getMaturityAmount()     { return maturityAmount; }
    public Integer     getInstallmentsPaid()   { return installmentsPaid; }
    public Integer     getInstallmentsMissed() { return installmentsMissed; }
    public LocalDate   getNextDueDate()        { return nextDueDate; }
    public LocalDate   getStartDate()          { return startDate; }
    public LocalDate   getMaturityDate()       { return maturityDate; }
    public RdStatus    getStatus()             { return status; }
    public String      getNomineeName()        { return nomineeName; }
    public LocalDateTime getClosedAt()         { return closedAt; }
    public LocalDateTime getCreatedAt()        { return createdAt; }

    // Setters
    public void setRdId(String v)                      { this.rdId = v; }
    public void setUser(User v)                        { this.user = v; }
    public void setSourceAccount(Account v)            { this.sourceAccount = v; }
    public void setMonthlyInstallment(BigDecimal v)    { this.monthlyInstallment = v; }
    public void setTenureMonths(Integer v)             { this.tenureMonths = v; }
    public void setInterestRate(BigDecimal v)          { this.interestRate = v; }
    public void setTotalDeposited(BigDecimal v)        { this.totalDeposited = v; }
    public void setMaturityAmount(BigDecimal v)        { this.maturityAmount = v; }
    public void setInstallmentsPaid(Integer v)         { this.installmentsPaid = v; }
    public void setInstallmentsMissed(Integer v)       { this.installmentsMissed = v; }
    public void setNextDueDate(LocalDate v)            { this.nextDueDate = v; }
    public void setStartDate(LocalDate v)              { this.startDate = v; }
    public void setMaturityDate(LocalDate v)           { this.maturityDate = v; }
    public void setStatus(RdStatus v)                  { this.status = v; }
    public void setNomineeName(String v)               { this.nomineeName = v; }
    public void setClosedAt(LocalDateTime v)           { this.closedAt = v; }
    public void setCreatedAt(LocalDateTime v)          { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String rdId; private User user; private Account sourceAccount;
        private BigDecimal monthlyInstallment; private Integer tenureMonths;
        private BigDecimal interestRate; private BigDecimal maturityAmount;
        private LocalDate startDate; private LocalDate maturityDate;
        private LocalDate nextDueDate; private String nomineeName;

        public Builder rdId(String v)                  { this.rdId = v; return this; }
        public Builder user(User v)                    { this.user = v; return this; }
        public Builder sourceAccount(Account v)        { this.sourceAccount = v; return this; }
        public Builder monthlyInstallment(BigDecimal v){ this.monthlyInstallment = v; return this; }
        public Builder tenureMonths(Integer v)         { this.tenureMonths = v; return this; }
        public Builder interestRate(BigDecimal v)      { this.interestRate = v; return this; }
        public Builder maturityAmount(BigDecimal v)    { this.maturityAmount = v; return this; }
        public Builder startDate(LocalDate v)          { this.startDate = v; return this; }
        public Builder maturityDate(LocalDate v)       { this.maturityDate = v; return this; }
        public Builder nextDueDate(LocalDate v)        { this.nextDueDate = v; return this; }
        public Builder nomineeName(String v)           { this.nomineeName = v; return this; }

        public RecurringDeposit build() {
            RecurringDeposit rd = new RecurringDeposit();
            rd.rdId = this.rdId; rd.user = this.user;
            rd.sourceAccount = this.sourceAccount;
            rd.monthlyInstallment = this.monthlyInstallment;
            rd.tenureMonths = this.tenureMonths;
            rd.interestRate = this.interestRate != null ? this.interestRate : INTEREST_RATE;
            rd.maturityAmount = this.maturityAmount;
            rd.startDate = this.startDate; rd.maturityDate = this.maturityDate;
            rd.nextDueDate = this.nextDueDate; rd.nomineeName = this.nomineeName;
            rd.totalDeposited = BigDecimal.ZERO;
            rd.installmentsPaid = 0; rd.installmentsMissed = 0;
            rd.status = RdStatus.ACTIVE;
            return rd;
        }
    }
}