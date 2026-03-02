package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_deposits")
public class FixedDeposit {

    // Standard FD interest rates by tenure
    public static final BigDecimal RATE_7_TO_29_DAYS    = new BigDecimal("4.00");
    public static final BigDecimal RATE_30_TO_90_DAYS   = new BigDecimal("5.50");
    public static final BigDecimal RATE_91_TO_180_DAYS  = new BigDecimal("6.00");
    public static final BigDecimal RATE_181_TO_365_DAYS = new BigDecimal("6.75");
    public static final BigDecimal RATE_1_TO_2_YEARS    = new BigDecimal("7.00");
    public static final BigDecimal RATE_2_TO_3_YEARS    = new BigDecimal("7.25");
    public static final BigDecimal RATE_ABOVE_3_YEARS   = new BigDecimal("7.50");
    public static final BigDecimal PREMATURE_PENALTY    = new BigDecimal("1.00"); // 1% penalty

    @Id
    @Column(name = "fd_id", nullable = false, length = 25)
    private String fdId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_account", nullable = false)
    private Account sourceAccount;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_days", nullable = false)
    private Integer tenureDays;

    @Column(name = "maturity_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FdStatus status = FdStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_option", nullable = false, length = 20)
    private PayoutOption payoutOption = PayoutOption.ON_MATURITY;

    @Column(name = "nominee_name", length = 100)
    private String nomineeName;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "premature_withdrawal", nullable = false)
    private Boolean prematureWithdrawal = false;

    @Column(name = "actual_interest_earned", precision = 15, scale = 2)
    private BigDecimal actualInterestEarned;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum FdStatus    { ACTIVE, MATURED, CLOSED, PREMATURE_CLOSED }
    public enum PayoutOption { ON_MATURITY, MONTHLY_INTEREST, QUARTERLY_INTEREST }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = FdStatus.ACTIVE;
    }

    // Static helper — get interest rate based on tenure
    public static BigDecimal getInterestRate(int days) {
        if (days < 30)   return RATE_7_TO_29_DAYS;
        if (days < 91)   return RATE_30_TO_90_DAYS;
        if (days < 181)  return RATE_91_TO_180_DAYS;
        if (days < 366)  return RATE_181_TO_365_DAYS;
        if (days < 731)  return RATE_1_TO_2_YEARS;
        if (days < 1096) return RATE_2_TO_3_YEARS;
        return RATE_ABOVE_3_YEARS;
    }

    // Calculate maturity amount using compound interest (quarterly)
    public static BigDecimal calculateMaturityAmount(BigDecimal principal, BigDecimal annualRate, int days) {
        double p = principal.doubleValue();
        double r = annualRate.doubleValue() / 100.0;
        double t = days / 365.0;
        double n = 4.0; // quarterly compounding
        double maturity = p * Math.pow(1 + r / n, n * t);
        return new BigDecimal(maturity).setScale(2, RoundingMode.HALF_UP);
    }

    // Getters
    public String       getFdId()                { return fdId; }
    public User         getUser()                { return user; }
    public Account      getSourceAccount()       { return sourceAccount; }
    public BigDecimal   getPrincipalAmount()     { return principalAmount; }
    public BigDecimal   getInterestRate()        { return interestRate; }
    public Integer      getTenureDays()          { return tenureDays; }
    public BigDecimal   getMaturityAmount()      { return maturityAmount; }
    public LocalDate    getStartDate()           { return startDate; }
    public LocalDate    getMaturityDate()        { return maturityDate; }
    public FdStatus     getStatus()              { return status; }
    public PayoutOption getPayoutOption()        { return payoutOption; }
    public String       getNomineeName()         { return nomineeName; }
    public LocalDateTime getClosedAt()           { return closedAt; }
    public Boolean      getPrematureWithdrawal() { return prematureWithdrawal; }
    public BigDecimal   getActualInterestEarned(){ return actualInterestEarned; }
    public LocalDateTime getCreatedAt()          { return createdAt; }

    // Setters
    public void setFdId(String v)                      { this.fdId = v; }
    public void setUser(User v)                        { this.user = v; }
    public void setSourceAccount(Account v)            { this.sourceAccount = v; }
    public void setPrincipalAmount(BigDecimal v)       { this.principalAmount = v; }
    public void setInterestRate(BigDecimal v)          { this.interestRate = v; }
    public void setTenureDays(Integer v)               { this.tenureDays = v; }
    public void setMaturityAmount(BigDecimal v)        { this.maturityAmount = v; }
    public void setStartDate(LocalDate v)              { this.startDate = v; }
    public void setMaturityDate(LocalDate v)           { this.maturityDate = v; }
    public void setStatus(FdStatus v)                  { this.status = v; }
    public void setPayoutOption(PayoutOption v)        { this.payoutOption = v; }
    public void setNomineeName(String v)               { this.nomineeName = v; }
    public void setClosedAt(LocalDateTime v)           { this.closedAt = v; }
    public void setPrematureWithdrawal(Boolean v)      { this.prematureWithdrawal = v; }
    public void setActualInterestEarned(BigDecimal v)  { this.actualInterestEarned = v; }
    public void setCreatedAt(LocalDateTime v)          { this.createdAt = v; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String fdId; private User user; private Account sourceAccount;
        private BigDecimal principalAmount; private BigDecimal interestRate;
        private Integer tenureDays; private BigDecimal maturityAmount;
        private LocalDate startDate; private LocalDate maturityDate;
        private FdStatus status = FdStatus.ACTIVE;
        private PayoutOption payoutOption = PayoutOption.ON_MATURITY;
        private String nomineeName;

        public Builder fdId(String v)              { this.fdId = v; return this; }
        public Builder user(User v)                { this.user = v; return this; }
        public Builder sourceAccount(Account v)    { this.sourceAccount = v; return this; }
        public Builder principalAmount(BigDecimal v){ this.principalAmount = v; return this; }
        public Builder interestRate(BigDecimal v)  { this.interestRate = v; return this; }
        public Builder tenureDays(Integer v)       { this.tenureDays = v; return this; }
        public Builder maturityAmount(BigDecimal v){ this.maturityAmount = v; return this; }
        public Builder startDate(LocalDate v)      { this.startDate = v; return this; }
        public Builder maturityDate(LocalDate v)   { this.maturityDate = v; return this; }
        public Builder status(FdStatus v)          { this.status = v; return this; }
        public Builder payoutOption(PayoutOption v){ this.payoutOption = v; return this; }
        public Builder nomineeName(String v)       { this.nomineeName = v; return this; }

        public FixedDeposit build() {
            FixedDeposit fd = new FixedDeposit();
            fd.fdId = this.fdId; fd.user = this.user;
            fd.sourceAccount = this.sourceAccount;
            fd.principalAmount = this.principalAmount;
            fd.interestRate = this.interestRate;
            fd.tenureDays = this.tenureDays;
            fd.maturityAmount = this.maturityAmount;
            fd.startDate = this.startDate;
            fd.maturityDate = this.maturityDate;
            fd.status = this.status; fd.payoutOption = this.payoutOption;
            fd.nomineeName = this.nomineeName;
            fd.prematureWithdrawal = false;
            return fd;
        }
    }
}