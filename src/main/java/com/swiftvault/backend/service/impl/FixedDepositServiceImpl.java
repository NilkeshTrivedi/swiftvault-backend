package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.FdWithdrawRequest;
import com.swiftvault.backend.dto.request.OpenFdRequest;
import com.swiftvault.backend.dto.response.FdResponse;
import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.*;
import com.swiftvault.backend.service.FixedDepositService;
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
public class FixedDepositServiceImpl implements FixedDepositService {

    private static final Logger log = Logger.getLogger(FixedDepositServiceImpl.class.getName());
    private static final BigDecimal MIN_FD_AMOUNT = new BigDecimal("1000.00");

    private final FixedDepositRepository fdRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public FixedDepositServiceImpl(FixedDepositRepository fdRepository,
                                   AccountRepository accountRepository,
                                   TransactionRepository transactionRepository,
                                   UserService userService,
                                   PasswordEncoder passwordEncoder) {
        this.fdRepository = fdRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public FdResponse openFd(String userId, OpenFdRequest request) {
        User user = userService.findById(userId);
        Account account = accountRepository.findById(request.getAccountNumber())
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found"));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active");
        if (request.getAmount().compareTo(MIN_FD_AMOUNT) < 0)
            throw SwiftVaultException.badRequest("Minimum FD amount is ₹1,000");
        if (account.getBalance().compareTo(request.getAmount()) < 0)
            throw SwiftVaultException.badRequest("Insufficient balance");

        BigDecimal rate        = FixedDeposit.getInterestRate(request.getTenureDays());
        BigDecimal maturity    = FixedDeposit.calculateMaturityAmount(request.getAmount(), rate, request.getTenureDays());
        LocalDate startDate    = LocalDate.now();
        LocalDate maturityDate = startDate.plusDays(request.getTenureDays());

        account.setBalance(account.getBalance().subtract(request.getAmount()).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(account.getAccountNumber())
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(request.getAmount())
                .description("Fixed Deposit opened")
                .build();
        transactionRepository.save(txn);

        FixedDeposit fd = FixedDeposit.builder()
                .fdId(IdGenerator.fdId())
                .user(user)
                .sourceAccount(account)
                .principalAmount(request.getAmount())
                .interestRate(rate)
                .tenureDays(request.getTenureDays())
                .maturityAmount(maturity)
                .startDate(startDate)
                .maturityDate(maturityDate)
                .payoutOption(request.getPayoutOption() != null ? request.getPayoutOption() : FixedDeposit.PayoutOption.ON_MATURITY)
                .nomineeName(request.getNomineeName())
                .build();
        fdRepository.save(fd);
        log.info("FD opened: " + fd.getFdId() + " for user: " + userId);
        return FdResponse.from(fd);
    }

    @Override
    public List<FdResponse> getMyFds(String userId) {
        User user = userService.findById(userId);
        return fdRepository.findByUser(user).stream().map(FdResponse::from).toList();
    }

    @Override
    public FdResponse getFd(String userId, String fdId) {
        FixedDeposit fd = fdRepository.findById(fdId)
                .orElseThrow(() -> SwiftVaultException.notFound("FD not found"));
        if (!fd.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        return FdResponse.from(fd);
    }

    @Override
    @Transactional
    public FdResponse withdrawFd(String userId, FdWithdrawRequest request) {
        FixedDeposit fd = fdRepository.findById(request.getFdId())
                .orElseThrow(() -> SwiftVaultException.notFound("FD not found"));
        if (!fd.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        if (fd.getStatus() != FixedDeposit.FdStatus.ACTIVE)
            throw SwiftVaultException.badRequest("FD is not active");

        User user = userService.findById(userId);
        if (!passwordEncoder.matches(request.getTransactionPin(), user.getPinHash()))
            throw SwiftVaultException.badRequest("Invalid transaction PIN");

        BigDecimal returnAmount;
        boolean premature = LocalDate.now().isBefore(fd.getMaturityDate());

        if (premature) {
            BigDecimal penalty = fd.getPrincipalAmount()
                    .multiply(FixedDeposit.PREMATURE_PENALTY)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            returnAmount = fd.getPrincipalAmount().subtract(penalty);
            fd.setPrematureWithdrawal(true);
            fd.setActualInterestEarned(BigDecimal.ZERO.subtract(penalty));
            fd.setStatus(FixedDeposit.FdStatus.PREMATURE_CLOSED);
        } else {
            returnAmount = fd.getMaturityAmount();
            fd.setActualInterestEarned(fd.getMaturityAmount().subtract(fd.getPrincipalAmount()));
            fd.setStatus(FixedDeposit.FdStatus.MATURED);
        }

        Account account = fd.getSourceAccount();
        account.setBalance(account.getBalance().add(returnAmount).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .toAccount(account.getAccountNumber())
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(returnAmount)
                .description(premature ? "FD premature withdrawal (1% penalty applied)" : "FD maturity credit")
                .build();
        transactionRepository.save(txn);

        fd.setClosedAt(LocalDateTime.now());
        fdRepository.save(fd);
        return FdResponse.from(fd);
    }

    @Override
    @Transactional
    public void processMaturedFds() {
        List<FixedDeposit> matured = fdRepository.findMaturedFds(LocalDate.now());
        for (FixedDeposit fd : matured) {
            try {
                Account account = fd.getSourceAccount();
                account.setBalance(account.getBalance().add(fd.getMaturityAmount()).setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);
                fd.setStatus(FixedDeposit.FdStatus.MATURED);
                fd.setActualInterestEarned(fd.getMaturityAmount().subtract(fd.getPrincipalAmount()));
                fd.setClosedAt(LocalDateTime.now());
                fdRepository.save(fd);
                log.info("Auto-matured FD: " + fd.getFdId());
            } catch (Exception e) {
                log.warning("Failed to mature FD: " + fd.getFdId() + " - " + e.getMessage());
            }
        }
    }
}