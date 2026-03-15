package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.response.MonthlyEarningsResponse;
import com.studio.app.dto.response.PeriodEarningsResponse;
import com.studio.app.enums.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * REST API contract for earnings tracking.
 * Provides endpoints to view selected-period and monthly earnings.
 * Daily breakdown entries include paid per-class sessions only,
 * while period totals and monthly totals include package purchases
 * whose payment date is in range.
 *
 * <p>Weekly earnings can be derived without a dedicated endpoint by calling
 * {@code /daily} with a 7-day {@code from}/{@code to} window and reading
 * the period totals from the response.
 */
@Tag(name = "Earnings", description = "Selected-period and monthly earnings tracking")
@RequestMapping(ApiConstants.EARNINGS)
public interface EarningsApi {

    /**
     * Returns earnings for the selected date range.
     *
     * @param from         start date (inclusive)
     * @param to           end date (inclusive)
     * @param baseCurrency optional currency to normalise all totals into
     * @return period response with daily breakdown plus range totals:
     * total earned, total potential excluding cancellations,
     * and total potential including cancellations
     */
    @Operation(summary = "Get daily earnings",
            description = "Returns daily earnings for the given date range. "
                    + "Includes daily earned breakdown (PAID per-class sessions), "
                    + "period total earned, period potential excluding cancellations, "
                    + "and period potential including cancellations. "
                    + "Package purchases are included in period totals when package paymentDate "
                    + "is inside the selected range. "
                    + "Use any 7-day window to get weekly earnings without backend changes. "
                    + "Optionally specify a baseCurrency to get normalised totals.")
    @GetMapping("/daily")
    ResponseEntity<PeriodEarningsResponse> getDailyEarnings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Currency baseCurrency);

    /**
     * Returns a monthly earnings summary with per-day breakdown.
     *
     * @param year         the year (e.g. 2026)
     * @param month        the month (1–12)
     * @param baseCurrency optional currency to normalise all totals into
     * @return aggregated monthly earnings
     */
    @Operation(summary = "Get monthly earnings",
            description = "Returns aggregated monthly earnings with a per-day breakdown. "
                    + "Includes both per-class (PAID) session payments and package purchase payments. "
                    + "Optionally specify a baseCurrency to get a single normalised total.")
    @GetMapping("/monthly")
    ResponseEntity<MonthlyEarningsResponse> getMonthlyEarnings(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Currency baseCurrency);
}

