package com.swiftvault.backend.dto.request;

import com.swiftvault.backend.entity.VirtualCard;
import jakarta.validation.constraints.*;

public class CardRequest {
    @NotBlank
    private String accountNumber;
    @NotNull
    private VirtualCard.CardType cardType;
    private VirtualCard.CardNetwork cardNetwork = VirtualCard.CardNetwork.VISA;
    private String nickname;

    public String getAccountNumber()                  { return accountNumber; }
    public VirtualCard.CardType getCardType()         { return cardType; }
    public VirtualCard.CardNetwork getCardNetwork()   { return cardNetwork; }
    public String getNickname()                       { return nickname; }
    public void setAccountNumber(String v)            { this.accountNumber = v; }
    public void setCardType(VirtualCard.CardType v)   { this.cardType = v; }
    public void setCardNetwork(VirtualCard.CardNetwork v){ this.cardNetwork = v; }
    public void setNickname(String v)                 { this.nickname = v; }
}
