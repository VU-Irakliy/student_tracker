package com.studio.app.service;

import com.studio.app.dto.response.MonthlyEarningsResponse;
import com.studio.app.dto.response.PaymentRecordResponse;
import com.studio.app.dto.response.PeriodEarningsResponse;
import com.studio.app.enums.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Service for calculating earnings.
 * <p>Selected-period daily breakdown includes per-class (PAID) sessions.
 * Period totals include package purchases whose payment date is in range.
 * <p>Monthly earnings include both per-class session payments and package purchase payments.
 * <p>Weekly earnings can be obtained by requesting a 7-day daily period.
 */
public interface EarningsService {

    /**
     * Returns daily earnings summaries for each day in the given date range.
     *
     * @param from         start date (inclusive)
     * @param to           end date (inclusive)
     * @param baseCurrency optional target currency to normalise totals into
     * @return selected-period earnings with daily breakdown, total earned,
     * potential total excluding cancellations, and potential total including cancellations
     */
    PeriodEarningsResponse getDailyEarnings(LocalDate from, LocalDate to, Currency baseCurrency);

    /**
     * Returns a monthly earnings summary, including a per-day breakdown.
     *
     * @param month        the target month
     * @param baseCurrency optional target currency to normalise totals into
     * @return aggregated monthly earnings
     */
    MonthlyEarningsResponse getMonthlyEarnings(YearMonth month, Currency baseCurrency);

    /**
     * Returns all recorded payments (per-class session payments + package purchases), paginated.
     *
     * @param pageable pagination request
     * @return page of payment records ordered from newest to oldest
     */
    Page<PaymentRecordResponse> getAllPayments(Pageable pageable);
}

