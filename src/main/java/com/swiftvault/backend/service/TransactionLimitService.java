package com.swiftvault.backend.service;

import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FEATURE: Transaction Limit Controls
 *
 * Users set per-account limits:
 *   maxSingleTransaction → single transfer/withdrawal cap
 *   dailyLimit           → total outflow cap per day
 *   monthlyLimit         → total outflow cap per month
 *
 * Integration:
 *   Call enforceLimit(accountNumber, amount) from AccountServiceImpl
 *   BEFORE processing any transfer or withdrawal.
 *
 *   If a limit config doesn't exist for an account, defaults are applied:
 *     single  → ₹50,000
 *     daily   → ₹1,00,000
 *     monthly → ₹5,00,000
 */
@Service
public class TransactionLimitService {

    // Defaults applied when no limit config exists for the account
    private static final BigDecimal DEFAULT_SINGLE  = new BigDecimal("50000.00");
    private static final BigDecimal DEFAULT_DAILY   = new BigDecimal("100000.00");
    private static final BigDecimal DEFAULT_MONTHLY = new BigDecimal("500000.00");

    private final TransactionLimitRepository limitRepository;
    private final TransactionRepository      transactionRepository;
    private final AccountRepository          accountRepository;
    private final UserService                userService;

    public TransactionLimitService(TransactionLimitRepository limitRepository,
                                   TransactionRepository transactionRepository,
                                   AccountRepository accountRepository,
                                   UserService userService) {
        this.limitRepository      = limitRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository    = accountRepository;
        this.userService          = userService;
    }

    // ─── Enforce (called before every debit) ─────────────────────────────────

    /**
     * Throws SwiftVaultException if any limit is breached.
     * Call this from AccountServiceImpl.transfer() and .withdraw()
     * BEFORE touching any balances.
     */
    public void enforceLimit(String accountNumber, BigDecimal amount) {
        TransactionLimit limits = limitRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> defaultLimits(accountNumber));

        // ── 1. Single transaction cap ─────────────────────────────────────────
        BigDecimal maxSingle = limits.getMaxSingleTransaction();
        if (maxSingle.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(maxSingle) > 0) {
            throw SwiftVaultException.badRequest(
                    "Amount ₹" + amount.toPlainString() +
                            " exceeds your single-transaction limit of ₹" + maxSingle.toPlainString() +
                            ". Update your limits via Settings → Transaction Limits.");
        }

        // ── 2. Daily cap ──────────────────────────────────────────────────────
        BigDecimal dailyLimit = limits.getDailyLimit();
        if (dailyLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal todaySpent = getTodaySpend(accountNumber);
            if (todaySpent.add(amount).compareTo(dailyLimit) > 0) {
                BigDecimal remaining = dailyLimit.subtract(todaySpent);
                throw SwiftVaultException.badRequest(
                        "Daily limit of ₹" + dailyLimit.toPlainString() + " would be exceeded. " +
                                "You have ₹" + remaining.max(BigDecimal.ZERO).toPlainString() + " remaining today.");
            }
        }

        // ── 3. Monthly cap ────────────────────────────────────────────────────
        BigDecimal monthlyLimit = limits.getMonthlyLimit();
        if (monthlyLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal monthSpent = getMonthSpend(accountNumber);
            if (monthSpent.add(amount).compareTo(monthlyLimit) > 0) {
                BigDecimal remaining = monthlyLimit.subtract(monthSpent);
                throw SwiftVaultException.badRequest(
                        "Monthly limit of ₹" + monthlyLimit.toPlainString() + " would be exceeded. " +
                                "You have ₹" + remaining.max(BigDecimal.ZERO).toPlainString() + " remaining this month.");
            }
        }
    }

    // ─── Configuration endpoints ──────────────────────────────────────────────

    @Transactional
    public TransactionLimit updateLimits(String userId, String accountNumber,
                                         BigDecimal maxSingle, BigDecimal daily, BigDecimal monthly) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found"));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");

        TransactionLimit limits = limitRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> {
                    TransactionLimit t = new TransactionLimit();
                    t.setAccountNumber(accountNumber);
                    t.setUser(account.getUser());
                    return t;
                });

        if (maxSingle != null) limits.setMaxSingleTransaction(maxSingle);
        if (daily     != null) limits.setDailyLimit(daily);
        if (monthly   != null) limits.setMonthlyLimit(monthly);

        return limitRepository.save(limits);
    }

    public TransactionLimit getLimits(String userId, String accountNumber) {
        accountRepository.findByAccountNumber(accountNumber)
                .filter(a -> a.getUser().getUserId().equals(userId))
                .orElseThrow(() -> SwiftVaultException.forbidden("Access denied"));

        return limitRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> defaultLimits(accountNumber));
    }

    public List<TransactionLimit> getAllMyLimits(String userId) {
        User user = userService.findById(userId);
        return limitRepository.findByUser(user);
    }

    /** Returns current spend vs limit for today and this month — useful for dashboard */
    public Map<String, Object> getSpendSummary(String userId, String accountNumber) {
        getLimits(userId, accountNumber); // ownership check

        TransactionLimit limits = limitRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> defaultLimits(accountNumber));

        BigDecimal todaySpent  = getTodaySpend(accountNumber);
        BigDecimal monthSpent  = getMonthSpend(accountNumber);

        return Map.of(
                "accountNumber",    accountNumber,
                "singleLimit",      limits.getMaxSingleTransaction(),
                "dailyLimit",       limits.getDailyLimit(),
                "monthlyLimit",     limits.getMonthlyLimit(),
                "todaySpent",       todaySpent,
                "monthSpent",       monthSpent,
                "dailyRemaining",   limits.getDailyLimit().subtract(todaySpent).max(BigDecimal.ZERO),
                "monthlyRemaining", limits.getMonthlyLimit().subtract(monthSpent).max(BigDecimal.ZERO),
                "dailyUsedPercent", limits.getDailyLimit().compareTo(BigDecimal.ZERO) > 0
                        ? todaySpent.multiply(new BigDecimal("100"))
                        .divide(limits.getDailyLimit(), 1, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private BigDecimal getTodaySpend(String accountNumber) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return transactionRepository.findByAccountNumber(accountNumber).stream()
                .filter(t -> isDebit(t, accountNumber))
                .filter(t -> t.getTimestamp().isAfter(startOfDay))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getMonthSpend(String accountNumber) {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .toLocalDate().withDayOfMonth(1).atStartOfDay();
        return transactionRepository.findByAccountNumber(accountNumber).stream()
                .filter(t -> isDebit(t, accountNumber))
                .filter(t -> t.getTimestamp().isAfter(startOfMonth))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isDebit(Transaction t, String accountNumber) {
        return (t.getType() == Transaction.TransactionType.WITHDRAW ||
                t.getType() == Transaction.TransactionType.TRANSFER) &&
                accountNumber.equals(t.getFromAccount());
    }

    private TransactionLimit defaultLimits(String accountNumber) {
        TransactionLimit t = new TransactionLimit();
        t.setAccountNumber(accountNumber);
        t.setMaxSingleTransaction(DEFAULT_SINGLE);
        t.setDailyLimit(DEFAULT_DAILY);
        t.setMonthlyLimit(DEFAULT_MONTHLY);
        return t;
    }
}