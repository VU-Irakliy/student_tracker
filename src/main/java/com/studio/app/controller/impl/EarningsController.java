package com.studio.app.controller.impl;

import com.studio.app.controller.EarningsApi;
import com.studio.app.dto.response.MonthlyEarningsResponse;
import com.studio.app.dto.response.PeriodEarningsResponse;
import com.studio.app.enums.Currency;
import com.studio.app.service.EarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * REST controller implementation for earnings tracking.
 * Delegates to {@link EarningsService} for business logic.
 */
@RestController
@RequiredArgsConstructor
public class EarningsController implements EarningsApi {

    private final EarningsService earningsService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<PeriodEarningsResponse> getDailyEarnings(LocalDate from,
                                                                    LocalDate to,
                                                                    Currency baseCurrency) {
        return ResponseEntity.ok(earningsService.getDailyEarnings(from, to, baseCurrency));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<MonthlyEarningsResponse> getMonthlyEarnings(int year,
                                                                       int month,
                                                                       Currency baseCurrency) {
        return ResponseEntity.ok(earningsService.getMonthlyEarnings(YearMonth.of(year, month), baseCurrency));
    }
}

