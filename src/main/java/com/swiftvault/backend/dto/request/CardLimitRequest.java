package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CardLimitRequest {
    @NotBlank private String cardId;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal perTxnLimit;
    private Boolean onlineTransactions;
    private Boolean internationalTransactions;
    private Boolean contactlessPayments;

    public String getCardId()                        { return cardId; }
    public BigDecimal getDailyLimit()                { return dailyLimit; }
    public BigDecimal getMonthlyLimit()              { return monthlyLimit; }
    public BigDecimal getPerTxnLimit()               { return perTxnLimit; }
    public Boolean getOnlineTransactions()           { return onlineTransactions; }
    public Boolean getInternationalTransactions()    { return internationalTransactions; }
    public Boolean getContactlessPayments()          { return contactlessPayments; }
    public void setCardId(String v)                  { this.cardId = v; }
    public void setDailyLimit(BigDecimal v)          { this.dailyLimit = v; }
    public void setMonthlyLimit(BigDecimal v)        { this.monthlyLimit = v; }
    public void setPerTxnLimit(BigDecimal v)         { this.perTxnLimit = v; }
    public void setOnlineTransactions(Boolean v)     { this.onlineTransactions = v; }
    public void setInternationalTransactions(Boolean v){ this.internationalTransactions = v; }
    public void setContactlessPayments(Boolean v)    { this.contactlessPayments = v; }
}
