package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.DepositRequest;
import com.swiftvault.backend.dto.request.OpenAccountRequest;
import com.swiftvault.backend.dto.request.TransferRequest;
import com.swiftvault.backend.dto.request.WithdrawRequest;
import com.swiftvault.backend.dto.response.AccountResponse;
import com.swiftvault.backend.dto.response.TransactionResponse;
import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.AccountRepository;
import com.swiftvault.backend.repository.TransactionRepository;
import com.swiftvault.backend.service.*;
import com.swiftvault.backend.util.IdGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository       accountRepository;
    private final TransactionRepository   transactionRepository;
    private final UserService             userService;
    private final PasswordEncoder         passwordEncoder;

    // ── Phase 3B services (wired for fraud, limits, savings, alerts, referral) ─
    private final FraudDetectionService   fraudDetectionService;
    private final TransactionLimitService transactionLimitService;
    private final AutoSavingsService      autoSavingsService;
    private final LowBalanceAlertService  lowBalanceAlertService;
    private final ReferralService         referralService;

    public AccountServiceImpl(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              UserService userService,
                              PasswordEncoder passwordEncoder,
                              FraudDetectionService fraudDetectionService,
                              TransactionLimitService transactionLimitService,
                              AutoSavingsService autoSavingsService,
                              LowBalanceAlertService lowBalanceAlertService,
                              ReferralService referralService) {
        this.accountRepository     = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userService           = userService;
        this.passwordEncoder       = passwordEncoder;
        this.fraudDetectionService = fraudDetectionService;
        this.transactionLimitService = transactionLimitService;
        this.autoSavingsService    = autoSavingsService;
        this.lowBalanceAlertService = lowBalanceAlertService;
        this.referralService       = referralService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Open Account
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AccountResponse openAccount(String userId, OpenAccountRequest request) {
        User user = userService.findById(userId);

        // Validate account type
        if (request.getAccountType() == null)
            throw SwiftVaultException.badRequest("Account type is required");

        // Generate unique account number
        String accountNumber;
        do {
            accountNumber = IdGenerator.accountNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUser(user);
        account.setAccountType(request.getAccountType());
        account.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        account.setStatus(Account.AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);

        // ── Phase 3B: Trigger referral reward on first account open ──────────
        referralService.onFirstAccountOpened(user, saved);

        return AccountResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get My Accounts
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<AccountResponse> getMyAccounts(String userId) {
        User user = userService.findById(userId);
        return accountRepository.findByUser(user)
                .stream().map(AccountResponse::from).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Account By Number
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public AccountResponse getAccount(String userId, String accountNumber) {
        Account account = findAndVerifyOwnership(userId, accountNumber);
        return AccountResponse.from(account);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deposit
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse deposit(String userId, String accountNumber,
                                       DepositRequest request) {
        Account account = findAndVerifyOwnership(userId, accountNumber);

        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active. Status: " + account.getStatus());

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw SwiftVaultException.badRequest("Deposit amount must be greater than zero");

        // ── Update balance ────────────────────────────────────────────────────
        account.setBalance(account.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        // ── Record transaction ────────────────────────────────────────────────
        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .toAccount(accountNumber)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(request.getDescription() != null
                        ? request.getDescription() : "Deposit")
                .build();
        transactionRepository.save(txn);

        return TransactionResponse.from(txn, account.getBalance());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Withdraw
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse withdraw(String userId, String accountNumber,
                                        WithdrawRequest request) {
        // Pessimistic lock prevents race conditions on the same account
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber,
                        LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));

        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active. Status: " + account.getStatus());

        User user = userService.findById(userId);
        verifyTransactionPin(user, request.getTransactionPin());

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw SwiftVaultException.badRequest("Withdrawal amount must be greater than zero");
        if (account.getBalance().compareTo(amount) < 0)
            throw SwiftVaultException.badRequest("Insufficient balance. Available: ₹" +
                    account.getBalance().toPlainString());

        // ── Phase 3B: Enforce transaction limits BEFORE any balance change ────
        transactionLimitService.enforceLimit(accountNumber, amount);

        // ── Update balance ────────────────────────────────────────────────────
        account.setBalance(account.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        // ── Record transaction ────────────────────────────────────────────────
        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(accountNumber)
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(amount)
                .description(request.getDescription() != null
                        ? request.getDescription() : "Withdrawal")
                .build();
        transactionRepository.save(txn);

        // ── Phase 3B: Post-transaction hooks (all silent — never throw) ───────
        runPostDebitHooks(user, account, amount, Transaction.TransactionType.WITHDRAW);

        return TransactionResponse.from(txn, account.getBalance());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transfer
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse transfer(String userId, TransferRequest request) {
        String fromAccountNumber = request.getFromAccount();
        String toAccountNumber   = request.getToAccount();

        if (fromAccountNumber.equals(toAccountNumber))
            throw SwiftVaultException.badRequest("Cannot transfer to the same account");

        // Pessimistic lock on both accounts — always lock in consistent order
        // (lower account number first) to prevent deadlocks
        String first  = fromAccountNumber.compareTo(toAccountNumber) < 0
                ? fromAccountNumber : toAccountNumber;
        String second = first.equals(fromAccountNumber) ? toAccountNumber : fromAccountNumber;

        Account firstLocked = accountRepository.findByAccountNumberWithLock(first,
                        LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + first));
        Account secondLocked = accountRepository.findByAccountNumberWithLock(second,
                        LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + second));

        Account fromAccount = first.equals(fromAccountNumber) ? firstLocked : secondLocked;
        Account toAccount   = first.equals(fromAccountNumber) ? secondLocked : firstLocked;

        if (!fromAccount.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own the source account");
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Source account is not active. Status: " + fromAccount.getStatus());
        if (toAccount.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Destination account is not active");

        User user = userService.findById(userId);
        verifyTransactionPin(user, request.getTransactionPin());

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw SwiftVaultException.badRequest("Transfer amount must be greater than zero");
        if (fromAccount.getBalance().compareTo(amount) < 0)
            throw SwiftVaultException.badRequest("Insufficient balance. Available: ₹" +
                    fromAccount.getBalance().toPlainString());

        // ── Phase 3B: Enforce transaction limits BEFORE any balance change ────
        transactionLimitService.enforceLimit(fromAccountNumber, amount);

        // ── Update balances ───────────────────────────────────────────────────
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP));
        toAccount.setBalance(toAccount.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // ── Record transaction ────────────────────────────────────────────────
        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(fromAccountNumber)
                .toAccount(toAccountNumber)
                .type(Transaction.TransactionType.TRANSFER)
                .amount(amount)
                .description(request.getDescription() != null
                        ? request.getDescription() : "Transfer")
                .build();
        transactionRepository.save(txn);

        // ── Phase 3B: Post-transaction hooks (all silent — never throw) ───────
        runPostDebitHooks(user, fromAccount, amount, Transaction.TransactionType.TRANSFER);

        return TransactionResponse.from(txn, fromAccount.getBalance());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Transaction History
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<TransactionResponse> getTransactionHistory(String userId, String accountNumber) {
        findAndVerifyOwnership(userId, accountNumber); // ownership check
        return transactionRepository.findByAccountNumber(accountNumber)
                .stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .map(t -> TransactionResponse.from(t, null))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin — Freeze / Unfreeze
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AccountResponse freezeAccount(String accountNumber, String reason) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (account.getStatus() == Account.AccountStatus.FROZEN)
            throw SwiftVaultException.badRequest("Account is already frozen");
        account.setStatus(Account.AccountStatus.FROZEN);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse unfreezeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (account.getStatus() != Account.AccountStatus.FROZEN)
            throw SwiftVaultException.badRequest("Account is not frozen");
        account.setStatus(Account.AccountStatus.ACTIVE);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .stream().map(AccountResponse::from).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Account findAndVerifyOwnership(String userId, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        return account;
    }

    private void verifyTransactionPin(User user, String pin) {
        if (!user.hasTransactionPin())
            throw SwiftVaultException.badRequest(
                    "Transaction PIN not set. Please set a PIN before making transactions.");
        if (pin == null || pin.isBlank())
            throw SwiftVaultException.badRequest("Transaction PIN is required");
        if (!passwordEncoder.matches(pin, user.getPinHash()))
            throw SwiftVaultException.unauthorized("Incorrect transaction PIN");
    }

    /**
     * Runs after every successful debit (withdraw or transfer).
     * All three hooks are wrapped individually — one failure never breaks others.
     * This method itself never throws.
     */
    private void runPostDebitHooks(User user, Account account,
                                   BigDecimal amount, Transaction.TransactionType type) {
        // 1. Fraud detection — risk-scores the transaction and creates alerts
        try {
            fraudDetectionService.analyzeTransaction(user, account, amount, type);
        } catch (Exception e) {
            // logged inside FraudDetectionService — never propagate
        }

        // 2. Low balance alert — fires if balance < user's configured threshold
        try {
            lowBalanceAlertService.checkAndAlert(account);
        } catch (Exception e) {
            // silent
        }

        // 3. Round-up auto savings — sweeps round-up difference to linked goal
        try {
            autoSavingsService.applyRoundUp(account.getAccountNumber(), amount);
        } catch (Exception e) {
            // silent
        }
    }
}