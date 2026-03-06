package com.swiftvault.backend.service;

import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.*;
import com.swiftvault.backend.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * FEATURE: Referral System
 *
 * Integration points:
 *   - UserServiceImpl.register()         → call generateCodeForNewUser(user)
 *   - UserServiceImpl.register() cont.   → if referralCode in request, call markReferred(code, newUser)
 *   - AccountServiceImpl.openAccount()   → call onFirstAccountOpened(userId) to pay out reward
 */
@Service
public class ReferralService {

    private static final Logger log = Logger.getLogger(ReferralService.class.getName());
    private static final BigDecimal REFERRER_REWARD = new BigDecimal("200.00");
    private static final BigDecimal REFERRED_REWARD = new BigDecimal("200.00");

    private final ReferralRepository    referralRepository;
    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;

    public ReferralService(ReferralRepository referralRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository) {
        this.referralRepository  = referralRepository;
        this.accountRepository   = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Called on every new user registration.
     * Generates a unique referral code and saves a PENDING referral record.
     */
    @Transactional
    public String generateCodeForNewUser(User user) {
        String code = generateUniqueCode(user);
        Referral referral = Referral.builder()
                .referralId(IdGenerator.referralId())
                .referrer(user)
                .referralCode(code)
                .build();
        referralRepository.save(referral);
        log.info("Generated referral code " + code + " for user " + user.getUserId());
        return code;
    }

    /**
     * Called during registration if a referral code was provided.
     * Links the new user as the "referred" party on the matching referral.
     */
    @Transactional
    public void markReferred(String referralCode, User newUser) {
        referralRepository.findByReferralCode(referralCode.toUpperCase()).ifPresent(referral -> {
            if (referral.getStatus() == Referral.ReferralStatus.PENDING &&
                    !referral.getReferrer().getUserId().equals(newUser.getUserId())) {
                referral.setReferred(newUser);
                referral.setStatus(Referral.ReferralStatus.REGISTERED);
                referralRepository.save(referral);
                log.info("Referral " + referralCode + " linked to new user " + newUser.getUserId());
            }
        });
    }

    /**
     * Called from AccountServiceImpl when a user opens their FIRST account.
     * If this user was referred, pays ₹200 to both the referrer and the referred.
     */
    @Transactional
    public void onFirstAccountOpened(User newUser, Account newAccount) {
        // Only trigger on first-ever account
        long accountCount = accountRepository.findByUser(newUser).size();
        if (accountCount > 1) return; // not the first account

        referralRepository.findByReferred(newUser).ifPresent(referral -> {
            if (referral.getStatus() != Referral.ReferralStatus.REGISTERED) return;

            // ── Pay referrer ──────────────────────────────────────────────────
            List<Account> referrerAccounts = accountRepository
                    .findByUserAndStatus(referral.getReferrer(), Account.AccountStatus.ACTIVE);
            if (!referrerAccounts.isEmpty()) {
                Account referrerAccount = referrerAccounts.get(0);
                referrerAccount.setBalance(referrerAccount.getBalance().add(REFERRER_REWARD)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
                accountRepository.save(referrerAccount);

                Transaction txn = Transaction.builder()
                        .transactionId(IdGenerator.transactionId())
                        .fromAccount("REFERRAL-SYSTEM")
                        .toAccount(referrerAccount.getAccountNumber())
                        .type(Transaction.TransactionType.DEPOSIT)
                        .amount(REFERRER_REWARD)
                        .description("Referral reward — your friend " + newUser.getFullName() + " joined SwiftVault!")
                        .build();
                transactionRepository.save(txn);
            }

            // ── Pay referred user ─────────────────────────────────────────────
            newAccount.setBalance(newAccount.getBalance().add(REFERRED_REWARD)
                    .setScale(2, java.math.RoundingMode.HALF_UP));
            accountRepository.save(newAccount);

            Transaction txn = Transaction.builder()
                    .transactionId(IdGenerator.transactionId())
                    .fromAccount("REFERRAL-SYSTEM")
                    .toAccount(newAccount.getAccountNumber())
                    .type(Transaction.TransactionType.DEPOSIT)
                    .amount(REFERRED_REWARD)
                    .description("Welcome bonus — referred by " + referral.getReferrer().getFullName())
                    .build();
            transactionRepository.save(txn);

            // ── Mark referral complete ────────────────────────────────────────
            referral.setStatus(Referral.ReferralStatus.COMPLETED);
            referral.setRewardCreditedAt(LocalDateTime.now());
            referralRepository.save(referral);

            log.info("Referral reward paid: ₹" + REFERRER_REWARD +
                    " to referrer " + referral.getReferrer().getUserId() +
                    " + ₹" + REFERRED_REWARD + " to new user " + newUser.getUserId());
        });
    }

    // ─── Query methods ────────────────────────────────────────────────────────

    public Map<String, Object> getMyReferralInfo(String userId, User user) {
        List<Referral> myReferrals = referralRepository.findByReferrerOrderByCreatedAtDesc(user);
        String myCode = myReferrals.stream()
                .filter(r -> r.getReferred() == null && r.getStatus() == Referral.ReferralStatus.PENDING)
                .findFirst()
                .map(Referral::getReferralCode)
                .orElse("N/A");

        long completed = myReferrals.stream()
                .filter(r -> r.getStatus() == Referral.ReferralStatus.COMPLETED).count();

        BigDecimal totalEarned = new BigDecimal(completed).multiply(REFERRER_REWARD);

        return Map.of(
                "myReferralCode",   myCode,
                "totalReferrals",   myReferrals.size(),
                "completedReferrals", completed,
                "pendingReferrals",  myReferrals.stream().filter(r -> r.getStatus() == Referral.ReferralStatus.REGISTERED).count(),
                "totalEarned",      totalEarned,
                "rewardPerReferral","₹" + REFERRER_REWARD + " for you + ₹" + REFERRED_REWARD + " for your friend",
                "referrals",        myReferrals.stream().map(this::toMap).toList()
        );
    }

    private Map<String, Object> toMap(Referral r) {
        return Map.of(
                "referralCode",   r.getReferralCode(),
                "status",         r.getStatus(),
                "referredUser",   r.getReferred() != null ? r.getReferred().getFullName() : "Not yet registered",
                "rewardCredited", r.getRewardCreditedAt() != null ? r.getRewardCreditedAt().toString() : "Pending",
                "createdAt",      r.getCreatedAt().toString()
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String generateUniqueCode(User user) {
        // Format: SWIFT + first 3 chars of name (uppercase) + 4 random digits
        // e.g. SWIFTNIL7823
        String prefix = "SWIFT" + user.getFullName()
                .replaceAll("[^A-Za-z]", "")
                .toUpperCase()
                .substring(0, Math.min(3, user.getFullName().replaceAll("[^A-Za-z]", "").length()));
        String code;
        int attempts = 0;
        do {
            code = prefix + String.format("%04d", new Random().nextInt(10000));
            attempts++;
            if (attempts > 50) code = "SWIFT" + IdGenerator.shortId();
        } while (referralRepository.existsByReferralCode(code));
        return code;
    }
}