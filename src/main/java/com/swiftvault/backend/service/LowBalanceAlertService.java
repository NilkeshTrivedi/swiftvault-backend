package com.swiftvault.backend.service;

import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.*;
import com.swiftvault.backend.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * FEATURE: Low Balance Alerts
 *
 * Users configure a per-account threshold via PUT /api/alerts/threshold.
 * After every withdrawal/transfer, AccountServiceImpl calls checkAndAlert().
 * If balance < threshold → a LowBalanceAlert is saved.
 * Users fetch unread alerts via GET /api/alerts/balance.
 * ScheduledJobs also runs a nightly scan to catch any missed cases.
 */
@Service
public class LowBalanceAlertService {

    private final LowBalanceAlertRepository alertRepository;
    private final AlertThresholdRepository  thresholdRepository;
    private final AccountRepository         accountRepository;
    private final UserService               userService;

    public LowBalanceAlertService(LowBalanceAlertRepository alertRepository,
                                  AlertThresholdRepository thresholdRepository,
                                  AccountRepository accountRepository,
                                  UserService userService) {
        this.alertRepository    = alertRepository;
        this.thresholdRepository = thresholdRepository;
        this.accountRepository  = accountRepository;
        this.userService        = userService;
    }

    // ─── Threshold config ─────────────────────────────────────────────────────

    @Transactional
    public AlertThreshold setThreshold(String userId, String accountNumber, BigDecimal threshold) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found"));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (threshold.compareTo(BigDecimal.ZERO) < 0)
            throw SwiftVaultException.badRequest("Threshold cannot be negative");

        AlertThreshold at = thresholdRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> {
                    AlertThreshold newAt = new AlertThreshold();
                    newAt.setAccountNumber(accountNumber);
                    newAt.setUser(account.getUser());
                    return newAt;
                });
        at.setThresholdAmount(threshold);
        return thresholdRepository.save(at);
    }

    public List<AlertThreshold> getMyThresholds(String userId) {
        User user = userService.findById(userId);
        return thresholdRepository.findByUser(user);
    }

    // ─── Alert checking (called after every debit) ────────────────────────────

    /**
     * Call this from AccountServiceImpl after every withdrawal or transfer.
     * Silently skips if no threshold is configured for the account.
     */
    @Transactional
    public void checkAndAlert(Account account) {
        thresholdRepository.findByAccountNumber(account.getAccountNumber()).ifPresent(threshold -> {
            if (account.getBalance().compareTo(threshold.getThresholdAmount()) < 0) {
                LowBalanceAlert alert = LowBalanceAlert.builder()
                        .alertId(IdGenerator.alertId())
                        .user(account.getUser())
                        .accountNumber(account.getAccountNumber())
                        .thresholdAmount(threshold.getThresholdAmount())
                        .balanceAtAlert(account.getBalance())
                        .build();
                alertRepository.save(alert);
            }
        });
    }

    // ─── Fetch alerts ─────────────────────────────────────────────────────────

    public List<LowBalanceAlert> getMyAlerts(String userId) {
        User user = userService.findById(userId);
        return alertRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<LowBalanceAlert> getUnreadAlerts(String userId) {
        User user = userService.findById(userId);
        return alertRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(String userId) {
        User user = userService.findById(userId);
        return alertRepository.countByUserAndReadFalse(user);
    }

    @Transactional
    public void markAllRead(String userId) {
        User user = userService.findById(userId);
        List<LowBalanceAlert> unread = alertRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
        unread.forEach(a -> a.setRead(true));
        alertRepository.saveAll(unread);
    }

    // ─── Nightly scan (called from ScheduledJobs) ─────────────────────────────

    /**
     * Scans all accounts with configured thresholds and creates alerts
     * for any that are currently below threshold. Prevents duplicate alerts
     * by checking if an unread alert already exists for the same account today.
     */
    @Transactional
    public void runNightlyScan() {
        List<AlertThreshold> thresholds = thresholdRepository.findAll();
        for (AlertThreshold threshold : thresholds) {
            try {
                accountRepository.findByAccountNumber(threshold.getAccountNumber()).ifPresent(account -> {
                    if (account.getBalance().compareTo(threshold.getThresholdAmount()) < 0) {
                        // Check if an unread alert already exists today for this account
                        long existing = alertRepository.countByUserAndReadFalse(account.getUser());
                        if (existing == 0) {
                            LowBalanceAlert alert = LowBalanceAlert.builder()
                                    .alertId(IdGenerator.alertId())
                                    .user(account.getUser())
                                    .accountNumber(account.getAccountNumber())
                                    .thresholdAmount(threshold.getThresholdAmount())
                                    .balanceAtAlert(account.getBalance())
                                    .build();
                            alertRepository.save(alert);
                        }
                    }
                });
            } catch (Exception e) {
                // Never let one failure stop the entire scan
            }
        }
    }
}