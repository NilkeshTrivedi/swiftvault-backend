package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FEATURE: Low Balance Alerts
 *
 * Users set a threshold per account (e.g. ₹2,000).
 * When any transaction drops the account balance below the threshold,
 * the alert is recorded and surfaced via GET /api/alerts/balance.
 *
 * The scheduled job also checks daily and flags any accounts
 * that were missed (e.g. interest fees deducting balance overnight).
 */
@Entity
@Table(name = "low_balance_alerts", indexes = {
        @Index(name = "idx_lba_user",    columnList = "user_id"),
        @Index(name = "idx_lba_account", columnList = "account_number")
})
public class LowBalanceAlert {

    @Id
    @Column(name = "alert_id", nullable = false, length = 30)
    private String alertId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /** User-configured threshold. Alert fires when balance drops below this. */
    @Column(name = "threshold_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal thresholdAmount;

    /** Balance at the time the alert was triggered. */
    @Column(name = "balance_at_alert", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAtAlert;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public LowBalanceAlert() {}

    public String        getAlertId()         { return alertId; }
    public User          getUser()            { return user; }
    public String        getAccountNumber()   { return accountNumber; }
    public BigDecimal    getThresholdAmount() { return thresholdAmount; }
    public BigDecimal    getBalanceAtAlert()  { return balanceAtAlert; }
    public boolean       isRead()             { return read; }
    public LocalDateTime getCreatedAt()       { return createdAt; }

    public void setAlertId(String v)             { this.alertId = v; }
    public void setUser(User v)                  { this.user = v; }
    public void setAccountNumber(String v)       { this.accountNumber = v; }
    public void setThresholdAmount(BigDecimal v) { this.thresholdAmount = v; }
    public void setBalanceAtAlert(BigDecimal v)  { this.balanceAtAlert = v; }
    public void setRead(boolean v)               { this.read = v; }
    public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final LowBalanceAlert a = new LowBalanceAlert();
        public Builder alertId(String v)            { a.alertId = v;          return this; }
        public Builder user(User v)                 { a.user = v;             return this; }
        public Builder accountNumber(String v)      { a.accountNumber = v;    return this; }
        public Builder thresholdAmount(BigDecimal v){ a.thresholdAmount = v;  return this; }
        public Builder balanceAtAlert(BigDecimal v) { a.balanceAtAlert = v;   return this; }
        public LowBalanceAlert build()              { return a; }
    }
}