package com.studio.app.dto.response;

import com.studio.app.enums.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Earnings summary for a single day.
 * Only includes per-class (PAID) sessions — package-covered sessions are excluded.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyEarningsResponse {

    /** The date these earnings belong to. */
    private LocalDate date;

    /** Number of PAID sessions on this day. */
    private int sessionCount;

    /**
     * Earnings breakdown by original currency.
     * Key = the currency sessions were charged in, value = sum of priceCharged.
     */
    private Map<Currency, BigDecimal> earningsByCurrency;

    /**
     * Total earnings normalised to the requested base currency.
     * Null if no base currency was requested.
     */
    private BigDecimal totalInBaseCurrency;

    /** The base currency used for normalisation (mirrors the request param). */
    private Currency baseCurrency;

    /**
     * Grand total converted into every supported currency for quick reference.
     * Key = target currency, value = equivalent total.
     */
    private Map<Currency, BigDecimal> convertedTotals;
}

