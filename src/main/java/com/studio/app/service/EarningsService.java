package com.studio.app.service;

import com.studio.app.dto.response.DailyEarningsResponse;
import com.studio.app.dto.response.MonthlyEarningsResponse;
import com.studio.app.enums.Currency;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Service for calculating earnings.
 * <p>Daily earnings only include per-class (PAID) sessions.
 * <p>Monthly earnings include both per-class session payments and package purchase payments.
 */
public interface EarningsService {

    /**
     * Returns daily earnings summaries for each day in the given date range.
     *
     * @param from         start date (inclusive)
     * @param to           end date (inclusive)
     * @param baseCurrency optional target currency to normalise totals into
     * @return list of daily earnings, one entry per day that has at least one paid session
     */
    List<DailyEarningsResponse> getDailyEarnings(LocalDate from, LocalDate to, Currency baseCurrency);

    /**
     * Returns a monthly earnings summary, including a per-day breakdown.
     *
     * @param month        the target month
     * @param baseCurrency optional target currency to normalise totals into
     * @return aggregated monthly earnings
     */
    MonthlyEarningsResponse getMonthlyEarnings(YearMonth month, Currency baseCurrency);
}

