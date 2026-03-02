package com.swiftvault.backend.dto.response;

import com.swiftvault.backend.entity.VirtualCard;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CardResponse {
    private String cardId;
    private String maskedCardNumber;
    private String cardHolderName;
    private String expiryFormatted;
    private VirtualCard.CardType cardType;
    private VirtualCard.CardNetwork cardNetwork;
    private VirtualCard.CardStatus status;
    private String linkedAccount;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal perTxnLimit;
    private BigDecimal todaySpent;
    private BigDecimal monthSpent;
    private Boolean onlineTransactions;
    private Boolean internationalTransactions;
    private Boolean contactlessPayments;
    private String nickname;
    private LocalDateTime issuedAt;

    public static CardResponse from(VirtualCard c) {
        CardResponse r = new CardResponse();
        r.cardId = c.getCardId();
        r.maskedCardNumber = c.getMaskedCardNumber();
        r.cardHolderName = c.getCardHolderName();
        r.expiryFormatted = c.getExpiryFormatted();
        r.cardType = c.getCardType();
        r.cardNetwork = c.getCardNetwork();
        r.status = c.getStatus();
        r.linkedAccount = c.getLinkedAccount().getAccountNumber();
        r.dailyLimit = c.getDailyLimit();
        r.monthlyLimit = c.getMonthlyLimit();
        r.perTxnLimit = c.getPerTxnLimit();
        r.todaySpent = c.getTodaySpent();
        r.monthSpent = c.getMonthSpent();
        r.onlineTransactions = c.getOnlineTransactions();
        r.internationalTransactions = c.getInternationalTransactions();
        r.contactlessPayments = c.getContactlessPayments();
        r.nickname = c.getNickname();
        r.issuedAt = c.getIssuedAt();
        return r;
    }

    public String getCardId()                        { return cardId; }
    public String getMaskedCardNumber()              { return maskedCardNumber; }
    public String getCardHolderName()               { return cardHolderName; }
    public String getExpiryFormatted()              { return expiryFormatted; }
    public VirtualCard.CardType getCardType()       { return cardType; }
    public VirtualCard.CardNetwork getCardNetwork() { return cardNetwork; }
    public VirtualCard.CardStatus getStatus()       { return status; }
    public String getLinkedAccount()                { return linkedAccount; }
    public BigDecimal getDailyLimit()               { return dailyLimit; }
    public BigDecimal getMonthlyLimit()             { return monthlyLimit; }
    public BigDecimal getPerTxnLimit()              { return perTxnLimit; }
    public BigDecimal getTodaySpent()               { return todaySpent; }
    public BigDecimal getMonthSpent()               { return monthSpent; }
    public Boolean getOnlineTransactions()          { return onlineTransactions; }
    public Boolean getInternationalTransactions()   { return internationalTransactions; }
    public Boolean getContactlessPayments()         { return contactlessPayments; }
    public String getNickname()                     { return nickname; }
    public LocalDateTime getIssuedAt()              { return issuedAt; }
}
