package com.studio.app.service.impl;

import com.studio.app.enums.Currency;
import com.studio.app.service.EarningsService;
import com.studio.app.support.StubCurrencyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(StubCurrencyTestConfig.class)
@Sql(scripts = "/cleanup-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/testdata/service/earnings/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup-test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class EarningsServiceImplTest {

    @Autowired
    private EarningsService earningsService;

    @Test
    void shouldGroupDailyPaidSessionsByDate() {
        var result = earningsService.getDailyEarnings(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null
        );

        assertThat(result.getDailyBreakdown()).hasSize(2);
        assertThat(result.getDailyBreakdown().get(0).getDate()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(result.getDailyBreakdown().get(0).getSessionCount()).isEqualTo(3);
        assertThat(result.getDailyBreakdown().get(1).getDate()).isEqualTo(LocalDate.of(2026, 3, 10));
    }

    @Test
    void shouldGroupMultipleCurrenciesOnSameDay() {
        var result = earningsService.getDailyEarnings(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null
        );

        var day = result.getDailyBreakdown().getFirst();
        assertThat(day.getEarningsByCurrency()).containsKeys(Currency.EUROS, Currency.RUBLES);
        assertThat(day.getEarningsByCurrency().get(Currency.EUROS)).isEqualByComparingTo("60.00");
        assertThat(day.getEarningsByCurrency().get(Currency.RUBLES)).isEqualByComparingTo("2000.00");
    }

    @Test
    void shouldComputeTotalInBaseCurrency() {
        var result = earningsService.getDailyEarnings(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                Currency.EUROS
        );

        var day = result.getDailyBreakdown().getFirst();
        // 60 EUR + (2000 RUB -> 19.80 EUR via stub rates)
        assertThat(day.getTotalInBaseCurrency()).isEqualByComparingTo("79.80");
        assertThat(day.getBaseCurrency()).isEqualTo(Currency.EUROS);
    }

    @Test
    void shouldIncludePackagesInPeriodTotals() {
        var result = earningsService.getDailyEarnings(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null
        );

        assertThat(result.getTotalEarnedByCurrency().get(Currency.EUROS)).isEqualByComparingTo("95.00");
        assertThat(result.getTotalEarnedByCurrency().get(Currency.RUBLES)).isEqualByComparingTo("25000.00");
        assertThat(result.getTotalCouldHaveEarnedExcludingCancellationsByCurrency().get(Currency.EUROS))
                .isEqualByComparingTo("115.00");
        assertThat(result.getTotalCouldHaveEarnedIncludingCancellationsByCurrency().get(Currency.EUROS))
                .isEqualByComparingTo("130.00");
    }

    @Test
    void shouldReturnMonthlyEarningsWithSessionsAndPackages() {
        var result = earningsService.getMonthlyEarnings(YearMonth.of(2026, 3), null);

        assertThat(result.getTotalSessionCount()).isEqualTo(4);
        assertThat(result.getTotalPackageCount()).isEqualTo(2);
        assertThat(result.getSessionEarningsByCurrency().get(Currency.EUROS)).isEqualByComparingTo("95.00");
        assertThat(result.getPackageEarningsByCurrency().get(Currency.RUBLES)).isEqualByComparingTo("23000.00");
        assertThat(result.getDailyBreakdown()).hasSize(2);
    }

    @Test
    void shouldComputeMonthlyTotalInBaseCurrency() {
        var result = earningsService.getMonthlyEarnings(YearMonth.of(2026, 3), Currency.DOLLARS);

        assertThat(result.getBaseCurrency()).isEqualTo(Currency.DOLLARS);
        assertThat(result.getTotalInBaseCurrency()).isNotNull();
        assertThat(result.getTotalInBaseCurrency()).isPositive();
    }

    @Test
    void shouldReturnEmptyBreakdownWhenNoPaidSessions() {
        var result = earningsService.getDailyEarnings(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                null
        );

        assertThat(result.getDailyBreakdown()).isEmpty();
    }
}
