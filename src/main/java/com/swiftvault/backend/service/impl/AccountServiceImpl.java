package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.*;
import com.swiftvault.backend.dto.response.AccountResponse;
import com.swiftvault.backend.dto.response.DashboardResponse;
import com.swiftvault.backend.dto.response.TransactionResponse;
import com.swiftvault.backend.entity.Account;
import com.swiftvault.backend.entity.Transaction;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.AccountRepository;
import com.swiftvault.backend.repository.TransactionRepository;
import com.swiftvault.backend.repository.UserRepository;
import com.swiftvault.backend.service.AccountService;
import com.swiftvault.backend.service.UserService;
import com.swiftvault.backend.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = Logger.getLogger(AccountServiceImpl.class.getName());

    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository        userRepository;
    private final UserService           userService;

    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              @Lazy UserService userService) {
        this.accountRepository     = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository        = userRepository;
        this.userService           = userService;
    }

    @Override
    @Transactional
    public AccountResponse openAccount(String userId, OpenAccountRequest request) {
        User user = userService.findById(userId);
        if (request.getType() == Account.AccountType.SAVINGS &&
                request.getInitialDeposit().compareTo(Account.MINIMUM_BALANCE_SAVINGS) < 0)
            throw SwiftVaultException.badRequest("SAVINGS account requires minimum deposit of " + Account.MINIMUM_BALANCE_SAVINGS);

        Account account = Account.builder()
                .accountNumber(IdGenerator.accountNumber())
                .user(user).balance(request.getInitialDeposit())
                .type(request.getType()).status(Account.AccountStatus.ACTIVE).build();
        accountRepository.save(account);

        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0)
            recordTransaction(account.getAccountNumber(), null, Transaction.TransactionType.DEPOSIT,
                    request.getInitialDeposit(), "Initial deposit");

        log.info("Account opened: " + account.getAccountNumber() + " for user: " + userId);
        return AccountResponse.from(account);
    }

    @Override
    public List<AccountResponse> getMyAccounts(String userId) {
        User user = userService.findById(userId);
        return accountRepository.findByUser(user).stream().map(AccountResponse::from).collect(Collectors.toList());
    }

    @Override
    public AccountResponse getAccount(String userId, String accountNumber) {
        return AccountResponse.from(findAndVerifyOwnership(userId, accountNumber, "view"));
    }

    @Override
    @Transactional
    public void setNickname(String userId, String accountNumber, String nickname) {
        Account account = findAndVerifyOwnership(userId, accountNumber, "rename");
        account.setNickname(nickname);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public TransactionResponse deposit(String userId, DepositRequest request) {
        Account account = findActiveAndVerifyOwnership(userId, request.getAccountNumber(), "deposit into");
        account.deposit(request.getAmount());
        accountRepository.save(account);
        Transaction txn = recordTransaction(account.getAccountNumber(), null,
                Transaction.TransactionType.DEPOSIT, request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "Deposit");
        return TransactionResponse.from(txn);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(String userId, WithdrawRequest request) {
        userService.verifyTransactionPin(userId, request.getTransactionPin());
        Account account = findActiveAndVerifyOwnership(userId, request.getAccountNumber(), "withdraw from");
        handleWithdrawResult(account.withdrawWithChecks(request.getAmount()), account);
        accountRepository.save(account);
        Transaction txn = recordTransaction(account.getAccountNumber(), null,
                Transaction.TransactionType.WITHDRAW, request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "Withdrawal");
        return TransactionResponse.from(txn);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(String userId, TransferRequest request) {
        userService.verifyTransactionPin(userId, request.getTransactionPin());
        String toAccountNumber = resolveDestinationAccount(request);
        if (request.getFromAccount().equals(toAccountNumber))
            throw SwiftVaultException.badRequest("Cannot transfer to the same account.");

        Account from = findActiveAndVerifyOwnership(userId, request.getFromAccount(), "transfer from");
        Account to   = findActiveAccount(toAccountNumber);
        handleWithdrawResult(from.withdrawWithChecks(request.getAmount()), from);
        to.deposit(request.getAmount());
        accountRepository.save(from);
        accountRepository.save(to);
        Transaction txn = recordTransaction(from.getAccountNumber(), to.getAccountNumber(),
                Transaction.TransactionType.TRANSFER, request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "Transfer");
        return TransactionResponse.from(txn);
    }

    @Override
    public List<TransactionResponse> getTransactionHistory(String userId, String accountNumber) {
        findAndVerifyOwnership(userId, accountNumber, "view transactions of");
        return transactionRepository.findByAccountNumber(accountNumber).stream()
                .map(TransactionResponse::from).collect(Collectors.toList());
    }

    @Override
    public List<TransactionResponse> getMiniStatement(String userId, String accountNumber) {
        findAndVerifyOwnership(userId, accountNumber, "view transactions of");
        return transactionRepository.findTop5ByAccountNumber(accountNumber).stream()
                .map(TransactionResponse::from).collect(Collectors.toList());
    }

    @Override
    public List<TransactionResponse> getFilteredHistory(String userId, String accountNumber, String type) {
        findAndVerifyOwnership(userId, accountNumber, "view transactions of");
        Transaction.TransactionType txnType;
        try { txnType = Transaction.TransactionType.valueOf(type.toUpperCase()); }
        catch (IllegalArgumentException e) { throw SwiftVaultException.badRequest("Invalid type: " + type); }
        return transactionRepository.findByAccountNumberAndType(accountNumber, txnType).stream()
                .map(TransactionResponse::from).collect(Collectors.toList());
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream().map(AccountResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void freezeAccount(String accountNumber) {
        Account account = findActiveAccount(accountNumber);
        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void unfreezeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        account.setStatus(Account.AccountStatus.ACTIVE);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void closeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0)
            throw SwiftVaultException.badRequest("Cannot close account with balance. Withdraw first.");
        account.setStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void applyMonthlyInterest() {
        List<Account> accounts = accountRepository.findByTypeAndStatus(
                Account.AccountType.SAVINGS, Account.AccountStatus.ACTIVE);
        for (Account account : accounts) {
            BigDecimal interest = account.getBalance().multiply(Account.SAVINGS_INTEREST_RATE)
                    .divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
            account.deposit(interest);
            account.setLastInterestApplied(java.time.LocalDateTime.now());
            accountRepository.save(account);
            recordTransaction(account.getAccountNumber(), null,
                    Transaction.TransactionType.DEPOSIT, interest, "Monthly interest @ 4% p.a.");
        }
    }

    @Override
    @Transactional
    public void applyLowBalanceFees() {
        List<Account> accounts = accountRepository.findByTypeAndStatus(
                Account.AccountType.SAVINGS, Account.AccountStatus.ACTIVE);
        for (Account account : accounts) {
            if (account.getBalance().compareTo(Account.MINIMUM_BALANCE_SAVINGS) < 0) {
                account.withdraw(Account.LOW_BALANCE_FEE);
                accountRepository.save(account);
                recordTransaction(account.getAccountNumber(), null,
                        Transaction.TransactionType.WITHDRAW, Account.LOW_BALANCE_FEE, "Low balance fee");
            }
        }
    }

    @Override
    public DashboardResponse getDashboard() {
        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.findAll().stream()
                        .filter(u -> u.getStatus() == User.UserStatus.ACTIVE).count())
                .totalAccounts(accountRepository.count())
                .activeAccounts(accountRepository.countByStatus(Account.AccountStatus.ACTIVE))
                .totalTransactions(transactionRepository.count())
                .totalBankBalance(accountRepository.getTotalBankBalanceByStatus(Account.AccountStatus.ACTIVE))
                .build();
    }

    @Override
    public void exportTransactionsCsv(String filePath) {
        try {
            Files.createDirectories(Paths.get("exports"));
            List<Transaction> txns = transactionRepository.findAll();
            try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
                pw.println("Transaction ID,From Account,To Account,Type,Amount,Description,Timestamp");
                for (Transaction t : txns) {
                    pw.printf("%s,%s,%s,%s,%.2f,%s,%s%n",
                            t.getTransactionId(), t.getFromAccount(),
                            t.getToAccount() != null ? t.getToAccount() : "",
                            t.getType(), t.getAmount(),
                            t.getDescription() != null ? t.getDescription().replace(",", ";") : "",
                            t.getTimestamp());
                }
            }
        } catch (IOException e) {
            throw SwiftVaultException.badRequest("Failed to export CSV: " + e.getMessage());
        }
    }

    private Account findAndVerifyOwnership(String userId, String accountNumber, String action) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You do not have permission to " + action + " account " + accountNumber);
        return account;
    }

    private Account findActiveAndVerifyOwnership(String userId, String accountNumber, String action) {
        Account account = findAndVerifyOwnership(userId, accountNumber, action);
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account " + accountNumber + " is " + account.getStatus());
        return account;
    }

    private Account findActiveAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found: " + accountNumber));
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account " + accountNumber + " is " + account.getStatus());
        return account;
    }

    private void handleWithdrawResult(String result, Account account) {
        switch (result) {
            case "INSUFFICIENT" -> throw SwiftVaultException.badRequest("Insufficient balance. Available: " + account.getBalance());
            case "DAILY_LIMIT"  -> throw SwiftVaultException.badRequest("Daily limit reached. Remaining: " + account.getRemainingDailyLimit());
            case "MIN_BALANCE"  -> throw SwiftVaultException.badRequest("SAVINGS minimum balance: " + Account.MINIMUM_BALANCE_SAVINGS);
        }
    }

    private String resolveDestinationAccount(TransferRequest request) {
        if (request.getToAccount() != null && !request.getToAccount().isBlank()) return request.getToAccount();
        if (request.getToEmail() != null && !request.getToEmail().isBlank()) {
            User recipient = userRepository.findByEmail(request.getToEmail())
                    .orElseThrow(() -> SwiftVaultException.notFound("No user found with email: " + request.getToEmail()));
            List<Account> accounts = accountRepository.findByUserAndStatus(recipient, Account.AccountStatus.ACTIVE);
            if (accounts.isEmpty()) throw SwiftVaultException.badRequest("Recipient has no active accounts.");
            return accounts.get(0).getAccountNumber();
        }
        throw SwiftVaultException.badRequest("Provide either toAccount or toEmail.");
    }

    private Transaction recordTransaction(String from, String to,
                                          Transaction.TransactionType type, BigDecimal amount, String description) {
        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(from).toAccount(to)
                .type(type).amount(amount).description(description).build();
        return transactionRepository.save(txn);
    }
}