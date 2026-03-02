package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.LoanApplicationRequest;
import com.swiftvault.backend.dto.request.PayEmiRequest;
import com.swiftvault.backend.dto.response.LoanResponse;
import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.*;
import com.swiftvault.backend.service.LoanService;
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
public class LoanServiceImpl implements LoanService {

    private static final Logger log = Logger.getLogger(LoanServiceImpl.class.getName());

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public LoanServiceImpl(LoanRepository loanRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           UserService userService,
                           PasswordEncoder passwordEncoder) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public LoanResponse applyForLoan(String userId, LoanApplicationRequest request) {
        User user = userService.findById(userId);
        Account account = accountRepository.findById(request.getAccountNumber())
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found"));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Account is not active");

        List<Loan> existing = loanRepository.findByUserAndStatus(user, Loan.LoanStatus.ACTIVE);
        boolean hasSameType = existing.stream().anyMatch(l -> l.getLoanType() == request.getLoanType());
        if (hasSameType)
            throw SwiftVaultException.badRequest("You already have an active " + request.getLoanType() + " loan");

        List<Loan> pending = loanRepository.findByUserAndStatus(user, Loan.LoanStatus.PENDING);
        if (!pending.isEmpty())
            throw SwiftVaultException.badRequest("You have a pending loan application. Please wait for approval.");

        BigDecimal rate          = Loan.getInterestRate(request.getLoanType());
        BigDecimal emi           = Loan.calculateEmi(request.getLoanAmount(), rate, request.getTenureMonths());
        BigDecimal totalPayable  = emi.multiply(new BigDecimal(request.getTenureMonths())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayable.subtract(request.getLoanAmount()).setScale(2, RoundingMode.HALF_UP);

        Loan loan = Loan.builder()
                .loanId(IdGenerator.loanId())
                .user(user)
                .disbursalAccount(account)
                .loanType(request.getLoanType())
                .loanAmount(request.getLoanAmount())
                .interestRate(rate)
                .tenureMonths(request.getTenureMonths())
                .emiAmount(emi)
                .totalPayable(totalPayable)
                .totalInterest(totalInterest)
                .outstandingBalance(totalPayable)
                .purpose(request.getPurpose())
                .status(Loan.LoanStatus.PENDING)
                .build();
        loanRepository.save(loan);
        log.info("Loan application: " + loan.getLoanId() + " for user: " + userId);
        return LoanResponse.from(loan);
    }

    @Override
    public List<LoanResponse> getMyLoans(String userId) {
        User user = userService.findById(userId);
        return loanRepository.findByUser(user).stream().map(LoanResponse::from).toList();
    }

    @Override
    public LoanResponse getLoan(String userId, String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> SwiftVaultException.notFound("Loan not found"));
        if (!loan.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        return LoanResponse.from(loan);
    }

    @Override
    @Transactional
    public LoanResponse payEmi(String userId, PayEmiRequest request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> SwiftVaultException.notFound("Loan not found"));
        if (!loan.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("Access denied");
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE)
            throw SwiftVaultException.badRequest("Loan is not active");

        User user = userService.findById(userId);
        if (!passwordEncoder.matches(request.getTransactionPin(), user.getPinHash()))
            throw SwiftVaultException.badRequest("Invalid transaction PIN");

        Account account = accountRepository.findById(request.getAccountNumber())
                .orElseThrow(() -> SwiftVaultException.notFound("Account not found"));
        if (!account.getUser().getUserId().equals(userId))
            throw SwiftVaultException.forbidden("You don't own this account");
        if (account.getBalance().compareTo(loan.getEmiAmount()) < 0)
            throw SwiftVaultException.badRequest("Insufficient balance to pay EMI of ₹" + loan.getEmiAmount());

        account.setBalance(account.getBalance().subtract(loan.getEmiAmount()).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        loan.setEmisPaid(loan.getEmisPaid() + 1);
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(loan.getEmiAmount()).setScale(2, RoundingMode.HALF_UP));
        loan.setNextEmiDate(loan.getNextEmiDate() != null ? loan.getNextEmiDate().plusMonths(1) : LocalDate.now().plusMonths(1));

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(account.getAccountNumber())
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(loan.getEmiAmount())
                .description("EMI payment - " + loan.getLoanType() + " Loan (" + loan.getEmisPaid() + "/" + loan.getTenureMonths() + ")")
                .build();
        transactionRepository.save(txn);

        if (loan.getEmisPaid() >= loan.getTenureMonths()) {
            loan.setStatus(Loan.LoanStatus.CLOSED);
            loan.setOutstandingBalance(BigDecimal.ZERO);
            loan.setClosedAt(LocalDateTime.now());
            log.info("Loan fully repaid: " + loan.getLoanId());
        }
        loanRepository.save(loan);
        return LoanResponse.from(loan);
    }

    @Override
    public List<LoanResponse> getPendingLoans() {
        return loanRepository.findByStatus(Loan.LoanStatus.PENDING).stream().map(LoanResponse::from).toList();
    }

    @Override
    @Transactional
    public LoanResponse approveLoan(String loanId, String adminNotes) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> SwiftVaultException.notFound("Loan not found"));
        if (loan.getStatus() != Loan.LoanStatus.PENDING)
            throw SwiftVaultException.badRequest("Loan is not in PENDING status");

        Account account = loan.getDisbursalAccount();
        account.setBalance(account.getBalance().add(loan.getLoanAmount()).setScale(2, RoundingMode.HALF_UP));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .toAccount(account.getAccountNumber())
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(loan.getLoanAmount())
                .description("Loan disbursed - " + loan.getLoanType())
                .build();
        transactionRepository.save(txn);

        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loan.setDisbursalDate(LocalDate.now());
        loan.setNextEmiDate(LocalDate.now().plusMonths(1));
        loan.setAdminNotes(adminNotes);
        loanRepository.save(loan);
        log.info("Loan approved: " + loanId);
        return LoanResponse.from(loan);
    }

    @Override
    @Transactional
    public LoanResponse rejectLoan(String loanId, String reason) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> SwiftVaultException.notFound("Loan not found"));
        if (loan.getStatus() != Loan.LoanStatus.PENDING)
            throw SwiftVaultException.badRequest("Loan is not in PENDING status");
        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        loanRepository.save(loan);
        log.info("Loan rejected: " + loanId);
        return LoanResponse.from(loan);
    }
}