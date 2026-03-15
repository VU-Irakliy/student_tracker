package com.studio.app.dto.response;

import com.studio.app.enums.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Earnings summary for an arbitrary selected date range.
 *
 * <p>Contains a daily breakdown of earned amounts (paid per-class sessions)
 * and period-level totals for both earned and collectible amounts.
 * Package purchases are included in period totals when {@code paymentDate}
 * is inside the selected range.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodEarningsResponse {

    /** Start date of the selected period (inclusive). */
    private LocalDate from;

    /** End date of the selected period (inclusive). */
    private LocalDate to;

    /** Per-day earnings entries for paid per-class sessions in the selected period. */
    private List<DailyEarningsResponse> dailyBreakdown;

    /** Total earned amount in original currencies (paid per-class sessions + packages paid in range). */
    private Map<Currency, BigDecimal> totalEarnedByCurrency;

    /** Total earned normalised to {@code baseCurrency}; null when base currency is not requested. */
    private BigDecimal totalEarnedInBaseCurrency;

    /** Total amount that could have been earned excluding cancelled per-class sessions, plus packages paid in range. */
    private Map<Currency, BigDecimal> totalCouldHaveEarnedExcludingCancellationsByCurrency;

    /** Excluding-cancellations potential total normalised to {@code baseCurrency}; null when base currency is not requested. */
    private BigDecimal totalCouldHaveEarnedExcludingCancellationsInBaseCurrency;

    /** Total amount that could have been earned including cancelled per-class sessions, plus packages paid in range. */
    private Map<Currency, BigDecimal> totalCouldHaveEarnedIncludingCancellationsByCurrency;

    /** Including-cancellations potential total normalised to {@code baseCurrency}; null when base currency is not requested. */
    private BigDecimal totalCouldHaveEarnedIncludingCancellationsInBaseCurrency;

    /** Base currency used for normalisation in this response. */
    private Currency baseCurrency;

    /** Earned total converted into every supported currency for quick comparison. */
    private Map<Currency, BigDecimal> convertedTotalEarned;

    /** Excluding-cancellations potential total converted into every supported currency for quick comparison. */
    private Map<Currency, BigDecimal> convertedTotalCouldHaveEarnedExcludingCancellations;

    /** Including-cancellations potential total converted into every supported currency for quick comparison. */
    private Map<Currency, BigDecimal> convertedTotalCouldHaveEarnedIncludingCancellations;
}

