package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FEATURE: Referral System
 *
 * Flow:
 *   1. User gets a unique referral code (auto-generated on registration).
 *   2. New user enters referral code during registration.
 *   3. When the referred user opens their first account, both users get ₹200 reward.
 *   4. Reward is credited to the referrer's oldest active account.
 *   5. Each referral is tracked here with status.
 */
@Entity
@Table(name = "referrals", indexes = {
        @Index(name = "idx_referral_referrer", columnList = "referrer_id"),
        @Index(name = "idx_referral_code",     columnList = "referral_code")
})
public class Referral {

    @Id
    @Column(name = "referral_id", nullable = false, length = 25)
    private String referralId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "referred_id")
    private User referred;              // null until the new user actually registers

    @Column(name = "referral_code", nullable = false, length = 12, unique = true)
    private String referralCode;

    @Column(name = "reward_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal rewardAmount = new BigDecimal("200.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "reward_credited_at")
    private LocalDateTime rewardCreditedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ReferralStatus {
        PENDING,    // code shared, referred user hasn't registered yet
        REGISTERED, // referred user registered, waiting for first account
        COMPLETED,  // reward paid out to both
        EXPIRED     // no action taken within 30 days
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null)       this.status = ReferralStatus.PENDING;
        if (this.rewardAmount == null) this.rewardAmount = new BigDecimal("200.00");
    }

    public Referral() {}

    public String         getReferralId()        { return referralId; }
    public User           getReferrer()          { return referrer; }
    public User           getReferred()          { return referred; }
    public String         getReferralCode()      { return referralCode; }
    public BigDecimal     getRewardAmount()      { return rewardAmount; }
    public ReferralStatus getStatus()            { return status; }
    public LocalDateTime  getRewardCreditedAt()  { return rewardCreditedAt; }
    public LocalDateTime  getCreatedAt()         { return createdAt; }

    public void setReferralId(String v)              { this.referralId = v; }
    public void setReferrer(User v)                  { this.referrer = v; }
    public void setReferred(User v)                  { this.referred = v; }
    public void setReferralCode(String v)            { this.referralCode = v; }
    public void setRewardAmount(BigDecimal v)        { this.rewardAmount = v; }
    public void setStatus(ReferralStatus v)          { this.status = v; }
    public void setRewardCreditedAt(LocalDateTime v) { this.rewardCreditedAt = v; }
    public void setCreatedAt(LocalDateTime v)        { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Referral r = new Referral();
        public Builder referralId(String v)   { r.referralId = v;   return this; }
        public Builder referrer(User v)       { r.referrer = v;     return this; }
        public Builder referralCode(String v) { r.referralCode = v; return this; }
        public Referral build()               { return r; }
    }
}