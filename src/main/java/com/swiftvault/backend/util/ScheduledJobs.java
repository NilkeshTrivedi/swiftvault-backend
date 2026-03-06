package com.swiftvault.backend.util;

import com.swiftvault.backend.entity.*;
import com.swiftvault.backend.repository.*;
import com.swiftvault.backend.service.AutoSavingsService;
import com.swiftvault.backend.service.LowBalanceAlertService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * All scheduled background jobs for SwiftVault.
 *
 * Requires @EnableScheduling on SwiftVaultApplication (already present).
 */
@Component
public class ScheduledJobs {

    private static final Logger log = Logger.getLogger(ScheduledJobs.class.getName());

    private final FixedDepositRepository    fixedDepositRepository;
    private final RecurringDepositRepository recurringDepositRepository;
    private final LoanRepository            loanRepository;
    private final AccountRepository         accountRepository;
    private final TransactionRepository     transactionRepository;
    private final ReferralRepository        referralRepository;
    private final AutoSavingsService        autoSavingsService;
    private final LowBalanceAlertService    lowBalanceAlertService;

    public ScheduledJobs(FixedDepositRepository fixedDepositRepository,
                         RecurringDepositRepository recurringDepositRepository,
                         LoanRepository loanRepository,
                         AccountRepository accountRepository,
                         TransactionRepository transactionRepository,
                         ReferralRepository referralRepository,
                         AutoSavingsService autoSavingsService,
                         LowBalanceAlertService lowBalanceAlertService) {
        this.fixedDepositRepository    = fixedDepositRepository;
        this.recurringDepositRepository = recurringDepositRepository;
        this.loanRepository            = loanRepository;
        this.accountRepository         = accountRepository;
        this.transactionRepository     = transactionRepository;
        this.referralRepository        = referralRepository;
        this.autoSavingsService        = autoSavingsService;
        this.lowBalanceAlertService    = lowBalanceAlertService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Fixed Deposit Maturity — daily at 8:00 AM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processFixedDepositMaturities() {
        log.info("=== FD Maturity Job START ===");
        int processed = 0, failed = 0;

        // FIX: use findMaturedFds(LocalDate) — the actual query method in FixedDepositRepository
        List<FixedDeposit> matured = fixedDepositRepository.findMaturedFds(LocalDate.now());

        for (FixedDeposit fd : matured) {
            try {
                // FIX: fd.getSourceAccount().getAccountNumber() — no getLinkedAccountNumber()
                String accountNumber = fd.getSourceAccount().getAccountNumber();
                Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
                if (account == null || account.getStatus() != Account.AccountStatus.ACTIVE) {
                    log.warning("FD maturity skipped — account unavailable: " + fd.getFdId());
                    continue;
                }

                BigDecimal maturityAmount = fd.getMaturityAmount();
                account.setBalance(account.getBalance().add(maturityAmount)
                        .setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);

                fd.setStatus(FixedDeposit.FdStatus.MATURED);
                fd.setActualInterestEarned(maturityAmount.subtract(fd.getPrincipalAmount()));
                fd.setClosedAt(LocalDateTime.now());
                fixedDepositRepository.save(fd);

                Transaction txn = buildSystemTransaction(
                        accountNumber, maturityAmount,
                        "FD Maturity — " + fd.getFdId() +
                                " | Principal: ₹" + fd.getPrincipalAmount() +
                                " | Interest: ₹" + maturityAmount.subtract(fd.getPrincipalAmount())
                                .setScale(2, RoundingMode.HALF_UP)
                );
                transactionRepository.save(txn);

                processed++;
                log.info("FD matured: " + fd.getFdId() + " → ₹" + maturityAmount);
            } catch (Exception e) {
                failed++;
                log.severe("FD maturity FAILED for " + fd.getFdId() + ": " + e.getMessage());
            }
        }
        log.info("=== FD Maturity Job DONE | Processed: " + processed + " | Failed: " + failed + " ===");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Recurring Deposit Installments — daily at 8:30 AM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 30 8 * * *")
    @Transactional
    public void processRecurringDepositInstallments() {
        log.info("=== RD Installment Job START ===");
        int processed = 0, failed = 0, skipped = 0;

        // FIX: use findDueRds(LocalDate) — the actual query method in RecurringDepositRepository
        List<RecurringDeposit> dueRDs = recurringDepositRepository.findDueRds(LocalDate.now());

        for (RecurringDeposit rd : dueRDs) {
            try {
                // FIX: rd.getSourceAccount().getAccountNumber() — no getLinkedAccountNumber()
                String accountNumber = rd.getSourceAccount().getAccountNumber();
                Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
                if (account == null || account.getStatus() != Account.AccountStatus.ACTIVE) {
                    log.warning("RD installment skipped — account unavailable: " + rd.getRdId());
                    skipped++;
                    continue;
                }

                BigDecimal installment = rd.getMonthlyInstallment();
                if (account.getBalance().compareTo(installment) < 0) {
                    log.warning("RD installment skipped — insufficient balance: " + rd.getRdId());
                    rd.setInstallmentsMissed(rd.getInstallmentsMissed() + 1);
                    // FIX: rd.getNextDueDate() / setNextDueDate() — NOT getNextInstallmentDate()
                    rd.setNextDueDate(rd.getNextDueDate().plusMonths(1));
                    recurringDepositRepository.save(rd);
                    skipped++;
                    continue;
                }

                account.setBalance(account.getBalance().subtract(installment)
                        .setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);

                rd.setTotalDeposited(rd.getTotalDeposited().add(installment).setScale(2, RoundingMode.HALF_UP));
                rd.setInstallmentsPaid(rd.getInstallmentsPaid() + 1);
                // FIX: setNextDueDate — not setNextInstallmentDate
                rd.setNextDueDate(rd.getNextDueDate().plusMonths(1));

                // Auto-mature when all installments paid
                if (rd.getInstallmentsPaid() >= rd.getTenureMonths()) {
                    rd.setStatus(RecurringDeposit.RdStatus.MATURED);
                    rd.setClosedAt(LocalDateTime.now());
                    BigDecimal maturityAmount = rd.getMaturityAmount();
                    account.setBalance(account.getBalance().add(maturityAmount)
                            .setScale(2, RoundingMode.HALF_UP));
                    accountRepository.save(account);
                    Transaction matTxn = buildSystemTransaction(accountNumber, maturityAmount,
                            "RD Maturity — " + rd.getRdId());
                    transactionRepository.save(matTxn);
                    log.info("RD matured: " + rd.getRdId());
                }

                recurringDepositRepository.save(rd);

                Transaction txn = buildDebitTransaction(
                        accountNumber, installment,
                        "RD Installment #" + rd.getInstallmentsPaid() + " — " + rd.getRdId()
                );
                transactionRepository.save(txn);

                processed++;
            } catch (Exception e) {
                failed++;
                log.severe("RD installment FAILED for " + rd.getRdId() + ": " + e.getMessage());
            }
        }
        log.info("=== RD Installment Job DONE | Processed: " + processed +
                " | Skipped: " + skipped + " | Failed: " + failed + " ===");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Loan EMI Auto-Deduction — daily at 9:00 AM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processLoanEmiDeductions() {
        log.info("=== Loan EMI Job START ===");
        int processed = 0, failed = 0, skipped = 0;

        // FIX: use findDueEmis(LocalDate) — the actual query method in LoanRepository
        List<Loan> dueLoans = loanRepository.findDueEmis(LocalDate.now());

        for (Loan loan : dueLoans) {
            try {
                // FIX: loan.getDisbursalAccount().getAccountNumber() — no getLinkedAccountNumber()
                String accountNumber = loan.getDisbursalAccount().getAccountNumber();
                Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
                if (account == null || account.getStatus() != Account.AccountStatus.ACTIVE) {
                    log.warning("EMI skipped — account unavailable: " + loan.getLoanId());
                    skipped++;
                    continue;
                }

                BigDecimal emi = loan.getEmiAmount();
                if (account.getBalance().compareTo(emi) < 0) {
                    log.warning("EMI skipped — insufficient balance: " + loan.getLoanId());
                    loan.setEmisMissed(loan.getEmisMissed() + 1);
                    loan.setNextEmiDate(loan.getNextEmiDate().plusMonths(1));
                    loanRepository.save(loan);
                    skipped++;
                    continue;
                }

                account.setBalance(account.getBalance().subtract(emi)
                        .setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);

                // FIX: loan.getOutstandingBalance() / setOutstandingBalance() — not getOutstandingAmount()
                BigDecimal newOutstanding = loan.getOutstandingBalance().subtract(emi)
                        .setScale(2, RoundingMode.HALF_UP);
                loan.setOutstandingBalance(newOutstanding.max(BigDecimal.ZERO));
                loan.setEmisPaid(loan.getEmisPaid() + 1);
                loan.setNextEmiDate(loan.getNextEmiDate().plusMonths(1));

                if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0
                        || loan.getEmisPaid() >= loan.getTenureMonths()) {
                    loan.setStatus(Loan.LoanStatus.CLOSED);
                    loan.setOutstandingBalance(BigDecimal.ZERO);
                    loan.setClosedAt(LocalDateTime.now());
                    log.info("Loan fully repaid: " + loan.getLoanId());
                }

                loanRepository.save(loan);

                Transaction txn = buildDebitTransaction(
                        accountNumber, emi,
                        "Loan EMI #" + loan.getEmisPaid() + " — " + loan.getLoanId()
                );
                transactionRepository.save(txn);

                processed++;
            } catch (Exception e) {
                failed++;
                log.severe("EMI FAILED for " + loan.getLoanId() + ": " + e.getMessage());
            }
        }
        log.info("=== Loan EMI Job DONE | Processed: " + processed +
                " | Skipped: " + skipped + " | Failed: " + failed + " ===");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Monthly Recurring Auto-Savings — 1st of month at 8:00 AM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 8 1 * *")
    public void processMonthlyAutoSavings() {
        log.info("=== Monthly Auto-Savings Job START ===");
        try {
            autoSavingsService.processRecurringAutoSave();
            log.info("=== Monthly Auto-Savings Job COMPLETE ===");
        } catch (Exception e) {
            log.severe("Monthly Auto-Savings Job FAILED: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Nightly Low-Balance Scan — daily at 11:30 PM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 30 23 * * *")
    public void runNightlyLowBalanceScan() {
        log.info("=== Nightly Low-Balance Scan START ===");
        try {
            lowBalanceAlertService.runNightlyScan();
            log.info("=== Nightly Low-Balance Scan COMPLETE ===");
        } catch (Exception e) {
            log.severe("Nightly Low-Balance Scan FAILED: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Referral Expiry Cleanup — Sundays at 2:00 AM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void expireStaleReferrals() {
        log.info("=== Referral Expiry Job START ===");
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            List<Referral> stale = referralRepository.findAll().stream()
                    .filter(r -> r.getStatus() == Referral.ReferralStatus.PENDING
                            && r.getCreatedAt().isBefore(cutoff))
                    .toList();
            stale.forEach(r -> r.setStatus(Referral.ReferralStatus.EXPIRED));
            referralRepository.saveAll(stale);
            log.info("=== Referral Expiry Job DONE | Expired: " + stale.size() + " ===");
        } catch (Exception e) {
            log.severe("Referral Expiry Job FAILED: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Transaction buildSystemTransaction(String toAccount, BigDecimal amount, String description) {
        return Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount("SYSTEM")
                .toAccount(toAccount)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(description)
                .build();
    }

    private Transaction buildDebitTransaction(String fromAccount, BigDecimal amount, String description) {
        return Transaction.builder()
                .transactionId(IdGenerator.transactionId())
                .fromAccount(fromAccount)
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(amount)
                .description(description)
                .build();
    }
}