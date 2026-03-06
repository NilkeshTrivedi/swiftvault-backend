package com.swiftvault.backend.service;

import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.repository.SuspiciousActivityRepository;
import com.swiftvault.backend.repository.TransactionRepository;
import com.swiftvault.backend.util.IdGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FEATURE: Suspicious Activity Detection Engine
 *
 * Called from AccountServiceImpl after every transfer/withdrawal.
 * Runs multiple checks in sequence — each check may create an alert.
 *
 * Checks:
 *   1. LARGE_TRANSACTION     → amount > ₹25,000
 *   2. OFF_HOURS_LARGE_TXN   → amount > ₹5,000 between midnight–5 AM
 *   3. RAPID_TRANSFERS       → 5+ transfers from same account in last 10 minutes
 *   4. UNUSUAL_DAILY_SPEND   → today's spend > 3× the user's 30-day daily average
 */
@Service
public class FraudDetectionService {

    private static final BigDecimal LARGE_TXN_THRESHOLD    = new BigDecimal("25000.00");
    private static final BigDecimal OFF_HOURS_THRESHOLD    = new BigDecimal("5000.00");
    private static final int        RAPID_TRANSFER_COUNT   = 5;
    private static final int        RAPID_TRANSFER_MINUTES = 10;
    private static final int        UNUSUAL_SPEND_MULTIPLIER = 3;

    private final SuspiciousActivityRepository alertRepository;
    private final TransactionRepository        transactionRepository;

