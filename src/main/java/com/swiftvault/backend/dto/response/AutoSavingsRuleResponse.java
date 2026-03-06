package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.AutoSavingsRule;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoSavingsRuleResponse {

    private String                   ruleId;
    private String                   sourceAccountNumber;
    private String                   goalId;
    private String                   goalName;
    private String                   goalEmoji;
    private AutoSavingsRule.RuleType ruleType;
    private BigDecimal               amount;
    private BigDecimal               totalSaved;
    private boolean                  active;
    private String                   description;
    private LocalDateTime            createdAt;

    public static AutoSavingsRuleResponse from(AutoSavingsRule rule) {
        AutoSavingsRuleResponse r = new AutoSavingsRuleResponse();
        r.ruleId             = rule.getRuleId();
        r.sourceAccountNumber= rule.getSourceAccount().getAccountNumber();
        r.goalId             = rule.getSavingsGoal().getGoalId();
        r.goalName           = rule.getSavingsGoal().getName();
        r.goalEmoji          = rule.getSavingsGoal().getEmoji();
        r.ruleType           = rule.getRuleType();
        r.amount             = rule.getAmount();
        r.totalSaved         = rule.getTotalSaved();
        r.active             = rule.isActive();
        r.createdAt          = rule.getCreatedAt();

        if (rule.getRuleType() == AutoSavingsRule.RuleType.ROUND_UP) {
            r.description = "Round up every spend to nearest ₹" + rule.getAmount().toPlainString()
                    + " and save the difference into " + rule.getSavingsGoal().getEmoji()
                    + " " + rule.getSavingsGoal().getName();
        } else {
            r.description = "Auto-save ₹" + rule.getAmount().toPlainString()
                    + " every month into " + rule.getSavingsGoal().getEmoji()
                    + " " + rule.getSavingsGoal().getName();
        }
        return r;
    }

    // Getters
    public String                   getRuleId()              { return ruleId; }
    public String                   getSourceAccountNumber() { return sourceAccountNumber; }
    public String                   getGoalId()              { return goalId; }
    public String                   getGoalName()            { return goalName; }
    public String                   getGoalEmoji()           { return goalEmoji; }
    public AutoSavingsRule.RuleType getRuleType()            { return ruleType; }
    public BigDecimal               getAmount()              { return amount; }
    public BigDecimal               getTotalSaved()          { return totalSaved; }
    public boolean                  isActive()               { return active; }
    public String                   getDescription()         { return description; }
    public LocalDateTime            getCreatedAt()           { return createdAt; }
}