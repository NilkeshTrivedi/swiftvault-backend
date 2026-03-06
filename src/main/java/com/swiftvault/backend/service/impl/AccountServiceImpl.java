package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.DepositRequest;
import com.swiftvault.backend.dto.request.OpenAccountRequest;
import com.swiftvault.backend.dto.request.TransferRequest;
import com.swiftvault.backend.dto.request.WithdrawRequest;
import com.swiftvault.backend.dto.response.AccountResponse;
import com.swiftvault.backend.dto.response.DashboardResponse;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository       accountRepository;
    private final TransactionRepository   transactionRepository;
    private final UserService             userService;
    private final PasswordEncoder         passwordEncoder;

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
        this.accountRepository       = accountRepository;
        this.transactionRepository   = transactionRepository;
        this.userService             = userService;
        this.passwordEncoder         = passwordEncoder;
        this.fraudDetectionService   = fraudDetectionService;
        this.transactionLimitService = transactionLimitService;
        this.autoSavingsService      = autoSavingsService;
        this.lowBalanceAlertService  = lowBalanceAlertService;
        this.referralService         = referralService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Open Account
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AccountResponse openAccount(String userId, OpenAccountRequest request) {
        User user = userService.findById(userId);

        // FIX: request.getType() — not getAccountType()
        if (request.getType() == null)
            throw SwiftVaultException.badRequest("Account type is required");

        // FIX: accountRepository has no existsByAccountNumber() —
        //      use findByAccountNumber().isPresent() instead
        String accountNumber;
        do {
            accountNumber = IdGenerator.accountNumber();
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUser(user);
        // FIX: account.setType() — not setAccountType()
        account.setType(request.getType());
        account.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        account.setStatus(Account.AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);

        // Phase 3B: Referral reward on first account open
        try {
            referralService.onFirstAccountOpened(user, saved);
        } catch (Exception e) {
            // Never let referral logic break account creation
        }

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
    // Get Account
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public AccountResponse getAccount(String userId, String accountNumber) {
        return AccountResponse.from(findAndVerifyOwnership(userId, accountNumber));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Set Nickname
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void setNickname(String userId, String accountNumber, String nickname) {
        Account account = findAndVerifyOwnership(userId, accountNumber);
        account.setNickname(nickname);
        accountRepository.save(account);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deposit
    // FIX: signature is (userId, accountNumber, request) to match controller
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse deposit(String userId, String accountNumber, DepositRequest request) {
        Account account = findAndVerifyOwnership(userId, accountNumber);

        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active. Status: " + account.getStatus());

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw SwiftVaultException.badRequest("Deposit amount must be greater than zero");

        account.setBalance(account.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(accountNumber)   // fromAccount = depositing account
                .toAccount(accountNumber)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(request.getDescription() != null ? request.getDescription() : "Deposit")
                .build();
        transactionRepository.save(txn);

        // FIX: TransactionResponse.from() takes only Transaction — no second arg
        return TransactionResponse.from(txn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Withdraw
    // FIX: signature is (userId, accountNumber, request)
    // FIX: no findByAccountNumberWithLock — use findByAccountNumber + optimistic approach
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse withdraw(String userId, String accountNumber, WithdrawRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
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

        // Phase 3B: enforce transaction limits before balance change
        transactionLimitService.enforceLimit(accountNumber, amount);

        account.setBalance(account.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(accountNumber)
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(amount)
                .description(request.getDescription() != null ? request.getDescription() : "Withdrawal")
                .build();
        transactionRepository.save(txn);

        // Phase 3B: post-debit hooks (all silent)
        runPostDebitHooks(user, account, amount, Transaction.TransactionType.WITHDRAW);

        // FIX: TransactionResponse.from() — one arg
        return TransactionResponse.from(txn);
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

        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + fromAccountNumber));
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + toAccountNumber));

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

        // Phase 3B: enforce limits before balance change
        transactionLimitService.enforceLimit(fromAccountNumber, amount);

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP));
        toAccount.setBalance(toAccount.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(fromAccountNumber)
                .toAccount(toAccountNumber)
                .type(Transaction.TransactionType.TRANSFER)
                .amount(amount)
                .description(request.getDescription() != null ? request.getDescription() : "Transfer")
                .build();
        transactionRepository.save(txn);

        runPostDebitHooks(user, fromAccount, amount, Transaction.TransactionType.TRANSFER);

        // FIX: TransactionResponse.from() — one arg
        return TransactionResponse.from(txn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transaction History
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<TransactionResponse> getTransactionHistory(String userId, String accountNumber) {
        findAndVerifyOwnership(userId, accountNumber);
        return transactionRepository.findByAccountNumber(accountNumber)
                .stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                // FIX: TransactionResponse.from() — one arg
                .map(TransactionResponse::from)
                .toList();
    }

    @Override
    public List<TransactionResponse> getMiniStatement(String userId, String accountNumber) {
        findAndVerifyOwnership(userId, accountNumber);
        return transactionRepository.findTop5ByAccountNumber(accountNumber)
                .stream().map(TransactionResponse::from).toList();
    }

    @Override
    public List<TransactionResponse> getFilteredHistory(String userId, String accountNumber, String type) {
        findAndVerifyOwnership(userId, accountNumber);
        Transaction.TransactionType txnType = Transaction.TransactionType.valueOf(type.toUpperCase());
        return transactionRepository.findByAccountNumberAndType(accountNumber, txnType)
                .stream().map(TransactionResponse::from).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin — Freeze / Unfreeze / Close
    // FIX: freezeAccount(accountNumber, reason) and unfreezeAccount(accountNumber) return AccountResponse
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
    @Transactional
    public void closeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0)
            throw SwiftVaultException.badRequest("Cannot close account with non-zero balance");
        account.setStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin — Operations
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void applyMonthlyInterest() {
        List<Account> savingsAccounts = accountRepository
                .findByTypeAndStatus(Account.AccountType.SAVINGS, Account.AccountStatus.ACTIVE);
        for (Account account : savingsAccounts) {
            BigDecimal interest = account.getBalance()
                    .multiply(Account.SAVINGS_INTEREST_RATE)
                    .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            if (interest.compareTo(BigDecimal.ZERO) > 0) {
                account.setBalance(account.getBalance().add(interest));
                account.setLastInterestApplied(java.time.LocalDateTime.now());
                accountRepository.save(account);
            }
        }
    }

    @Override
    @Transactional
    public void applyLowBalanceFees() {
        List<Account> savingsAccounts = accountRepository
                .findByTypeAndStatus(Account.AccountType.SAVINGS, Account.AccountStatus.ACTIVE);
        for (Account account : savingsAccounts) {
            if (account.getBalance().compareTo(Account.MINIMUM_BALANCE_SAVINGS) < 0) {
                BigDecimal newBalance = account.getBalance().subtract(Account.LOW_BALANCE_FEE);
                account.setBalance(newBalance.max(BigDecimal.ZERO));
                accountRepository.save(account);
            }
        }
    }

    @Override
    public DashboardResponse getDashboard() {
        return DashboardResponse.builder()
                .totalUsers(0)  // Users counted in UserService
                .totalAccounts(accountRepository.count())
                .activeAccounts(accountRepository.countByStatus(Account.AccountStatus.ACTIVE))
                .totalBankBalance(accountRepository.getTotalBankBalanceByStatus(Account.AccountStatus.ACTIVE))
                .build();
    }

    @Override
    public void exportTransactionsCsv(String filePath) {
        // CSV export — writes all transactions to the given file path
        try {
            java.io.File dir = new java.io.File(filePath).getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();

            List<Transaction> all = transactionRepository.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("transactionId,fromAccount,toAccount,type,amount,description,timestamp\n");
            for (Transaction t : all) {
                sb.append(String.join(",",
                        t.getTransactionId(),
                        t.getFromAccount() != null ? t.getFromAccount() : "",
                        t.getToAccount()   != null ? t.getToAccount()   : "",
                        t.getType().name(),
                        t.getAmount().toPlainString(),
                        t.getDescription() != null ? "\"" + t.getDescription().replace("\"", "'") + "\"" : "",
                        t.getTimestamp().toString()
                )).append("\n");
            }
            java.nio.file.Files.writeString(java.nio.file.Path.of(filePath), sb.toString());
        } catch (Exception e) {
            throw SwiftVaultException.internal("Failed to export CSV: " + e.getMessage());
        }
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

    private void runPostDebitHooks(User user, Account account,
                                   BigDecimal amount, Transaction.TransactionType type) {
        try {
            fraudDetectionService.analyzeTransaction(user, account, amount, type);
        } catch (Exception ignored) {}

        try {
            lowBalanceAlertService.checkAndAlert(account);
        } catch (Exception ignored) {}

        try {
            autoSavingsService.applyRoundUp(account.getAccountNumber(), amount);
        } catch (Exception ignored) {}
    }
}