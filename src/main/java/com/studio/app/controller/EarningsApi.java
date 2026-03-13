package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.response.DailyEarningsResponse;
import com.studio.app.dto.response.MonthlyEarningsResponse;
import com.studio.app.enums.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API contract for earnings tracking.
 * Provides endpoints to view daily and monthly earnings from per-class payments.
 * Package-covered sessions are excluded from all earnings calculations.
 */
@Tag(name = "Earnings", description = "Daily and monthly earnings tracking (per-class payments only)")
@RequestMapping(ApiConstants.EARNINGS)
public interface EarningsApi {

    /**
     * Returns daily earnings summaries for the given date range.
     *
     * @param from         start date (inclusive)
     * @param to           end date (inclusive)
     * @param baseCurrency optional currency to normalise all totals into
     * @return list of daily earnings (one entry per day with at least one paid session)
     */
    @Operation(summary = "Get daily earnings",
            description = "Returns daily earnings for the given date range. "
                    + "Only per-class (PAID) sessions are included — package payments are excluded. "
                    + "Optionally specify a baseCurrency to get a single normalised total.")
    @GetMapping("/daily")
    ResponseEntity<List<DailyEarningsResponse>> getDailyEarnings(
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

