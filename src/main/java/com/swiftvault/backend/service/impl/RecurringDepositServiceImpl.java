package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.OpenRdRequest;
import com.swiftvault.backend.dto.response.RdResponse;
import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.*;
import com.swiftvault.backend.service.RecurringDepositService;
import com.swiftvault.backend.service.UserService;
import com.swiftvault.backend.util.IdGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Service
public class RecurringDepositServiceImpl implements RecurringDepositService {

    private static final Logger log = Logger.getLogger(RecurringDepositServiceImpl.class.getName());

    private final RecurringDepositRepository rdRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public RecurringDepositServiceImpl(RecurringDepositRepository rdRepository,
                                       AccountRepository accountRepository,
                                       TransactionRepository transactionRepository,
                                       UserService userService,
                                       PasswordEncoder passwordEncoder) {
        this.rdRepository = rdRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public RdResponse openRd(String userId, OpenRdRequest request) {
        User user = userService.findById(userId);
        Account account = accountRepository.findById(request.getAccountNumber())
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found"));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active");
        if (request.getMonthlyInstallment().compareTo(RecurringDeposit.MIN_INSTALLMENT) < 0)
            throw SwiftVaultException.badRequest("Minimum installment is ₹500");
        if (account.getBalance().compareTo(request.getMonthlyInstallment()) < 0)
            throw SwiftVaultException.badRequest("Insufficient balance for first installment");

        BigDecimal maturityAmount = RecurringDeposit.calculateMaturityAmount(
                request.getMonthlyInstallment(), request.getTenureMonths());
        LocalDate startDate    = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());
        LocalDate nextDueDate  = startDate.plusMonths(1);

        account.setBalance(account.getBalance().subtract(request.getMonthlyInstallment()).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(account.getAccountNumber())
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(request.getMonthlyInstallment())
                .description("RD installment #1")
                .build();
        transactionRepository.save(txn);

        RecurringDeposit rd = RecurringDeposit.builder()
                .rdId(IdGenerator.rdId())
                .user(user)
                .sourceAccount(account)
                .monthlyInstallment(request.getMonthlyInstallment())
                .tenureMonths(request.getTenureMonths())
                .interestRate(RecurringDeposit.INTEREST_RATE)
                .maturityAmount(maturityAmount)
                .startDate(startDate)
                .maturityDate(maturityDate)
                .nextDueDate(nextDueDate)
                .nomineeName(request.getNomineeName())
                .build();
        rd.setInstallmentsPaid(1);
        rd.setTotalDeposited(request.getMonthlyInstallment());
        rdRepository.save(rd);
        log.info("RD opened: " + rd.getRdId() + " for user: " + userId);
        return RdResponse.from(rd);
    }

    @Override
    public List<RdResponse> getMyRds(String userId) {
        User user = userService.findById(userId);
        return rdRepository.findByUser(user).stream().map(RdResponse::from).toList();
    }

