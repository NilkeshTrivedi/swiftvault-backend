package com.swiftvault.backend.service;

import com.swiftvault.backend.dto.request.CreateAutoSavingsRuleRequest;
import com.swiftvault.backend.dto.response.AutoSavingsRuleResponse;
import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.*;
import com.swiftvault.backend.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Logger;

/**
 * FEATURE: Auto Savings Rules
 *
 * ROUND_UP logic (called from AccountServiceImpl after every withdrawal/transfer):
 *   User spends ₹237 → round up to nearest ₹50 → ₹250 → save ₹13 to goal.
 *   Round-up unit: 10, 50, or 100 (stored in rule.amount).
 *   If account has insufficient balance for round-up → silently skip (never block the main tx).
 *
 * RECURRING logic (called from ScheduledJobs on 1st of each month at 8 AM):
 *   Fixed amount (rule.amount) swept from source account into linked savings goal.
 */
@Service
public class AutoSavingsService {

    private static final Logger log = Logger.getLogger(AutoSavingsService.class.getName());

    private final AutoSavingsRuleRepository ruleRepository;
    private final AccountRepository         accountRepository;
    private final SavingsGoalRepository     goalRepository;
    private final TransactionRepository     transactionRepository;
    private final UserService               userService;

    public AutoSavingsService(AutoSavingsRuleRepository ruleRepository,
                              AccountRepository accountRepository,
                              SavingsGoalRepository goalRepository,
                              TransactionRepository transactionRepository,
                              UserService userService) {
        this.ruleRepository        = ruleRepository;
        this.accountRepository     = accountRepository;
        this.goalRepository        = goalRepository;
        this.transactionRepository = transactionRepository;
        this.userService           = userService;
    }

