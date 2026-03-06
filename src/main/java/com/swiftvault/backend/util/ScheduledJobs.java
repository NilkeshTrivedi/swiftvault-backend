package com.swiftvault.backend.scheduler;

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
 * Jobs:
 *   1. Fixed Deposit maturity check     — runs daily at 8:00 AM
 *   2. Recurring Deposit installments   — runs daily at 8:30 AM
 *   3. Loan EMI deductions              — runs daily at 9:00 AM
 *   4. Monthly auto-savings (recurring) — runs on 1st of month at 8:00 AM
 *   5. Nightly low-balance scan         — runs daily at 11:30 PM
 *   6. Referral expiry cleanup          — runs weekly on Sunday at 2:00 AM
 *
 * Requires @EnableScheduling on SwiftVaultApplication.java
 */
@Component
public class ScheduledJobs {

    private static final Logger log = Logger.getLogger(ScheduledJobs.class.getName());

    private final FixedDepositRepository   fixedDepositRepository;
    private final RecurringDepositRepository recurringDepositRepository;
    private final LoanRepository           loanRepository;
    private final AccountRepository        accountRepository;
    private final TransactionRepository    transactionRepository;
    private final ReferralRepository       referralRepository;
    private final AutoSavingsService       autoSavingsService;
    private final LowBalanceAlertService   lowBalanceAlertService;

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
    // 1. Fixed Deposit Maturity Check
    //    Runs daily at 8:00 AM
    //    Finds all ACTIVE FDs that have passed their maturity date
    //    Credits principal + interest to the linked account
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processFixedDepositMaturities() {
        log.info("=== FD Maturity Job START ===");
        int processed = 0;
        int failed    = 0;

        List<FixedDeposit> matured = fixedDepositRepository
                .findByStatusAndMaturityDateBefore(FixedDeposit.FdStatus.ACTIVE, LocalDate.now());

        for (FixedDeposit fd : matured) {
            try {
                Account account = accountRepository
                        .findByAccountNumber(fd.getLinkedAccountNumber())
                        .orElse(null);
                if (account == null || account.getStatus() != Account.AccountStatus.ACTIVE) {
                    log.warning("FD maturity skipped — account unavailable: " + fd.getFdId());
                    continue;
                }

                BigDecimal maturityAmount = fd.getMaturityAmount();
                account.setBalance(account.getBalance().add(maturityAmount)
                        .setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);

                fd.setStatus(FixedDeposit.FdStatus.MATURED);
                fixedDepositRepository.save(fd);

                Transaction txn = buildSystemTransaction(
                        fd.getLinkedAccountNumber(),
                        maturityAmount,
                        "FD Maturity — " + fd.getFdId() +
                                " | Principal: ₹" + fd.getPrincipalAmount() +
                                " | Interest: ₹" + maturityAmount.subtract(fd.getPrincipalAmount()).setScale(2, RoundingMode.HALF_UP)
                );
                transactionRepository.save(txn);

                processed++;
                log.info("FD matured: " + fd.getFdId() + " → ₹" + maturityAmount + " credited to " + fd.getLinkedAccountNumber());
            } catch (Exception e) {
                failed++;
                log.severe("FD maturity FAILED for " + fd.getFdId() + ": " + e.getMessage());
            }
        }

        log.info("=== FD Maturity Job DONE | Processed: " + processed + " | Failed: " + failed + " ===");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Recurring Deposit Installment Processing
    //    Runs daily at 8:30 AM
    //    Finds all ACTIVE RDs where next installment date = today
    //    Deducts installment from linked account, credits RD total
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 30 8 * * *")
    @Transactional
    public void processRecurringDepositInstallments() {
        log.info("=== RD Installment Job START ===");
        int processed = 0;
        int failed    = 0;
        int skipped   = 0;

        List<RecurringDeposit> dueRDs = recurringDepositRepository
                .findByStatusAndNextInstallmentDate(RecurringDeposit.RdStatus.ACTIVE, LocalDate.now());

        for (RecurringDeposit rd : dueRDs) {
            try {
                Account account = accountRepository
                        .findByAccountNumber(rd.getLinkedAccountNumber())
                        .orElse(null);
                if (account == null || account.getStatus() != Account.AccountStatus.ACTIVE) {
                    log.warning("RD installment skipped — account unavailable: " + rd.getRdId());
                    skipped++;
                    continue;
                }

                BigDecimal installment = rd.getMonthlyInstallment();
                if (account.getBalance().compareTo(installment) < 0) {
                    log.warning("RD installment skipped — insufficient balance: " + rd.getRdId() +
                            " | Required: ₹" + installment + " | Available: ₹" + account.getBalance());
                    skipped++;
                    continue;
                }

                // Deduct from account
                account.setBalance(account.getBalance().subtract(installment)
                        .setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);

                // Credit to RD
                rd.setTotalDeposited(rd.getTotalDeposited().add(installment)
                        .setScale(2, RoundingMode.HALF_UP));
                rd.setInstallmentsPaid(rd.getInstallmentsPaid() + 1);
                rd.setNextInstallmentDate(rd.getNextInstallmentDate().plusMonths(1));

                // Check if all installments are done
                if (rd.getInstallmentsPaid() >= rd.getTenureMonths()) {
                    rd.setStatus(RecurringDeposit.RdStatus.MATURED);
                    // Credit maturity amount back to account
                    BigDecimal maturityAmount = rd.getMaturityAmount();
                    account.setBalance(account.getBalance().add(maturityAmount)
                            .setScale(2, RoundingMode.HALF_UP));
                    accountRepository.save(account);
                    Transaction maturityTxn = buildSystemTransaction(
                            rd.getLinkedAccountNumber(), maturityAmount,
                            "RD Maturity — " + rd.getRdId());
                    transactionRepository.save(maturityTxn);
                    log.info("RD matured: " + rd.getRdId());
                }

                recurringDepositRepository.save(rd);

                Transaction txn = buildDebitTransaction(
                        rd.getLinkedAccountNumber(), installment,
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
    // 3. Loan EMI Auto-Deduction
    //    Runs daily at 9:00 AM
    //    Finds all ACTIVE loans where next EMI date = today
    //    Deducts EMI from linked account, updates loan outstanding
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processLoanEmiDeductions() {
        log.info("=== Loan EMI Job START ===");
        int processed = 0;
        int failed    = 0;
        int skipped   = 0;

        List<Loan> dueLoans = loanRepository
                .findByStatusAndNextEmiDate(Loan.LoanStatus.ACTIVE, LocalDate.now());

        for (Loan loan : dueLoans) {
            try {
                Account account = accountRepository
                        .findByAccountNumber(loan.getLinkedAccountNumber())
                        .orElse(null);
                if (account == null || account.getStatus() != Account.AccountStatus.ACTIVE) {
                    log.warning("EMI skipped — account unavailable: " + loan.getLoanId());
                    skipped++;
                    continue;
                }

                BigDecimal emi = loan.getEmiAmount();
                if (account.getBalance().compareTo(emi) < 0) {
                    log.warning("EMI skipped — insufficient balance: " + loan.getLoanId() +
                            " | Required: ₹" + emi + " | Available: ₹" + account.getBalance());
                    skipped++;
                    continue;
                }

                // Deduct EMI from account
                account.setBalance(account.getBalance().subtract(emi)
                        .setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);

                // Update loan
                BigDecimal newOutstanding = loan.getOutstandingAmount().subtract(emi)
                        .setScale(2, RoundingMode.HALF_UP);
                loan.setOutstandingAmount(newOutstanding.max(BigDecimal.ZERO));
                loan.setEmisPaid(loan.getEmisPaid() + 1);
                loan.setNextEmiDate(loan.getNextEmiDate().plusMonths(1));

                if (loan.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0
                        || loan.getEmisPaid() >= loan.getTenureMonths()) {
                    loan.setStatus(Loan.LoanStatus.CLOSED);
                    loan.setOutstandingAmount(BigDecimal.ZERO);
                    log.info("Loan fully repaid: " + loan.getLoanId());
                }

                loanRepository.save(loan);

                Transaction txn = buildDebitTransaction(
                        loan.getLinkedAccountNumber(), emi,
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
    // 4. Monthly Recurring Auto-Savings
    //    Runs on 1st of every month at 8:00 AM
    //    Processes all active RECURRING auto-savings rules
    //    e.g. auto-sweeps ₹2,000/month into a savings goal
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
    // 5. Nightly Low-Balance Scan
    //    Runs every night at 11:30 PM
    //    Checks all accounts with configured thresholds
    //    Creates alert if balance < threshold (catches any missed during day)
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
    // 6. Referral Expiry Cleanup
    //    Runs every Sunday at 2:00 AM
    //    Marks PENDING referrals older than 30 days as EXPIRED
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
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Transaction buildSystemTransaction(String toAccount, BigDecimal amount, String description) {
        return Transaction.builder()
                .transactionId(com.swiftvault.backend.util.IdGenerator.transactionId())
                .fromAccount("SYSTEM")
                .toAccount(toAccount)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(description)
                .build();
    }

    private Transaction buildDebitTransaction(String fromAccount, BigDecimal amount, String description) {
        return Transaction.builder()
                .transactionId(com.swiftvault.backend.util.IdGenerator.transactionId())
                .fromAccount(fromAccount)
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(amount)
                .description(description)
                .build();
    }
}