    @Override
    public RdResponse getRd(String userId, String rdId) {
        RecurringDeposit rd = rdRepository.findById(rdId)
                .orElseThrow(() -> SwiftVaultException.notFound("RD not found"));
        if (!rd.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        return RdResponse.from(rd);
    }

    @Override
    @Transactional
    public RdResponse payInstallment(String userId, String rdId, String transactionPin) {
        RecurringDeposit rd = rdRepository.findById(rdId)
                .orElseThrow(() -> SwiftVaultException.notFound("RD not found"));
        if (!rd.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        if (rd.getStatus() != RecurringDeposit.RdStatus.ACTIVE)
            throw SwiftVaultException.badRequest("RD is not active");
        if (rd.getInstallmentsPaid() >= rd.getTenureMonths())
            throw SwiftVaultException.badRequest("All installments already paid");

        User user = userService.findById(userId);
        if (!passwordEncoder.matches(transactionPin, user.getPinHash()))
            throw SwiftVaultException.badRequest("Invalid transaction PIN");

        Account account = rd.getSourceAccount();
        if (account.getBalance().compareTo(rd.getMonthlyInstallment()) < 0)
            throw SwiftVaultException.badRequest("Insufficient balance");

        account.setBalance(account.getBalance().subtract(rd.getMonthlyInstallment()).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        int installmentNo = rd.getInstallmentsPaid() + 1;
        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(account.getAccountNumber())
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(rd.getMonthlyInstallment())
                .description("RD installment #" + installmentNo)
                .build();
        transactionRepository.save(txn);

        rd.setInstallmentsPaid(installmentNo);
        rd.setTotalDeposited(rd.getTotalDeposited().add(rd.getMonthlyInstallment()));
        rd.setNextDueDate(rd.getNextDueDate().plusMonths(1));

        if (rd.getInstallmentsPaid() >= rd.getTenureMonths()) {
            rd.setStatus(RecurringDeposit.RdStatus.MATURED);
            account.setBalance(account.getBalance().add(rd.getMaturityAmount()).setScale(2, RoundingMode.HALF_UP));
            accountRepository.save(account);
            Transaction matTxn = Transaction.builder()
                    .transactionId(IdGenerator.transactionId())
                    .toAccount(account.getAccountNumber())
                    .type(Transaction.TransactionType.DEPOSIT)
                    .amount(rd.getMaturityAmount())
                    .description("RD maturity credit")
                    .build();
            transactionRepository.save(matTxn);
            rd.setClosedAt(LocalDateTime.now());
        }
        rdRepository.save(rd);
        return RdResponse.from(rd);
    }

    @Override
    @Transactional
    public RdResponse closeRd(String userId, String rdId, String transactionPin) {
        RecurringDeposit rd = rdRepository.findById(rdId)
                .orElseThrow(() -> SwiftVaultException.notFound("RD not found"));
        if (!rd.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        if (rd.getStatus() != RecurringDeposit.RdStatus.ACTIVE)
            throw SwiftVaultException.badRequest("RD is not active");

        User user = userService.findById(userId);
        if (!passwordEncoder.matches(transactionPin, user.getPinHash()))
            throw SwiftVaultException.badRequest("Invalid transaction PIN");

        Account account = rd.getSourceAccount();
        account.setBalance(account.getBalance().add(rd.getTotalDeposited()).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .toAccount(account.getAccountNumber())
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(rd.getTotalDeposited())
                .description("RD premature closure - principal returned")
                .build();
        transactionRepository.save(txn);

        rd.setStatus(RecurringDeposit.RdStatus.CLOSED);
        rd.setClosedAt(LocalDateTime.now());
        rdRepository.save(rd);
        return RdResponse.from(rd);
    }

    @Override
    @Transactional
    public void processAutoDebitRds() {
        List<RecurringDeposit> dueRds = rdRepository.findDueRds(LocalDate.now());
        for (RecurringDeposit rd : dueRds) {
            try {
                Account account = rd.getSourceAccount();
                if (account.getBalance().compareTo(rd.getMonthlyInstallment()) >= 0) {
                    account.setBalance(account.getBalance().subtract(rd.getMonthlyInstallment()).setScale(2, RoundingMode.HALF_UP));
                    accountRepository.save(account);
                    rd.setInstallmentsPaid(rd.getInstallmentsPaid() + 1);
                    rd.setTotalDeposited(rd.getTotalDeposited().add(rd.getMonthlyInstallment()));
                    rd.setNextDueDate(rd.getNextDueDate().plusMonths(1));
                    log.info("Auto-debited RD installment: " + rd.getRdId());
                } else {
                    rd.setInstallmentsMissed(rd.getInstallmentsMissed() + 1);
                    rd.setNextDueDate(rd.getNextDueDate().plusMonths(1));
                    log.warning("Missed RD installment for: " + rd.getRdId());
                }
                if (rd.getInstallmentsPaid() >= rd.getTenureMonths()) {
                    rd.setStatus(RecurringDeposit.RdStatus.MATURED);
                    account.setBalance(account.getBalance().add(rd.getMaturityAmount()).setScale(2, RoundingMode.HALF_UP));
                    accountRepository.save(account);
                    rd.setClosedAt(LocalDateTime.now());
                }
                rdRepository.save(rd);
            } catch (Exception e) {
                log.warning("Failed auto-debit for RD: " + rd.getRdId());
            }
        }
    }
}