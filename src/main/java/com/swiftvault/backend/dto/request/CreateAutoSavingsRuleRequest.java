package com.swiftvault.backend.dto.request;

import com.swiftvault.backend.entity.AutoSavingsRule;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateAutoSavingsRuleRequest {

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotBlank(message = "Goal ID is required")
    private String goalId;

    @NotNull(message = "Rule type is required")
    private AutoSavingsRule.RuleType ruleType;

    /**
     * ROUND_UP  → must be 10, 50, or 100 (the round-up unit in ₹)
     * RECURRING → any amount >= ₹100 (monthly auto-save amount)
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10.00", message = "Minimum amount is ₹10")
    private BigDecimal amount;

    public String                   getSourceAccountNumber() { return sourceAccountNumber; }
    public String                   getGoalId()              { return goalId; }
    public AutoSavingsRule.RuleType getRuleType()            { return ruleType; }
    public BigDecimal               getAmount()              { return amount; }

    public void setSourceAccountNumber(String v)         { this.sourceAccountNumber = v; }
    public void setGoalId(String v)                      { this.goalId = v; }
    public void setRuleType(AutoSavingsRule.RuleType v)  { this.ruleType = v; }
    public void setAmount(BigDecimal v)                  { this.amount = v; }
}