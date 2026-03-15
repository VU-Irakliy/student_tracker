package com.studio.app.scheduler;

import com.studio.app.service.DebtorStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers debtor-status recomputation as a batch process.
 */
@Component
@RequiredArgsConstructor
public class DebtorStatusScheduler {

    private final DebtorStatusService debtorStatusService;

    @Value("${debtor.batch.run-on-startup:true}")
    private boolean runOnStartup;

    /**
     * Startup catch-up run. Ignores 22:00 gate so flags are corrected right after boot.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void refreshDebtorStatusesOnStartup() {
        if (runOnStartup) {
            debtorStatusService.refreshDebtorStatuses(true);
        }
    }

    /**
     * Runs hourly by default. Only students whose local time is 22:00+ are processed.
     */
    @Scheduled(cron = "${debtor.batch.cron:0 5 * * * *}")
    public void refreshDebtorStatuses() {
        debtorStatusService.refreshDebtorStatuses();
    }
}


