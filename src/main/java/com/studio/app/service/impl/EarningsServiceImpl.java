package com.studio.app.service.impl;

import com.studio.app.dto.response.DailyEarningsResponse;
import com.studio.app.dto.response.MonthlyEarningsResponse;
import com.studio.app.dto.response.PaymentRecordResponse;
import com.studio.app.dto.response.PeriodEarningsResponse;
import com.studio.app.entity.ClassSession;
import com.studio.app.entity.PackagePurchase;
import com.studio.app.enums.Currency;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PackagePurchaseRepository;
import com.studio.app.repository.PaymentFeedRepository;
import com.studio.app.service.CurrencyConversionService;
import com.studio.app.service.EarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link EarningsService}.
 *
 * <p><b>Selected-period daily earnings</b> aggregate sessions with
 * {@code paymentStatus == PAID} (i.e. per-class payments), and also provide
 * period totals for earned amounts, plus potential amounts both excluding
 * cancellations and including cancellations.
 * Package purchases are included in those period totals when
 * {@code paymentDate} falls inside the selected date range.
 *
 * <p><b>Monthly earnings</b> include both per-class session payments <em>and</em>
 * package purchase payments (matched by {@code paymentDate} within the month).
 *
 * <p>Because different students may pay in different currencies, earnings are
 * grouped by original currency. When a {@code baseCurrency} is requested, all
 * per-currency subtotals are converted and summed into a single value.
 * Additionally, a {@code convertedTotals} map shows the grand total expressed
 * in every supported currency.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EarningsServiceImpl implements EarningsService {

    private final ClassSessionRepository sessionRepository;
    private final PackagePurchaseRepository packageRepository;
    private final PaymentFeedRepository paymentFeedRepository;
    private final CurrencyConversionService currencyConversionService;

    /** {@inheritDoc} */
    @Override
    public PeriodEarningsResponse getDailyEarnings(LocalDate from, LocalDate to, Currency baseCurrency) {
        var sessions = sessionRepository.findPaidSessionsByDateRange(from, to);

        // Group sessions by date
        Map<LocalDate, List<ClassSession>> byDate = sessions.stream()
                .collect(Collectors.groupingBy(ClassSession::getClassDate, TreeMap::new, Collectors.toList()));

        List<DailyEarningsResponse> dailyList = byDate.entrySet().stream()
                .map(entry -> buildDailyResponse(entry.getKey(), entry.getValue(), baseCurrency))
                .toList();

        Map<Currency, BigDecimal> sessionEarnedByCurrency = aggregateByCurrency(
                sessions,
                ClassSession::getCurrency,
                ClassSession::getPriceCharged
        );

        var periodPackages = packageRepository.findByPaymentDateRange(from, to);
        Map<Currency, BigDecimal> packagePaidByCurrency = aggregateByCurrency(
                periodPackages,
                PackagePurchase::getCurrency,
                PackagePurchase::getAmountPaid
        );

        Map<Currency, BigDecimal> totalEarnedByCurrency = mergeByCurrency(
                sessionEarnedByCurrency,
                packagePaidByCurrency
        );

        var potentialExcludingCancelledSessions = sessionRepository.findCollectiblePerClassSessionsByDateRange(from, to);
        Map<Currency, BigDecimal> potentialExcludingCancelledPerClassByCurrency = aggregateByCurrency(
                potentialExcludingCancelledSessions,
                ClassSession::getCurrency,
                ClassSession::getPriceCharged
        );

        Map<Currency, BigDecimal> totalCouldHaveEarnedExcludingCancellationsByCurrency = mergeByCurrency(
                potentialExcludingCancelledPerClassByCurrency,
                packagePaidByCurrency
        );

        var potentialIncludingCancelledSessions =
                sessionRepository.findPotentialPerClassSessionsIncludingCancellationsByDateRange(from, to);
        Map<Currency, BigDecimal> potentialIncludingCancelledPerClassByCurrency = aggregateByCurrency(
                potentialIncludingCancelledSessions,
                ClassSession::getCurrency,
                ClassSession::getPriceCharged
        );

        Map<Currency, BigDecimal> totalCouldHaveEarnedIncludingCancellationsByCurrency = mergeByCurrency(
                potentialIncludingCancelledPerClassByCurrency,
                packagePaidByCurrency
        );

        return PeriodEarningsResponse.builder()
                .from(from)
                .to(to)
                .dailyBreakdown(dailyList)
                .totalEarnedByCurrency(totalEarnedByCurrency)
                .totalEarnedInBaseCurrency(computeTotalInBaseCurrency(totalEarnedByCurrency, baseCurrency))
                .totalCouldHaveEarnedExcludingCancellationsByCurrency(totalCouldHaveEarnedExcludingCancellationsByCurrency)
                .totalCouldHaveEarnedExcludingCancellationsInBaseCurrency(
                        computeTotalInBaseCurrency(totalCouldHaveEarnedExcludingCancellationsByCurrency, baseCurrency))
                .totalCouldHaveEarnedIncludingCancellationsByCurrency(totalCouldHaveEarnedIncludingCancellationsByCurrency)
                .totalCouldHaveEarnedIncludingCancellationsInBaseCurrency(
                        computeTotalInBaseCurrency(totalCouldHaveEarnedIncludingCancellationsByCurrency, baseCurrency))
                .baseCurrency(baseCurrency)
                .convertedTotalEarned(computeConvertedTotals(totalEarnedByCurrency))
                .convertedTotalCouldHaveEarnedExcludingCancellations(
                        computeConvertedTotals(totalCouldHaveEarnedExcludingCancellationsByCurrency))
                .convertedTotalCouldHaveEarnedIncludingCancellations(
                        computeConvertedTotals(totalCouldHaveEarnedIncludingCancellationsByCurrency))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public MonthlyEarningsResponse getMonthlyEarnings(YearMonth month, Currency baseCurrency) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        // ── Per-class session earnings ──────────────────────────────────
        var dailyPeriod = getDailyEarnings(from, to, baseCurrency);
        var dailyList = dailyPeriod.getDailyBreakdown();

        Map<Currency, BigDecimal> sessionByCurrency = new EnumMap<>(Currency.class);
        int totalSessions = 0;

        for (var day : dailyList) {
            totalSessions += day.getSessionCount();
            day.getEarningsByCurrency().forEach((cur, amt) ->
                    sessionByCurrency.merge(cur, amt, BigDecimal::add));
        }

        // ── Package purchase earnings ───────────────────────────────────
        var packages = packageRepository.findByPaymentDateRange(from, to);

        Map<Currency, BigDecimal> packageByCurrency = new EnumMap<>(Currency.class);
        for (var pkg : packages) {
            Currency cur = pkg.getCurrency();
            BigDecimal paid = pkg.getAmountPaid();
            if (cur != null && paid != null) {
                packageByCurrency.merge(cur, paid, BigDecimal::add);
            }
        }

        // ── Combined totals ────────────────────────────────────────────
        Map<Currency, BigDecimal> totalByCurrency = new EnumMap<>(Currency.class);
        sessionByCurrency.forEach((cur, amt) -> totalByCurrency.merge(cur, amt, BigDecimal::add));
        packageByCurrency.forEach((cur, amt) -> totalByCurrency.merge(cur, amt, BigDecimal::add));

        BigDecimal totalInBase = computeTotalInBaseCurrency(totalByCurrency, baseCurrency);
        Map<Currency, BigDecimal> convertedTotals = computeConvertedTotals(totalByCurrency);

        return MonthlyEarningsResponse.builder()
                .year(month.getYear())
                .month(month.getMonthValue())
                .totalSessionCount(totalSessions)
                .sessionEarningsByCurrency(sessionByCurrency)
                .totalPackageCount(packages.size())
                .packageEarningsByCurrency(packageByCurrency)
                .totalEarningsByCurrency(totalByCurrency)
                .totalInBaseCurrency(totalInBase)
                .baseCurrency(baseCurrency)
                .convertedTotals(convertedTotals)
                .dailyBreakdown(dailyList)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public Page<PaymentRecordResponse> getAllPayments(Pageable pageable) {
        return paymentFeedRepository.findAllPayments(pageable);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private <T> Map<Currency, BigDecimal> aggregateByCurrency(List<T> items,
                                                              java.util.function.Function<T, Currency> currencyExtractor,
                                                              java.util.function.Function<T, BigDecimal> amountExtractor) {
        Map<Currency, BigDecimal> byCurrency = new EnumMap<>(Currency.class);
        for (var item : items) {
            Currency cur = currencyExtractor.apply(item);
            BigDecimal amount = amountExtractor.apply(item);
            if (cur != null && amount != null) {
                byCurrency.merge(cur, amount, BigDecimal::add);
            }
        }
        return byCurrency;
    }

    private Map<Currency, BigDecimal> mergeByCurrency(Map<Currency, BigDecimal> left,
                                                      Map<Currency, BigDecimal> right) {
        Map<Currency, BigDecimal> merged = new EnumMap<>(Currency.class);
        left.forEach((cur, amt) -> merged.merge(cur, amt, BigDecimal::add));
        right.forEach((cur, amt) -> merged.merge(cur, amt, BigDecimal::add));
        return merged;
    }

    private DailyEarningsResponse buildDailyResponse(LocalDate date,
                                                      List<ClassSession> sessions,
                                                      Currency baseCurrency) {
        // Sum prices grouped by their original currency
        Map<Currency, BigDecimal> byCurrency = aggregateByCurrency(
                sessions,
                ClassSession::getCurrency,
                ClassSession::getPriceCharged
        );

        BigDecimal totalInBase = computeTotalInBaseCurrency(byCurrency, baseCurrency);
        Map<Currency, BigDecimal> convertedTotals = computeConvertedTotals(byCurrency);

        return DailyEarningsResponse.builder()
                .date(date)
                .sessionCount(sessions.size())
                .earningsByCurrency(byCurrency)
                .totalInBaseCurrency(totalInBase)
                .baseCurrency(baseCurrency)
                .convertedTotals(convertedTotals)
                .build();
    }

    /**
     * Converts each per-currency subtotal into the baseCurrency and sums them.
     * Returns null if baseCurrency is not provided.
     */
    private BigDecimal computeTotalInBaseCurrency(Map<Currency, BigDecimal> byCurrency,
                                                   Currency baseCurrency) {
        if (baseCurrency == null || byCurrency.isEmpty()) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var entry : byCurrency.entrySet()) {
            if (entry.getKey() == baseCurrency) {
                total = total.add(entry.getValue());
            } else {
                Map<Currency, BigDecimal> converted =
                        currencyConversionService.convertToAll(entry.getValue(), entry.getKey());
                BigDecimal inBase = converted.getOrDefault(baseCurrency, BigDecimal.ZERO);
                total = total.add(inBase);
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes the grand total expressed in every supported currency.
     * For each target currency, converts all per-currency subtotals and sums them.
     */
    private Map<Currency, BigDecimal> computeConvertedTotals(Map<Currency, BigDecimal> byCurrency) {
        if (byCurrency.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);
        for (Currency target : Currency.values()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (var entry : byCurrency.entrySet()) {
                if (entry.getKey() == target) {
                    sum = sum.add(entry.getValue());
                } else {
                    Map<Currency, BigDecimal> converted =
                            currencyConversionService.convertToAll(entry.getValue(), entry.getKey());
                    sum = sum.add(converted.getOrDefault(target, BigDecimal.ZERO));
                }
            }
            result.put(target, sum.setScale(2, RoundingMode.HALF_UP));
        }
        return Collections.unmodifiableMap(result);
    }
}

