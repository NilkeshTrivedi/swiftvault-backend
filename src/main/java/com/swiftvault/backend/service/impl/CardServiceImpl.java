package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.CardLimitRequest;
import com.swiftvault.backend.dto.request.CardRequest;
import com.swiftvault.backend.dto.response.CardResponse;
import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.repository.*;
import com.swiftvault.backend.service.CardService;
import com.swiftvault.backend.service.UserService;
import com.swiftvault.backend.util.IdGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import com.swiftvault.backend.exception.SwiftVaultException;

@Service
public class CardServiceImpl implements CardService {

    private static final Logger log = Logger.getLogger(CardServiceImpl.class.getName());
    private static final int MAX_CARDS_PER_USER = 3;

    private final VirtualCardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public CardServiceImpl(VirtualCardRepository cardRepository,
                           AccountRepository accountRepository,
                           UserService userService,
                           PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public CardResponse issueCard(String userId, CardRequest request) {
        User user = userService.findById(userId);
        Account account = accountRepository.findById(request.getAccountNumber())
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found"));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active");
        if (cardRepository.countByUser(user) >= MAX_CARDS_PER_USER)
            throw SwiftVaultException.badRequest("Maximum " + MAX_CARDS_PER_USER + " cards allowed per user");

        String cardNumber = generateUniqueCardNumber(request.getCardNetwork());
        String rawCvv     = String.format("%03d", new Random().nextInt(1000));
        String cvvHash    = passwordEncoder.encode(rawCvv);
        int expiryYear  = LocalDateTime.now().getYear() + 3;
        int expiryMonth = LocalDateTime.now().getMonthValue();

        VirtualCard card = VirtualCard.builder()
                .cardId(IdGenerator.cardId())
                .user(user)
                .linkedAccount(account)
                .cardNumber(cardNumber)
                .cardHolderName(user.getFullName().toUpperCase())
                .expiryMonth(expiryMonth)
                .expiryYear(expiryYear)
                .cvvHash(cvvHash)
                .cardType(request.getCardType())
                .cardNetwork(request.getCardNetwork() != null ? request.getCardNetwork() : VirtualCard.CardNetwork.VISA)
                .nickname(request.getNickname())
                .build();
        cardRepository.save(card);
        log.info("Card issued: " + card.getCardId() + " for user: " + userId);

        // Return with real CVV only at issuance — store hashed
        CardResponse response = CardResponse.from(card);
        log.info("Card CVV (shown once): " + rawCvv + " for card: " + card.getMaskedCardNumber());
        return response;
    }

    @Override
    public List<CardResponse> getMyCards(String userId) {
        User user = userService.findById(userId);
        return cardRepository.findByUser(user).stream().map(CardResponse::from).toList();
    }

    @Override
    public CardResponse getCard(String userId, String cardId) {
        VirtualCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> SwiftVaultException.notFound("Card not found"));
        if (!card.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public CardResponse freezeCard(String userId, String cardId) {
        VirtualCard card = getCardAndVerify(userId, cardId);
        if (card.getStatus() != VirtualCard.CardStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Card is not active");
        card.setStatus(VirtualCard.CardStatus.FROZEN);
        card.setFrozenAt(LocalDateTime.now());
        cardRepository.save(card);
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public CardResponse unfreezeCard(String userId, String cardId) {
        VirtualCard card = getCardAndVerify(userId, cardId);
        if (card.getStatus() != VirtualCard.CardStatus.FROZEN)
            throw SwiftVaultException.badRequest("Card is not frozen");
        card.setStatus(VirtualCard.CardStatus.ACTIVE);
        card.setFrozenAt(null);
        cardRepository.save(card);
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public CardResponse blockCard(String userId, String cardId) {
        VirtualCard card = getCardAndVerify(userId, cardId);
        if (card.getStatus() == VirtualCard.CardStatus.BLOCKED)
            throw SwiftVaultException.badRequest("Card is already blocked");
        card.setStatus(VirtualCard.CardStatus.BLOCKED);
        card.setBlockedAt(LocalDateTime.now());
        cardRepository.save(card);
        log.warning("Card blocked: " + cardId + " by user: " + userId);
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public CardResponse updateLimits(String userId, CardLimitRequest request) {
        VirtualCard card = getCardAndVerify(userId, request.getCardId());
        if (request.getDailyLimit() != null)   card.setDailyLimit(request.getDailyLimit());
        if (request.getMonthlyLimit() != null) card.setMonthlyLimit(request.getMonthlyLimit());
        if (request.getPerTxnLimit() != null)  card.setPerTxnLimit(request.getPerTxnLimit());
        if (request.getOnlineTransactions() != null)        card.setOnlineTransactions(request.getOnlineTransactions());
        if (request.getInternationalTransactions() != null) card.setInternationalTransactions(request.getInternationalTransactions());
        if (request.getContactlessPayments() != null)       card.setContactlessPayments(request.getContactlessPayments());
        cardRepository.save(card);
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public CardResponse toggleFeature(String userId, String cardId, String feature, boolean enabled) {
        VirtualCard card = getCardAndVerify(userId, cardId);
        switch (feature.toLowerCase()) {
            case "online"         -> card.setOnlineTransactions(enabled);
            case "international"  -> card.setInternationalTransactions(enabled);
            case "contactless"    -> card.setContactlessPayments(enabled);
            default -> throw SwiftVaultException.badRequest("Unknown feature: " + feature);
        }
        cardRepository.save(card);
        return CardResponse.from(card);
    }

    private VirtualCard getCardAndVerify(String userId, String cardId) {
        VirtualCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> SwiftVaultException.notFound("Card not found"));
        if (!card.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        return card;
    }

    private String generateUniqueCardNumber(VirtualCard.CardNetwork network) {
        String prefix = switch (network) {
            case MASTERCARD -> "5";
            case RUPAY      -> "6";
            default         -> "4"; // VISA
        };
        String number;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(prefix);
            Random rnd = new Random();
            while (sb.length() < 16) sb.append(rnd.nextInt(10));
            number = sb.toString();
            attempts++;
            if (attempts > 100) throw SwiftVaultException.internal("Could not generate unique card number");
        } while (cardRepository.existsByCardNumber(number));
        return number;
    }
}