    public FraudDetectionService(SuspiciousActivityRepository alertRepository,
                                 TransactionRepository transactionRepository) {
        this.alertRepository      = alertRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Run all checks after a transfer or withdrawal.
     * Call this from AccountServiceImpl.transfer() and .withdraw() AFTER saving the transaction.
     */
    @Transactional
    public void analyzeTransaction(User user, Account account,
                                   BigDecimal amount, Transaction.TransactionType type) {
        checkLargeTransaction(user, account, amount);
        checkOffHoursActivity(user, account, amount);
        if (type == Transaction.TransactionType.TRANSFER) {
            checkRapidTransfers(user, account);
        }
        checkUnusualDailySpend(user, account, amount);
    }

    /**
     * Called from AccountServiceImpl when user self-freezes account.
     */
    @Transactional
    public void flagAccountSelfFreeze(User user, String accountNumber) {
        createAlert(
                SuspiciousActivity.builder()
                        .alertId(IdGenerator.alertId())
                        .user(user)
                        .alertType(SuspiciousActivity.AlertType.ACCOUNT_SELF_FROZEN)
                        .description("User self-froze account " + accountNumber + ". May indicate suspected compromise.")
                        .accountNumber(accountNumber)
                        .riskScore(6)
                        .build()
        );
    }

    /**
     * Called from DeviceTrackingService when a brand-new IP logs in.
     */
    @Transactional
    public void flagNewIpLogin(User user, String ipAddress) {
        createAlert(
                SuspiciousActivity.builder()
                        .alertId(IdGenerator.alertId())
                        .user(user)
                        .alertType(SuspiciousActivity.AlertType.NEW_IP_LOGIN)
                        .description("Login from new IP address: " + ipAddress)
                        .ipAddress(ipAddress)
                        .riskScore(5)
                        .build()
        );
    }

    // ─── Admin queries ────────────────────────────────────────────────────────

    public Page<SuspiciousActivity> getAllAlerts(int page, int size) {
        return alertRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public Page<SuspiciousActivity> getOpenAlerts(int page, int size) {
        return alertRepository.findByStatusOrderByCreatedAtDesc(
                SuspiciousActivity.AlertStatus.OPEN, PageRequest.of(page, size));
    }

    public List<SuspiciousActivity> getMyAlerts(User user) {
        return alertRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Map<String, Long> getAlertSummary() {
        return Map.of(
                "open",         alertRepository.countByStatus(SuspiciousActivity.AlertStatus.OPEN),
                "reviewed",     alertRepository.countByStatus(SuspiciousActivity.AlertStatus.REVIEWED),
                "resolved",     alertRepository.countByStatus(SuspiciousActivity.AlertStatus.RESOLVED),
                "falsePositive",alertRepository.countByStatus(SuspiciousActivity.AlertStatus.FALSE_POSITIVE)
        );
    }

    @Transactional
    public SuspiciousActivity resolveAlert(String alertId, String adminId,
                                           String status, String notes) {
        SuspiciousActivity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
        alert.setStatus(SuspiciousActivity.AlertStatus.valueOf(status.toUpperCase()));
        alert.setResolvedBy(adminId);
        alert.setResolutionNotes(notes);
        alert.setResolvedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }

    // ─── Private check methods ────────────────────────────────────────────────

    private void checkLargeTransaction(User user, Account account, BigDecimal amount) {
        if (amount.compareTo(LARGE_TXN_THRESHOLD) > 0) {
            int risk = amount.compareTo(new BigDecimal("100000")) > 0 ? 9 : 7;
            createAlert(SuspiciousActivity.builder()
                    .alertId(IdGenerator.alertId())
                    .user(user)
                    .alertType(SuspiciousActivity.AlertType.LARGE_TRANSACTION)
                    .description("Large transaction of ₹" + amount + " from account " + account.getAccountNumber())
                    .amount(amount)
                    .accountNumber(account.getAccountNumber())
                    .riskScore(risk)
                    .build());
        }
    }

    private void checkOffHoursActivity(User user, Account account, BigDecimal amount) {
        int hour = LocalDateTime.now().getHour();
        boolean isOffHours = hour >= 0 && hour < 5;
        if (isOffHours && amount.compareTo(OFF_HOURS_THRESHOLD) > 0) {
            createAlert(SuspiciousActivity.builder()
                    .alertId(IdGenerator.alertId())
                    .user(user)
                    .alertType(SuspiciousActivity.AlertType.OFF_HOURS_LARGE_TXN)
                    .description("Large transaction of ₹" + amount + " at " + hour + ":00 (off-hours) from " + account.getAccountNumber())
                    .amount(amount)
                    .accountNumber(account.getAccountNumber())
                    .riskScore(8)
                    .build());
        }
    }

    private void checkRapidTransfers(User user, Account account) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RAPID_TRANSFER_MINUTES);
        List<SuspiciousActivity> recent = alertRepository.findRecentByUserAndType(
                user, SuspiciousActivity.AlertType.RAPID_TRANSFERS, windowStart);

        // Count actual transactions in the window (raw count from transaction table)
        List<com.swiftvault.backend.entity.Transaction> recentTxns =
                transactionRepository.findByAccountNumber(account.getAccountNumber())
                        .stream()
                        .filter(t -> t.getType() == Transaction.TransactionType.TRANSFER)
                        .filter(t -> t.getTimestamp().isAfter(windowStart))
                        .toList();

        if (recentTxns.size() >= RAPID_TRANSFER_COUNT && recent.isEmpty()) {
            createAlert(SuspiciousActivity.builder()
                    .alertId(IdGenerator.alertId())
                    .user(user)
                    .alertType(SuspiciousActivity.AlertType.RAPID_TRANSFERS)
                    .description(recentTxns.size() + " transfers in the last " + RAPID_TRANSFER_MINUTES +
                            " minutes from account " + account.getAccountNumber())
                    .accountNumber(account.getAccountNumber())
                    .riskScore(8)
                    .build());
        }
    }

    private void checkUnusualDailySpend(User user, Account account, BigDecimal amount) {
        // Get last 30 days of withdrawals to compute daily average
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime todayStart    = LocalDateTime.now().toLocalDate().atStartOfDay();

        List<Transaction> allTxns = transactionRepository.findByAccountNumber(account.getAccountNumber());

        BigDecimal last30DaysSpend = allTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.WITHDRAW ||
                        t.getType() == Transaction.TransactionType.TRANSFER)
                .filter(t -> t.getFromAccount().equals(account.getAccountNumber()))
                .filter(t -> t.getTimestamp().isAfter(thirtyDaysAgo) && t.getTimestamp().isBefore(todayStart))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyAverage = last30DaysSpend.divide(new BigDecimal("30"), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal todaySpend = allTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.WITHDRAW ||
                        t.getType() == Transaction.TransactionType.TRANSFER)
                .filter(t -> t.getFromAccount().equals(account.getAccountNumber()))
                .filter(t -> t.getTimestamp().isAfter(todayStart))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal threshold = dailyAverage.multiply(new BigDecimal(UNUSUAL_SPEND_MULTIPLIER));

        if (dailyAverage.compareTo(BigDecimal.ZERO) > 0 && todaySpend.compareTo(threshold) > 0) {
            createAlert(SuspiciousActivity.builder()
                    .alertId(IdGenerator.alertId())
                    .user(user)
                    .alertType(SuspiciousActivity.AlertType.UNUSUAL_DAILY_SPEND)
                    .description("Today's spend ₹" + todaySpend + " is " + UNUSUAL_SPEND_MULTIPLIER +
                            "× above the 30-day daily average of ₹" + dailyAverage +
                            " for account " + account.getAccountNumber())
                    .amount(todaySpend)
                    .accountNumber(account.getAccountNumber())
                    .riskScore(7)
                    .build());
        }
    }

    private void createAlert(SuspiciousActivity alert) {
        try {
            alertRepository.save(alert);
        } catch (Exception e) {
            // Never let fraud detection crash the main transaction
        }
    }
}