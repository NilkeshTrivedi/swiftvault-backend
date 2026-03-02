package com.swiftvault.backend.util;

import com.swiftvault.backend.service.FixedDepositService;
import com.swiftvault.backend.service.RecurringDepositService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.logging.Logger;

@Component
public class ScheduledJobs {

    private static final Logger log = Logger.getLogger(ScheduledJobs.class.getName());

    private final FixedDepositService fdService;
    private final RecurringDepositService rdService;

    public ScheduledJobs(FixedDepositService fdService, RecurringDepositService rdService) {
        this.fdService = fdService;
        this.rdService = rdService;
    }

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void processMaturedFds() {
        log.info("Running FD maturity check...");
        fdService.processMaturedFds();
    }

    // Run every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void processRdAutoDebits() {
        log.info("Running RD auto-debit...");
        rdService.processAutoDebitRds();
    }
}
