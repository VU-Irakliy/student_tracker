package com.studio.app.dto.response;

import com.studio.app.enums.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated earnings for a month.
 * Includes both per-class (PAID) session earnings and package purchase payments.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyEarningsResponse {

    /** Year of the earnings period. */
    private int year;

    /** Month of the earnings period (1–12). */
    private int month;

    // ── Per-class (PAID) session earnings ───────────────────────────────

    /** Total number of PAID (per-class) sessions in the month. */
    private int totalSessionCount;

    /**
     * Per-class session earnings breakdown by original currency.
     * Key = currency sessions were charged in, value = sum of priceCharged.
     */
    private Map<Currency, BigDecimal> sessionEarningsByCurrency;

    // ── Package purchase earnings ───────────────────────────────────────

    /** Total number of package purchases made in the month. */
    private int totalPackageCount;

    /**
     * Package purchase earnings breakdown by original currency.
     * Key = currency the package was paid in, value = sum of amountPaid.
     */
    private Map<Currency, BigDecimal> packageEarningsByCurrency;

    // ── Combined totals ────────────────────────────────────────────────

    /**
     * Combined (sessions + packages) earnings breakdown by original currency.
     */
    private Map<Currency, BigDecimal> totalEarningsByCurrency;

    /**
     * Combined total normalised to the requested base currency.
     * Null if no base currency was requested.
     */
    private BigDecimal totalInBaseCurrency;

    /** The base currency used for normalisation (mirrors the request param). */
    private Currency baseCurrency;

    /**
     * Combined grand total converted into every supported currency for quick reference.
     * Key = target currency, value = equivalent total.
     */
    private Map<Currency, BigDecimal> convertedTotals;

    /** Per-day breakdown of per-class (PAID) session earnings within the month. */
    private List<DailyEarningsResponse> dailyBreakdown;
}

