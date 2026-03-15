package com.studio.app.service;

/**
 * Recalculates debtor status for active students.
 */
public interface DebtorStatusService {

    /**
     * Updates debtor flags for students whose local time is after the configured nightly cutoff.
     */
    void refreshDebtorStatuses();

    /**
     * Recomputes debtor flags, optionally bypassing the nightly local-time cutoff.
     *
     * @param ignoreCutoff when true, all active students are recalculated immediately
     */
    void refreshDebtorStatuses(boolean ignoreCutoff);
}


