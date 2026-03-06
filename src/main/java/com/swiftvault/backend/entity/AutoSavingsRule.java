package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FEATURE: Auto Savings Rules
 *
 * Two rule types:
 *   ROUND_UP   — on every withdrawal/transfer, round up to nearest ₹10/50/100
 *                and auto-deposit the difference into the linked savings goal.
 *                e.g. spend ₹237 → round up to ₹250 → ₹13 goes to goal.
 *
 *   RECURRING  — fixed amount auto-saved on 1st of every month.
 *                e.g. ₹2,000 on the 1st → swept from source account to goal.
 */
@Entity
@Table(name = "auto_savings_rules")
public class AutoSavingsRule {

    @Id
    @Column(name = "rule_id", nullable = false, length = 25)
    private String ruleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_account", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "goal_id", nullable = false)
    private SavingsGoal savingsGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private RuleType ruleType;

    /**
     * ROUND_UP  → round-up unit: 10, 50, or 100
     * RECURRING → fixed monthly amount to save
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "total_saved", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSaved = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum RuleType { ROUND_UP, RECURRING }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.totalSaved == null) this.totalSaved = BigDecimal.ZERO;
    }

    public AutoSavingsRule() {}

    public String        getRuleId()       { return ruleId; }
    public User          getUser()         { return user; }
    public Account       getSourceAccount(){ return sourceAccount; }
    public SavingsGoal   getSavingsGoal()  { return savingsGoal; }
    public RuleType      getRuleType()     { return ruleType; }
    public BigDecimal    getAmount()       { return amount; }
    public BigDecimal    getTotalSaved()   { return totalSaved; }
    public boolean       isActive()        { return active; }
    public LocalDateTime getCreatedAt()    { return createdAt; }

    public void setRuleId(String v)          { this.ruleId = v; }
    public void setUser(User v)              { this.user = v; }
    public void setSourceAccount(Account v)  { this.sourceAccount = v; }
    public void setSavingsGoal(SavingsGoal v){ this.savingsGoal = v; }
    public void setRuleType(RuleType v)      { this.ruleType = v; }
    public void setAmount(BigDecimal v)      { this.amount = v; }
    public void setTotalSaved(BigDecimal v)  { this.totalSaved = v; }
    public void setActive(boolean v)         { this.active = v; }
    public void setCreatedAt(LocalDateTime v){ this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final AutoSavingsRule r = new AutoSavingsRule();
        public Builder ruleId(String v)          { r.ruleId = v;       return this; }
        public Builder user(User v)              { r.user = v;         return this; }
        public Builder sourceAccount(Account v)  { r.sourceAccount = v;return this; }
        public Builder savingsGoal(SavingsGoal v){ r.savingsGoal = v;  return this; }
        public Builder ruleType(RuleType v)      { r.ruleType = v;     return this; }
        public Builder amount(BigDecimal v)      { r.amount = v;       return this; }
        public AutoSavingsRule build()           { r.totalSaved = BigDecimal.ZERO; r.active = true; return r; }
    }
}