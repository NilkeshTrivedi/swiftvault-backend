package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FEATURE: Suspicious Activity Alerts
 * Auto-flagged on: large transfers, rapid transfers, off-hours activity,
 * new IP login, unusual daily spend, repeated wrong PIN, self-freeze.
 */
@Entity
@Table(name = "suspicious_activities", indexes = {
        @Index(name = "idx_sus_user",   columnList = "user_id"),
        @Index(name = "idx_sus_status", columnList = "status"),
        @Index(name = "idx_sus_time",   columnList = "created_at")
})
public class SuspiciousActivity {

    @Id
    @Column(name = "alert_id", nullable = false, length = 30)
    private String alertId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 40)
    private AlertType alertType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** 1–10, 10 = highest risk. Displayed in admin dashboard. */
    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "resolved_by", length = 20)
    private String resolvedBy;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AlertType {
        LARGE_TRANSACTION,      // single transfer > ₹25,000
        RAPID_TRANSFERS,        // 5+ transfers within 10 minutes
        OFF_HOURS_LARGE_TXN,    // large transfer midnight–5 AM
        NEW_IP_LOGIN,           // login from brand-new IP
        UNUSUAL_DAILY_SPEND,    // daily spend > 3× user's average
        MULTIPLE_FAILED_PINS,   // repeated wrong transaction PIN
        ACCOUNT_SELF_FROZEN     // user manually froze own account
    }

    public enum AlertStatus { OPEN, REVIEWED, RESOLVED, FALSE_POSITIVE }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = AlertStatus.OPEN;
    }

    public SuspiciousActivity() {}

    public String        getAlertId()         { return alertId; }
    public User          getUser()            { return user; }
    public AlertType     getAlertType()       { return alertType; }
    public String        getDescription()     { return description; }
    public BigDecimal    getAmount()          { return amount; }
    public String        getAccountNumber()   { return accountNumber; }
    public String        getIpAddress()       { return ipAddress; }
    public int           getRiskScore()       { return riskScore; }
    public AlertStatus   getStatus()          { return status; }
    public String        getResolvedBy()      { return resolvedBy; }
    public String        getResolutionNotes() { return resolutionNotes; }
    public LocalDateTime getResolvedAt()      { return resolvedAt; }
    public LocalDateTime getCreatedAt()       { return createdAt; }

    public void setAlertId(String v)            { this.alertId = v; }
    public void setUser(User v)                 { this.user = v; }
    public void setAlertType(AlertType v)       { this.alertType = v; }
    public void setDescription(String v)        { this.description = v; }
    public void setAmount(BigDecimal v)         { this.amount = v; }
    public void setAccountNumber(String v)      { this.accountNumber = v; }
    public void setIpAddress(String v)          { this.ipAddress = v; }
    public void setRiskScore(int v)             { this.riskScore = v; }
    public void setStatus(AlertStatus v)        { this.status = v; }
    public void setResolvedBy(String v)         { this.resolvedBy = v; }
    public void setResolutionNotes(String v)    { this.resolutionNotes = v; }
    public void setResolvedAt(LocalDateTime v)  { this.resolvedAt = v; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final SuspiciousActivity a = new SuspiciousActivity();
        public Builder alertId(String v)       { a.alertId = v;       return this; }
        public Builder user(User v)            { a.user = v;          return this; }
        public Builder alertType(AlertType v)  { a.alertType = v;     return this; }
        public Builder description(String v)   { a.description = v;   return this; }
        public Builder amount(BigDecimal v)    { a.amount = v;        return this; }
        public Builder accountNumber(String v) { a.accountNumber = v; return this; }
        public Builder ipAddress(String v)     { a.ipAddress = v;     return this; }
        public Builder riskScore(int v)        { a.riskScore = v;     return this; }
        public SuspiciousActivity build()      { return a; }
    }
}