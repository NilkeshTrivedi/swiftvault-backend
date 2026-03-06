package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A user's savings goal (e.g. "Vacation ✈️", "Emergency Fund 🏦").
 * Auto-savings rules sweep money into goals.
 * Created/managed via GET/POST /api/goals.
 */
@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    @Column(name = "goal_id", nullable = false, length = 25)
    private String goalId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "emoji", length = 10)
    private String emoji;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "saved_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal savedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum GoalStatus { ACTIVE, COMPLETED, CANCELLED }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = GoalStatus.ACTIVE;
        if (this.savedAmount == null) this.savedAmount = BigDecimal.ZERO;
    }

    public SavingsGoal() {}

    /** Returns true if savedAmount >= targetAmount */
    public boolean isCompleted() {
        return savedAmount.compareTo(targetAmount) >= 0;
    }

    public String        getGoalId()       { return goalId; }
    public User          getUser()         { return user; }
    public String        getName()         { return name; }
    public String        getEmoji()        { return emoji; }
    public BigDecimal    getTargetAmount() { return targetAmount; }
    public BigDecimal    getSavedAmount()  { return savedAmount; }
    public GoalStatus    getStatus()       { return status; }
    public LocalDateTime getCreatedAt()    { return createdAt; }

    public void setGoalId(String v)          { this.goalId = v; }
    public void setUser(User v)              { this.user = v; }
    public void setName(String v)            { this.name = v; }
    public void setEmoji(String v)           { this.emoji = v; }
    public void setTargetAmount(BigDecimal v){ this.targetAmount = v; }
    public void setSavedAmount(BigDecimal v) { this.savedAmount = v; }
    public void setStatus(GoalStatus v)      { this.status = v; }
    public void setCreatedAt(LocalDateTime v){ this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final SavingsGoal g = new SavingsGoal();
        public Builder goalId(String v)          { g.goalId = v;        return this; }
        public Builder user(User v)              { g.user = v;          return this; }
        public Builder name(String v)            { g.name = v;          return this; }
        public Builder emoji(String v)           { g.emoji = v;         return this; }
        public Builder targetAmount(BigDecimal v){ g.targetAmount = v;  return this; }
        public SavingsGoal build() {
            g.savedAmount = BigDecimal.ZERO;
            g.status = GoalStatus.ACTIVE;
            return g;
        }
    }
}