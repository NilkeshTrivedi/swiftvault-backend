package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores each account's user-configured low-balance threshold.
 * One row per account. Updated via PUT /api/alerts/threshold.
 */
@Entity
@Table(name = "alert_thresholds")
public class AlertThreshold {

    @Id
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "threshold_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public AlertThreshold() {}

    public String        getAccountNumber()   { return accountNumber; }
    public User          getUser()            { return user; }
    public BigDecimal    getThresholdAmount() { return thresholdAmount; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }

    public void setAccountNumber(String v)       { this.accountNumber = v; }
    public void setUser(User v)                  { this.user = v; }
    public void setThresholdAmount(BigDecimal v) { this.thresholdAmount = v; }
    public void setUpdatedAt(LocalDateTime v)    { this.updatedAt = v; }
}