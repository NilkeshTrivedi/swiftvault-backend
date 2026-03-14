package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.*;
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
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository         accountRepository;
    private final TransactionRepository     transactionRepository;
    private final UserService               userService;
    private final PasswordEncoder           passwordEncoder;
    private final FraudDetectionService     fraudDetectionService;
    private final TransactionLimitService   transactionLimitService;
    private final AutoSavingsService        autoSavingsService;
    private final LowBalanceAlertService    lowBalanceAlertService;
    private final ReferralService           referralService;

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

    // ── Open Account ──────────────────────────────────────────────────────────

    @Override
    public AccountResponse openAccount(String userId, OpenAccountRequest request) {
        User user = userService.findById(userId);

        if (request.getType() == null)
            throw SwiftVaultException.badRequest("Account type is required");

        // Validate minimum initial deposit for SAVINGS
        BigDecimal initialDeposit = request.getInitialDeposit() != null
                ? request.getInitialDeposit()
                : BigDecimal.ZERO;

        if (request.getType() == Account.AccountType.SAVINGS &&
                initialDeposit.compareTo(Account.MINIMUM_BALANCE_SAVINGS) < 0)
            throw SwiftVaultException.badRequest(
                    "SAVINGS account requires minimum deposit of ₹" + Account.MINIMUM_BALANCE_SAVINGS);

        // Generate unique account number
        String accountNumber;
        do {
            accountNumber = IdGenerator.accountNumber();
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        // FIX: use initialDeposit as starting balance (not ZERO)
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUser(user);
        account.setType(request.getType());
        account.setBalance(initialDeposit.setScale(2, RoundingMode.HALF_UP));
        account.setStatus(Account.AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);

        // Record initial deposit transaction if amount > 0
        if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            transactionRepository.save(Transaction.builder()
                    .transactionId(IdGenerator.transactionId())
                    .fromAccount(accountNumber)
                    .toAccount(accountNumber)
                    .type(Transaction.TransactionType.DEPOSIT)
                    .amount(initialDeposit.setScale(2, RoundingMode.HALF_UP))
                    .description("Initial deposit")
                    .build());
        }

        // Phase 3B: referral reward on first account open (non-fatal)
        try { referralService.onFirstAccountOpened(user, saved); } catch (Exception ignored) {}

        return AccountResponse.from(saved);
    }

    // ── Get My Accounts ───────────────────────────────────────────────────────

    @Override
    public List<AccountResponse> getMyAccounts(String userId) {
        User user = userService.findById(userId);
        return accountRepository.findByUser(user)
                .stream().map(AccountResponse::from).toList();
    }

    // ── Get Account ───────────────────────────────────────────────────────────

    @Override
    public AccountResponse getAccount(String userId, String accountNumber) {
        return AccountResponse.from(findAndVerifyOwnership(userId, accountNumber));
    }

    // ── Set Nickname ──────────────────────────────────────────────────────────

    @Override
    public void setNickname(String userId, String accountNumber, String nickname) {
        Account account = findAndVerifyOwnership(userId, accountNumber);
        account.setNickname(nickname);
        accountRepository.save(account);
    }

    // ── Deposit ───────────────────────────────────────────────────────────────
    // Interface: deposit(String userId, String accountNumber, DepositRequest request)

    @Override
    public TransactionResponse deposit(String userId, String accountNumber, DepositRequest request) {
        Account account = findAndVerifyOwnership(userId, accountNumber);

        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active. Status: " + account.getStatus());

        BigDecimal amount = request.getAmount();
        account.setBalance(account.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(accountNumber)
                .toAccount(accountNumber)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(request.getDescription() != null ? request.getDescription() : "Deposit")
                .build();
        transactionRepository.save(txn);

        return TransactionResponse.from(txn);
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────
    // Interface: withdraw(String userId, String accountNumber, WithdrawRequest request)

    @Override
    public TransactionResponse withdraw(String userId, String accountNumber, WithdrawRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));

        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active. Status: " + account.getStatus());

        User user = userService.findById(userId);
        verifyPin(user, request.getTransactionPin());

        BigDecimal amount = request.getAmount();
        if (account.getBalance().compareTo(amount) < 0)
            throw SwiftVaultException.badRequest(
                    "Insufficient balance. Available: ₹" + account.getBalance().toPlainString());

        // Phase 3B: enforce transaction limits
        try { transactionLimitService.enforceLimit(accountNumber, amount); } catch (SwiftVaultException e) { throw e; } catch (Exception ignored) {}

        // SAVINGS minimum balance check
        if (account.getType() == Account.AccountType.SAVINGS) {
            BigDecimal afterBalance = account.getBalance().subtract(amount);
            if (afterBalance.compareTo(Account.MINIMUM_BALANCE_SAVINGS) < 0)
                throw SwiftVaultException.badRequest(
                        "SAVINGS accounts must maintain minimum balance of ₹" + Account.MINIMUM_BALANCE_SAVINGS);
        }

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

        // Post-debit hooks (all non-fatal)
        runPostDebitHooks(user, account, amount, Transaction.TransactionType.WITHDRAW);

        return TransactionResponse.from(txn);
    }

    // ── Transfer ──────────────────────────────────────────────────────────────
    // Interface: transfer(String userId, TransferRequest request)

    @Override
    public TransactionResponse transfer(String userId, TransferRequest request) {
        String fromNum = request.getFromAccount();
        String toNum   = request.getToAccount();

        if (fromNum == null || fromNum.isBlank())
            throw SwiftVaultException.badRequest("fromAccount is required");
        if (toNum == null || toNum.isBlank())
            throw SwiftVaultException.badRequest("toAccount is required");
        if (fromNum.equals(toNum))
            throw SwiftVaultException.badRequest("Cannot transfer to the same account");

        Account from = accountRepository.findByAccountNumber(fromNum)
                .orElseThrow(() -> SwiftVaultException.notFound("Source account not found: " + fromNum));
        Account to   = accountRepository.findByAccountNumber(toNum)
                .orElseThrow(() -> SwiftVaultException.notFound("Destination account not found: " + toNum));

        if (!from.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own the source account");
        if (from.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Source account is not active");
        if (to.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Destination account is not active");

        User user = userService.findById(userId);
        verifyPin(user, request.getTransactionPin());

        BigDecimal amount = request.getAmount();
        if (from.getBalance().compareTo(amount) < 0)
            throw SwiftVaultException.badRequest(
                    "Insufficient balance. Available: ₹" + from.getBalance().toPlainString());

        // Phase 3B: enforce limits
        try { transactionLimitService.enforceLimit(fromNum, amount); } catch (SwiftVaultException e) { throw e; } catch (Exception ignored) {}

        from.setBalance(from.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP));
        to.setBalance(to.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(from);
        accountRepository.save(to);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(fromNum)
                .toAccount(toNum)
                .type(Transaction.TransactionType.TRANSFER)
                .amount(amount)
                .description(request.getDescription() != null ? request.getDescription() : "Transfer")
                .build();
        transactionRepository.save(txn);

        runPostDebitHooks(user, from, amount, Transaction.TransactionType.TRANSFER);

        return TransactionResponse.from(txn);
    }

    // ── Transaction History ───────────────────────────────────────────────────

    @Override
    public List<TransactionResponse> getTransactionHistory(String userId, String accountNumber) {
        findAndVerifyOwnership(userId, accountNumber);
        return transactionRepository.findByAccountNumber(accountNumber)
                .stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .map(TransactionResponse::from).toList();
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
        Transaction.TransactionType txnType;
        try {
            txnType = Transaction.TransactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw SwiftVaultException.badRequest("Invalid transaction type: " + type);
        }
        return transactionRepository.findByAccountNumberAndType(accountNumber, txnType)
                .stream().map(TransactionResponse::from).toList();
    }

    // ── Admin: Freeze / Unfreeze / Close ──────────────────────────────────────
    // Interface: freezeAccount(String accountNumber, String reason) → AccountResponse
    // Interface: unfreezeAccount(String accountNumber)              → AccountResponse

    @Override
    public AccountResponse freezeAccount(String accountNumber, String reason) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        account.setStatus(Account.AccountStatus.FROZEN);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Override
    public AccountResponse unfreezeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        account.setStatus(Account.AccountStatus.ACTIVE);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Override
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

    // ── Admin: Operations ─────────────────────────────────────────────────────

    @Override
    public void applyMonthlyInterest() {
        List<Account> savings = accountRepository
                .findByTypeAndStatus(Account.AccountType.SAVINGS, Account.AccountStatus.ACTIVE);
        for (Account a : savings) {
            BigDecimal interest = a.getBalance()
                    .multiply(Account.SAVINGS_INTEREST_RATE)
                    .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            if (interest.compareTo(BigDecimal.ZERO) > 0) {
                a.setBalance(a.getBalance().add(interest));
                a.setLastInterestApplied(java.time.LocalDateTime.now());
                accountRepository.save(a);
            }
        }
    }

    @Override
    public void applyLowBalanceFees() {
        List<Account> savings = accountRepository
                .findByTypeAndStatus(Account.AccountType.SAVINGS, Account.AccountStatus.ACTIVE);
        for (Account a : savings) {
            if (a.getBalance().compareTo(Account.MINIMUM_BALANCE_SAVINGS) < 0) {
                BigDecimal newBal = a.getBalance().subtract(Account.LOW_BALANCE_FEE).max(BigDecimal.ZERO);
                a.setBalance(newBal);
                accountRepository.save(a);
            }
        }
    }

    @Override
    public DashboardResponse getDashboard() {
        return DashboardResponse.builder()
                .totalAccounts(accountRepository.count())
                .activeAccounts(accountRepository.countByStatus(Account.AccountStatus.ACTIVE))
                .totalBankBalance(accountRepository.getTotalBankBalanceByStatus(Account.AccountStatus.ACTIVE))
                .build();
    }

    @Override
    public void exportTransactionsCsv(String filePath) {
        try {
            java.io.File dir = new java.io.File(filePath).getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            List<Transaction> all = transactionRepository.findAll();
            StringBuilder sb = new StringBuilder(
                    "transactionId,fromAccount,toAccount,type,amount,description,timestamp\n");
            for (Transaction t : all) {
                sb.append(String.join(",",
                        t.getTransactionId(),
                        t.getFromAccount() != null ? t.getFromAccount() : "",
                        t.getToAccount()   != null ? t.getToAccount()   : "",
                        t.getType().name(),
                        t.getAmount().toPlainString(),
                        t.getDescription() != null
                                ? "\"" + t.getDescription().replace("\"", "'") + "\""
                                : "",
                        t.getTimestamp().toString()
                )).append("\n");
            }
            java.nio.file.Files.writeString(java.nio.file.Path.of(filePath), sb.toString());
        } catch (Exception e) {
            throw SwiftVaultException.internal("Failed to export CSV: " + e.getMessage());
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Account findAndVerifyOwnership(String userId, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        return account;
    }

    private void verifyPin(User user, String pin) {
        if (!user.hasTransactionPin())
            throw SwiftVaultException.badRequest(
                    "Transaction PIN not set. Please set one in your profile.");
        if (pin == null || pin.isBlank())
            throw SwiftVaultException.badRequest("Transaction PIN is required");
        if (!passwordEncoder.matches(pin, user.getPinHash()))
            throw SwiftVaultException.unauthorized("Incorrect transaction PIN");
    }

    private void runPostDebitHooks(User user, Account account,
                                   BigDecimal amount, Transaction.TransactionType type) {
        try { fraudDetectionService.analyzeTransaction(user, account, amount, type); }
        catch (Exception ignored) {}
        try { lowBalanceAlertService.checkAndAlert(account); }
        catch (Exception ignored) {}
        try { autoSavingsService.applyRoundUp(account.getAccountNumber(), amount); }
        catch (Exception ignored) {}
    }
}