package com.swiftvault.backend.service;

import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

/**
 * FEATURE: Account Self-Freeze
 *
 * Allows users to instantly freeze their own account with PIN verification.
 * Useful when they suspect their account has been compromised.
 *
 * Freeze: requires only transaction PIN (fast emergency action).
 * Unfreeze: requires transaction PIN + confirms intent (slightly more friction).
 *
 * FraudDetectionService is also notified so admin sees an alert.
 *
 * Integrated into: AccountController (new endpoints added below).
 */
@Service
public class AccountSelfFreezeService {

    private static final Logger log = Logger.getLogger(AccountSelfFreezeService.class.getName());

    private final AccountRepository     accountRepository;
    private final UserService           userService;
    private final PasswordEncoder       passwordEncoder;
    private final FraudDetectionService fraudDetectionService;

    public AccountSelfFreezeService(AccountRepository accountRepository,
                                    UserService userService,
                                    PasswordEncoder passwordEncoder,
                                    FraudDetectionService fraudDetectionService) {
        this.accountRepository    = accountRepository;
        this.userService          = userService;
        this.passwordEncoder      = passwordEncoder;
        this.fraudDetectionService = fraudDetectionService;
    }

    /**
     * User freezes their own account.
     * PIN verification required — but no minimum balance or status check needed.
     * Any ACTIVE account can be self-frozen immediately.
     */
    @Transactional
    public void selfFreeze(String userId, String accountNumber, String transactionPin) {
        Account account = findAndVerifyOwnership(userId, accountNumber);
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is already " + account.getStatus());

        User user = userService.findById(userId);
        verifyPin(user, transactionPin);

        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);

        // Notify fraud detection — admin will see an ACCOUNT_SELF_FROZEN alert
        fraudDetectionService.flagAccountSelfFreeze(user, accountNumber);

        log.warning("Account self-frozen by user: " + userId + " → " + accountNumber);
    }

    /**
     * User unfreezes their own account.
     * Same PIN verification required to confirm identity.
     * Only self-frozen accounts can be unfrozen by the user;
     * admin-frozen accounts require admin action.
     */
    @Transactional
    public void selfUnfreeze(String userId, String accountNumber, String transactionPin) {
        Account account = findAndVerifyOwnership(userId, accountNumber);
        if (account.getStatus() != Account.AccountStatus.FROZEN)
            throw SwiftVaultException.badRequest("Account is not frozen");

        User user = userService.findById(userId);
        verifyPin(user, transactionPin);

        account.setStatus(Account.AccountStatus.ACTIVE);
        accountRepository.save(account);

        log.info("Account self-unfrozen by user: " + userId + " → " + accountNumber);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Account findAndVerifyOwnership(String userId, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        return account;
    }

    private void verifyPin(User user, String pin) {
        if (!user.hasTransactionPin())
            throw SwiftVaultException.badRequest("Set a transaction PIN before using this feature");
        if (!passwordEncoder.matches(pin, user.getPinHash()))
            throw SwiftVaultException.unauthorized("Incorrect transaction PIN");
    }
}