    @Transactional
    public AutoSavingsRuleResponse createRule(String userId, CreateAutoSavingsRuleRequest request) {
        User user = userService.findById(userId);

        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> SwiftVaultException.notFound("Source account not found"));
        if (!sourceAccount.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (sourceAccount.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Source account is not active");

        SavingsGoal goal = goalRepository.findById(request.getGoalId())
                .orElseThrow(() -> SwiftVaultException.notFound("Savings goal not found"));
        if (!goal.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this goal");
        if (goal.getStatus() != SavingsGoal.GoalStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Goal is not active");

        // Validate round-up unit
        if (request.getRuleType() == AutoSavingsRule.RuleType.ROUND_UP) {
            BigDecimal unit = request.getAmount();
            if (!unit.equals(new BigDecimal("10")) &&
                    !unit.equals(new BigDecimal("50")) &&
                    !unit.equals(new BigDecimal("100")))
                throw SwiftVaultException.badRequest("Round-up unit must be ₹10, ₹50, or ₹100");
        }

        AutoSavingsRule rule = AutoSavingsRule.builder()
                .ruleId(IdGenerator.ruleId())
                .user(user)
                .sourceAccount(sourceAccount)
                .savingsGoal(goal)
                .ruleType(request.getRuleType())
                .amount(request.getAmount())
                .build();
        ruleRepository.save(rule);
        return AutoSavingsRuleResponse.from(rule);
    }

    public List<AutoSavingsRuleResponse> getMyRules(String userId) {
        User user = userService.findById(userId);
        return ruleRepository.findByUser(user).stream()
                .map(AutoSavingsRuleResponse::from).toList();
    }

    @Transactional
    public AutoSavingsRuleResponse toggleRule(String userId, String ruleId, boolean active) {
        AutoSavingsRule rule = findAndVerify(userId, ruleId);
        rule.setActive(active);
        ruleRepository.save(rule);
        return AutoSavingsRuleResponse.from(rule);
    }

    @Transactional
    public void deleteRule(String userId, String ruleId) {
        AutoSavingsRule rule = findAndVerify(userId, ruleId);
        ruleRepository.delete(rule);
    }

    /**
     * Called from AccountServiceImpl after every withdrawal/transfer.
     * Finds all active ROUND_UP rules for this account, calculates round-up,
     * and moves the difference to the linked goal silently.
     *
     * @param accountNumber the account money was spent from
     * @param spentAmount   the actual amount spent
     */
    @Transactional
    public void applyRoundUp(String accountNumber, BigDecimal spentAmount) {
        List<AutoSavingsRule> rules = ruleRepository
                .findBySourceAccountAccountNumberAndActiveTrueAndRuleType(
                        accountNumber, AutoSavingsRule.RuleType.ROUND_UP);

        for (AutoSavingsRule rule : rules) {
            try {
                BigDecimal unit       = rule.getAmount();
                BigDecimal rounded    = roundUpTo(spentAmount, unit);
                BigDecimal saveAmount = rounded.subtract(spentAmount);

                if (saveAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

                Account account = rule.getSourceAccount();
                if (account.getBalance().compareTo(saveAmount) < 0) continue; // skip silently

                // Deduct from account
                account.setBalance(account.getBalance().subtract(saveAmount)
                        .setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);

                // Credit to goal
                SavingsGoal goal = rule.getSavingsGoal();
                goal.setSavedAmount(goal.getSavedAmount().add(saveAmount));
                if (goal.isCompleted()) goal.setStatus(SavingsGoal.GoalStatus.COMPLETED);
                goalRepository.save(goal);

                // Record transaction
                Transaction txn = Transaction.builder()
                        .transactionId(IdGenerator.transactionId())
                        .fromAccount(accountNumber)
                        .type(Transaction.TransactionType.WITHDRAW)
                        .amount(saveAmount)
                        .description("Auto round-up → Goal: " + goal.getName())
                        .build();
                transactionRepository.save(txn);

                // Update rule stats
                rule.setTotalSaved(rule.getTotalSaved().add(saveAmount));
                ruleRepository.save(rule);

                log.info("Round-up ₹" + saveAmount + " from " + accountNumber + " → Goal: " + goal.getName());
            } catch (Exception e) {
                log.warning("Round-up failed for rule " + rule.getRuleId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Called from ScheduledJobs on 1st of each month at 8 AM.
     * Processes all active RECURRING rules.
     */
    @Transactional
    public void processRecurringAutoSave() {
        List<AutoSavingsRule> rules = ruleRepository
                .findByActiveTrueAndRuleType(AutoSavingsRule.RuleType.RECURRING);

        for (AutoSavingsRule rule : rules) {
            try {
                Account account = rule.getSourceAccount();
                BigDecimal amount = rule.getAmount();

                if (account.getBalance().compareTo(amount) < 0) {
                    log.warning("Insufficient balance for recurring auto-save: " + rule.getRuleId());
                    continue;
                }

                account.setBalance(account.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);

                SavingsGoal goal = rule.getSavingsGoal();
                goal.setSavedAmount(goal.getSavedAmount().add(amount));
                if (goal.isCompleted()) goal.setStatus(SavingsGoal.GoalStatus.COMPLETED);
                goalRepository.save(goal);

                Transaction txn = Transaction.builder()
                        .transactionId(IdGenerator.transactionId())
                        .fromAccount(account.getAccountNumber())
                        .type(Transaction.TransactionType.WITHDRAW)
                        .amount(amount)
                        .description("Monthly auto-save → Goal: " + goal.getName())
                        .build();
                transactionRepository.save(txn);

                rule.setTotalSaved(rule.getTotalSaved().add(amount));
                ruleRepository.save(rule);

                log.info("Recurring auto-save ₹" + amount + " → Goal: " + goal.getName());
            } catch (Exception e) {
                log.warning("Recurring auto-save failed for rule " + rule.getRuleId() + ": " + e.getMessage());
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private BigDecimal roundUpTo(BigDecimal amount, BigDecimal unit) {
        // e.g. ₹237 rounded up to nearest ₹50 = ₹250
        BigDecimal[] divAndRem = amount.divideAndRemainder(unit);
        if (divAndRem[1].compareTo(BigDecimal.ZERO) == 0) return amount; // already a multiple
        return divAndRem[0].add(BigDecimal.ONE).multiply(unit);
    }

    private AutoSavingsRule findAndVerify(String userId, String ruleId) {
        AutoSavingsRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> SwiftVaultException.notFound("Rule not found"));
        if (!rule.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        return rule;
    }
}