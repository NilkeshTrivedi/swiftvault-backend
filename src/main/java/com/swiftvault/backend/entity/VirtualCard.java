package com.swiftvault.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "virtual_cards")
public class VirtualCard {

    public static final BigDecimal DEFAULT_DAILY_LIMIT    = new BigDecimal("25000.00");
    public static final BigDecimal DEFAULT_MONTHLY_LIMIT  = new BigDecimal("100000.00");
    public static final BigDecimal DEFAULT_PER_TXN_LIMIT  = new BigDecimal("10000.00");

    @Id
    @Column(name = "card_id", nullable = false, length = 25)
    private String cardId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linked_account", nullable = false)
    private Account linkedAccount;

    @Column(name = "card_number", nullable = false, length = 16, unique = true)
    private String cardNumber;

    @Column(name = "card_holder_name", nullable = false, length = 100)
    private String cardHolderName;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Column(name = "cvv_hash", nullable = false, length = 255)
    private String cvvHash; // BCrypt hashed

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 10)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_network", nullable = false, length = 10)
    private CardNetwork cardNetwork;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "daily_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit = DEFAULT_DAILY_LIMIT;

    @Column(name = "monthly_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyLimit = DEFAULT_MONTHLY_LIMIT;

    @Column(name = "per_txn_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal perTxnLimit = DEFAULT_PER_TXN_LIMIT;

    @Column(name = "today_spent", nullable = false, precision = 15, scale = 2)
    private BigDecimal todaySpent = BigDecimal.ZERO;

    @Column(name = "month_spent", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthSpent = BigDecimal.ZERO;

    @Column(name = "online_transactions", nullable = false)
    private Boolean onlineTransactions = true;

    @Column(name = "international_transactions", nullable = false)
    private Boolean internationalTransactions = false;

    @Column(name = "contactless_payments", nullable = false)
    private Boolean contactlessPayments = true;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    public enum CardType    { DEBIT, CREDIT }
    public enum CardNetwork { VISA, MASTERCARD, RUPAY }
    public enum CardStatus  { ACTIVE, FROZEN, BLOCKED, EXPIRED }

    @PrePersist
    protected void onCreate() {
        this.issuedAt = LocalDateTime.now();
        if (this.status == null) this.status = CardStatus.ACTIVE;
        if (this.dailyLimit == null) this.dailyLimit = DEFAULT_DAILY_LIMIT;
        if (this.monthlyLimit == null) this.monthlyLimit = DEFAULT_MONTHLY_LIMIT;
        if (this.perTxnLimit == null) this.perTxnLimit = DEFAULT_PER_TXN_LIMIT;
        if (this.todaySpent == null) this.todaySpent = BigDecimal.ZERO;
        if (this.monthSpent == null) this.monthSpent = BigDecimal.ZERO;
    }

    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    public String getExpiryFormatted() {
        return String.format("%02d/%d", expiryMonth, expiryYear % 100);
    }

    public boolean isExpired() {
        YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
        return expiry.isBefore(YearMonth.now());
    }

    // Getters
    public String      getCardId()                   { return cardId; }
    public User        getUser()                     { return user; }
    public Account     getLinkedAccount()            { return linkedAccount; }
    public String      getCardNumber()               { return cardNumber; }
    public String      getCardHolderName()           { return cardHolderName; }
    public Integer     getExpiryMonth()              { return expiryMonth; }
    public Integer     getExpiryYear()               { return expiryYear; }
    public String      getCvvHash()                  { return cvvHash; }
    public CardType    getCardType()                 { return cardType; }
    public CardNetwork getCardNetwork()              { return cardNetwork; }
    public CardStatus  getStatus()                   { return status; }
    public BigDecimal  getDailyLimit()               { return dailyLimit; }
    public BigDecimal  getMonthlyLimit()             { return monthlyLimit; }
    public BigDecimal  getPerTxnLimit()              { return perTxnLimit; }
    public BigDecimal  getTodaySpent()               { return todaySpent; }
    public BigDecimal  getMonthSpent()               { return monthSpent; }
    public Boolean     getOnlineTransactions()       { return onlineTransactions; }
    public Boolean     getInternationalTransactions(){ return internationalTransactions; }
    public Boolean     getContactlessPayments()      { return contactlessPayments; }
    public String      getNickname()                 { return nickname; }
    public LocalDateTime getIssuedAt()               { return issuedAt; }
    public LocalDateTime getFrozenAt()               { return frozenAt; }
    public LocalDateTime getBlockedAt()              { return blockedAt; }

    // Setters
    public void setCardId(String v)                      { this.cardId = v; }
    public void setUser(User v)                          { this.user = v; }
    public void setLinkedAccount(Account v)              { this.linkedAccount = v; }
    public void setCardNumber(String v)                  { this.cardNumber = v; }
    public void setCardHolderName(String v)              { this.cardHolderName = v; }
    public void setExpiryMonth(Integer v)                { this.expiryMonth = v; }
    public void setExpiryYear(Integer v)                 { this.expiryYear = v; }
    public void setCvvHash(String v)                     { this.cvvHash = v; }
    public void setCardType(CardType v)                  { this.cardType = v; }
    public void setCardNetwork(CardNetwork v)            { this.cardNetwork = v; }
    public void setStatus(CardStatus v)                  { this.status = v; }
    public void setDailyLimit(BigDecimal v)              { this.dailyLimit = v; }
    public void setMonthlyLimit(BigDecimal v)            { this.monthlyLimit = v; }
    public void setPerTxnLimit(BigDecimal v)             { this.perTxnLimit = v; }
    public void setTodaySpent(BigDecimal v)              { this.todaySpent = v; }
    public void setMonthSpent(BigDecimal v)              { this.monthSpent = v; }
    public void setOnlineTransactions(Boolean v)         { this.onlineTransactions = v; }
    public void setInternationalTransactions(Boolean v)  { this.internationalTransactions = v; }
    public void setContactlessPayments(Boolean v)        { this.contactlessPayments = v; }
    public void setNickname(String v)                    { this.nickname = v; }
    public void setIssuedAt(LocalDateTime v)             { this.issuedAt = v; }
    public void setFrozenAt(LocalDateTime v)             { this.frozenAt = v; }
    public void setBlockedAt(LocalDateTime v)            { this.blockedAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String cardId; private User user; private Account linkedAccount;
        private String cardNumber; private String cardHolderName;
        private Integer expiryMonth; private Integer expiryYear;
        private String cvvHash; private CardType cardType;
        private CardNetwork cardNetwork; private String nickname;

        public Builder cardId(String v)            { this.cardId = v; return this; }
        public Builder user(User v)                { this.user = v; return this; }
        public Builder linkedAccount(Account v)    { this.linkedAccount = v; return this; }
        public Builder cardNumber(String v)        { this.cardNumber = v; return this; }
        public Builder cardHolderName(String v)    { this.cardHolderName = v; return this; }
        public Builder expiryMonth(Integer v)      { this.expiryMonth = v; return this; }
        public Builder expiryYear(Integer v)       { this.expiryYear = v; return this; }
        public Builder cvvHash(String v)           { this.cvvHash = v; return this; }
        public Builder cardType(CardType v)        { this.cardType = v; return this; }
        public Builder cardNetwork(CardNetwork v)  { this.cardNetwork = v; return this; }
        public Builder nickname(String v)          { this.nickname = v; return this; }

        public VirtualCard build() {
            VirtualCard c = new VirtualCard();
            c.cardId = this.cardId; c.user = this.user;
            c.linkedAccount = this.linkedAccount;
            c.cardNumber = this.cardNumber; c.cardHolderName = this.cardHolderName;
            c.expiryMonth = this.expiryMonth; c.expiryYear = this.expiryYear;
            c.cvvHash = this.cvvHash; c.cardType = this.cardType;
            c.cardNetwork = this.cardNetwork; c.nickname = this.nickname;
            c.status = CardStatus.ACTIVE;
            c.dailyLimit = DEFAULT_DAILY_LIMIT; c.monthlyLimit = DEFAULT_MONTHLY_LIMIT;
            c.perTxnLimit = DEFAULT_PER_TXN_LIMIT;
            c.todaySpent = BigDecimal.ZERO; c.monthSpent = BigDecimal.ZERO;
            c.onlineTransactions = true; c.internationalTransactions = false;
            c.contactlessPayments = true;
            return c;
        }
    }
}