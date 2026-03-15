package com.studio.app.service.impl;

import com.studio.app.entity.ClassSession;
import com.studio.app.entity.PackagePurchase;
import com.studio.app.entity.Student;
import com.studio.app.enums.Currency;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PackagePurchaseRepository;
import com.studio.app.service.CurrencyConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EarningsServiceImplTest {

    @Mock ClassSessionRepository sessionRepository;
    @Mock PackagePurchaseRepository packageRepository;
    @Mock CurrencyConversionService currencyConversionService;

    @InjectMocks EarningsServiceImpl earningsService;

    private Student euroStudent;
    private Student rubleStudent;

    @BeforeEach
    void setUp() {
        euroStudent = Student.builder()
                .id(1L).firstName("Ana").lastName("García")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("30.00"))
                .currency(Currency.EUROS)
                .timezone(StudioTimezone.SPAIN)
                .build();

        rubleStudent = Student.builder()
                .id(2L).firstName("Ivan").lastName("Petrov")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("2000.00"))
                .currency(Currency.RUBLES)
                .timezone(StudioTimezone.RUSSIA_MOSCOW)
                .build();

        when(sessionRepository.findCollectiblePerClassSessionsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(sessionRepository.findPotentialPerClassSessionsIncludingCancellationsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(packageRepository.findByPaymentDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
    }

    private ClassSession paidSession(Long id, Student student, LocalDate date,
                                      BigDecimal price, Currency currency) {
        var s = ClassSession.builder()
                .id(id).student(student)
                .classDate(date)
                .startTime(LocalTime.of(10, 0))
                .durationMinutes(60)
                .priceCharged(price)
                .currency(currency)
                .build();
        s.setPaymentStatus(PaymentStatus.PAID);
        return s;
    }

    @Nested
    class DailyEarnings {

        @Test
        void shouldReturnEmptyListWhenNoPaidSessions() {
            var from = LocalDate.of(2026, 3, 1);
            var to = LocalDate.of(2026, 3, 31);

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of());

            var result = earningsService.getDailyEarnings(from, to, null);

            assertThat(result.getDailyBreakdown()).isEmpty();
        }

        @Test
        void shouldGroupSessionsByDate() {
            var from = LocalDate.of(2026, 3, 1);
            var to = LocalDate.of(2026, 3, 31);

            var s1 = paidSession(1L, euroStudent, LocalDate.of(2026, 3, 5),
                    new BigDecimal("30.00"), Currency.EUROS);
            var s2 = paidSession(2L, euroStudent, LocalDate.of(2026, 3, 5),
                    new BigDecimal("30.00"), Currency.EUROS);
            var s3 = paidSession(3L, euroStudent, LocalDate.of(2026, 3, 10),
                    new BigDecimal("35.00"), Currency.EUROS);

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of(s1, s2, s3));

            var result = earningsService.getDailyEarnings(from, to, null);

            assertThat(result.getDailyBreakdown()).hasSize(2);
            var daily = result.getDailyBreakdown();
            // March 5 — 2 sessions, 60 EUR total
            assertThat(daily.get(0).getDate()).isEqualTo(LocalDate.of(2026, 3, 5));
            assertThat(daily.get(0).getSessionCount()).isEqualTo(2);
            assertThat(daily.get(0).getEarningsByCurrency().get(Currency.EUROS))
                    .isEqualByComparingTo("60.00");
            // March 10 — 1 session, 35 EUR
            assertThat(daily.get(1).getDate()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(daily.get(1).getSessionCount()).isEqualTo(1);
        }

        @Test
        void shouldGroupMultipleCurrenciesOnSameDay() {
            var from = LocalDate.of(2026, 3, 1);
            var to = LocalDate.of(2026, 3, 31);
            var date = LocalDate.of(2026, 3, 5);

            var euroSession = paidSession(1L, euroStudent, date,
                    new BigDecimal("30.00"), Currency.EUROS);
            var rubSession = paidSession(2L, rubleStudent, date,
                    new BigDecimal("2000.00"), Currency.RUBLES);

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of(euroSession, rubSession));

            var result = earningsService.getDailyEarnings(from, to, null);

            assertThat(result.getDailyBreakdown()).hasSize(1);
            var day = result.getDailyBreakdown().get(0);
            assertThat(day.getEarningsByCurrency()).containsKey(Currency.EUROS);
            assertThat(day.getEarningsByCurrency()).containsKey(Currency.RUBLES);
            assertThat(day.getEarningsByCurrency().get(Currency.EUROS))
                    .isEqualByComparingTo("30.00");
            assertThat(day.getEarningsByCurrency().get(Currency.RUBLES))
                    .isEqualByComparingTo("2000.00");
        }

        @Test
        void shouldComputeTotalInBaseCurrency() {
            var from = LocalDate.of(2026, 3, 1);
            var to = LocalDate.of(2026, 3, 31);
            var date = LocalDate.of(2026, 3, 5);

            var rubSession = paidSession(1L, rubleStudent, date,
                    new BigDecimal("2000.00"), Currency.RUBLES);

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of(rubSession));
            when(currencyConversionService.convertToAll(new BigDecimal("2000.00"), Currency.RUBLES))
                    .thenReturn(Map.of(
                            Currency.RUBLES, new BigDecimal("2000.00"),
                            Currency.EUROS, new BigDecimal("19.80"),
                            Currency.DOLLARS, new BigDecimal("21.60")
                    ));

            var result = earningsService.getDailyEarnings(from, to, Currency.EUROS);

            assertThat(result.getDailyBreakdown()).hasSize(1);
            assertThat(result.getDailyBreakdown().get(0).getTotalInBaseCurrency()).isEqualByComparingTo("19.80");
            assertThat(result.getDailyBreakdown().get(0).getBaseCurrency()).isEqualTo(Currency.EUROS);
        }

        @Test
        void shouldNotSetTotalInBaseCurrency_whenNotRequested() {
            var from = LocalDate.of(2026, 3, 1);
            var to = LocalDate.of(2026, 3, 31);

            var s1 = paidSession(1L, euroStudent, LocalDate.of(2026, 3, 5),
                    new BigDecimal("30.00"), Currency.EUROS);

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of(s1));

            var result = earningsService.getDailyEarnings(from, to, null);

            assertThat(result.getDailyBreakdown().get(0).getTotalInBaseCurrency()).isNull();
        }

        @Test
        void shouldReturnDatesInOrder() {
            var from = LocalDate.of(2026, 3, 1);
            var to = LocalDate.of(2026, 3, 31);

            var s1 = paidSession(1L, euroStudent, LocalDate.of(2026, 3, 20),
                    new BigDecimal("30.00"), Currency.EUROS);
            var s2 = paidSession(2L, euroStudent, LocalDate.of(2026, 3, 5),
                    new BigDecimal("30.00"), Currency.EUROS);

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of(s1, s2));

            var result = earningsService.getDailyEarnings(from, to, null);

            assertThat(result.getDailyBreakdown().get(0).getDate())
                    .isBefore(result.getDailyBreakdown().get(1).getDate());
        }

        @Test
        void shouldIncludePackagesInPeriodTotalsWhenPaymentDateInRange() {
            var from = LocalDate.of(2026, 3, 1);
            var to = LocalDate.of(2026, 3, 31);

            var paid = paidSession(1L, euroStudent, LocalDate.of(2026, 3, 5),
                    new BigDecimal("30.00"), Currency.EUROS);
            var unpaidCollectible = ClassSession.builder()
                    .id(2L).student(euroStudent)
                    .classDate(LocalDate.of(2026, 3, 7))
                    .startTime(LocalTime.of(11, 0))
                    .durationMinutes(60)
                    .priceCharged(new BigDecimal("20.00"))
                    .currency(Currency.EUROS)
                    .build();
            unpaidCollectible.setPaymentStatus(PaymentStatus.UNPAID);

            var cancelledPotential = ClassSession.builder()
                    .id(3L).student(euroStudent)
                    .classDate(LocalDate.of(2026, 3, 8))
                    .startTime(LocalTime.of(12, 0))
                    .durationMinutes(60)
                    .priceCharged(new BigDecimal("15.00"))
                    .currency(Currency.EUROS)
                    .build();
            cancelledPotential.setPaymentStatus(PaymentStatus.UNPAID);
            cancelledPotential.setStatus(com.studio.app.enums.ClassStatus.CANCELLED);

            var pkg = PackagePurchase.builder()
                    .id(10L).student(rubleStudent)
                    .amountPaid(new BigDecimal("15000.00"))
                    .currency(Currency.RUBLES)
                    .paymentDate(LocalDate.of(2026, 3, 10))
                    .totalClasses(10).classesRemaining(10)
                    .build();

            when(sessionRepository.findPaidSessionsByDateRange(from, to)).thenReturn(List.of(paid));
            when(sessionRepository.findCollectiblePerClassSessionsByDateRange(from, to))
                    .thenReturn(List.of(paid, unpaidCollectible));
            when(sessionRepository.findPotentialPerClassSessionsIncludingCancellationsByDateRange(from, to))
                    .thenReturn(List.of(paid, unpaidCollectible, cancelledPotential));
            when(packageRepository.findByPaymentDateRange(from, to)).thenReturn(List.of(pkg));

            var result = earningsService.getDailyEarnings(from, to, null);

            assertThat(result.getTotalEarnedByCurrency().get(Currency.EUROS)).isEqualByComparingTo("30.00");
            assertThat(result.getTotalEarnedByCurrency().get(Currency.RUBLES)).isEqualByComparingTo("15000.00");

            assertThat(result.getTotalCouldHaveEarnedExcludingCancellationsByCurrency().get(Currency.EUROS))
                    .isEqualByComparingTo("50.00");
            assertThat(result.getTotalCouldHaveEarnedExcludingCancellationsByCurrency().get(Currency.RUBLES))
                    .isEqualByComparingTo("15000.00");

            assertThat(result.getTotalCouldHaveEarnedIncludingCancellationsByCurrency().get(Currency.EUROS))
                    .isEqualByComparingTo("65.00");
            assertThat(result.getTotalCouldHaveEarnedIncludingCancellationsByCurrency().get(Currency.RUBLES))
                    .isEqualByComparingTo("15000.00");

            // Daily breakdown remains per-class paid sessions only.
            assertThat(result.getDailyBreakdown()).hasSize(1);
            assertThat(result.getDailyBreakdown().get(0).getEarningsByCurrency().containsKey(Currency.RUBLES))
                    .isFalse();
        }
    }

    @Nested
    class MonthlyEarnings {

        @Test
        void shouldIncludeBothSessionsAndPackages() {
            var month = YearMonth.of(2026, 3);
            var from = month.atDay(1);
            var to = month.atEndOfMonth();

            // 1 paid session
            var s1 = paidSession(1L, euroStudent, LocalDate.of(2026, 3, 5),
                    new BigDecimal("30.00"), Currency.EUROS);

            // 1 package purchase
            var pkg = PackagePurchase.builder()
                    .id(10L).student(rubleStudent)
                    .totalClasses(10).classesRemaining(10)
                    .amountPaid(new BigDecimal("15000.00"))
                    .currency(Currency.RUBLES)
                    .paymentDate(LocalDate.of(2026, 3, 10))
                    .build();

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of(s1));
            when(packageRepository.findByPaymentDateRange(from, to))
                    .thenReturn(List.of(pkg));

            var result = earningsService.getMonthlyEarnings(month, null);

            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonth()).isEqualTo(3);
            assertThat(result.getTotalSessionCount()).isEqualTo(1);
            assertThat(result.getTotalPackageCount()).isEqualTo(1);

            // Session earnings in EUR
            assertThat(result.getSessionEarningsByCurrency().get(Currency.EUROS))
                    .isEqualByComparingTo("30.00");

            // Package earnings in RUB
            assertThat(result.getPackageEarningsByCurrency().get(Currency.RUBLES))
                    .isEqualByComparingTo("15000.00");

            // Combined totals
            assertThat(result.getTotalEarningsByCurrency()).containsKey(Currency.EUROS);
            assertThat(result.getTotalEarningsByCurrency()).containsKey(Currency.RUBLES);
        }

        @Test
        void shouldReturnZerosWhenNoEarnings() {
            var month = YearMonth.of(2026, 3);
            var from = month.atDay(1);
            var to = month.atEndOfMonth();

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of());
            when(packageRepository.findByPaymentDateRange(from, to))
                    .thenReturn(List.of());

            var result = earningsService.getMonthlyEarnings(month, null);

            assertThat(result.getTotalSessionCount()).isZero();
            assertThat(result.getTotalPackageCount()).isZero();
            assertThat(result.getSessionEarningsByCurrency()).isEmpty();
            assertThat(result.getPackageEarningsByCurrency()).isEmpty();
            assertThat(result.getDailyBreakdown()).isEmpty();
        }

        @Test
        void shouldComputeBaseCurrencyTotal() {
            var month = YearMonth.of(2026, 3);
            var from = month.atDay(1);
            var to = month.atEndOfMonth();

            var s1 = paidSession(1L, rubleStudent, LocalDate.of(2026, 3, 5),
                    new BigDecimal("2000.00"), Currency.RUBLES);

            var pkg = PackagePurchase.builder()
                    .id(10L).student(euroStudent)
                    .totalClasses(5).classesRemaining(5)
                    .amountPaid(new BigDecimal("100.00"))
                    .currency(Currency.EUROS)
                    .paymentDate(LocalDate.of(2026, 3, 10))
                    .build();

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of(s1));
            when(packageRepository.findByPaymentDateRange(from, to))
                    .thenReturn(List.of(pkg));

            // For daily: converting RUB to USD
            when(currencyConversionService.convertToAll(new BigDecimal("2000.00"), Currency.RUBLES))
                    .thenReturn(Map.of(
                            Currency.RUBLES, new BigDecimal("2000.00"),
                            Currency.DOLLARS, new BigDecimal("21.60"),
                            Currency.EUROS, new BigDecimal("19.80")
                    ));
            // For monthly combined: converting EUR to USD
            when(currencyConversionService.convertToAll(new BigDecimal("100.00"), Currency.EUROS))
                    .thenReturn(Map.of(
                            Currency.EUROS, new BigDecimal("100.00"),
                            Currency.DOLLARS, new BigDecimal("108.00"),
                            Currency.RUBLES, new BigDecimal("10100.00")
                    ));

            var result = earningsService.getMonthlyEarnings(month, Currency.DOLLARS);

            assertThat(result.getBaseCurrency()).isEqualTo(Currency.DOLLARS);
            assertThat(result.getTotalInBaseCurrency()).isNotNull();
        }

        @Test
        void shouldIncludeMultiplePackagesInSameMonth() {
            var month = YearMonth.of(2026, 3);
            var from = month.atDay(1);
            var to = month.atEndOfMonth();

            var pkg1 = PackagePurchase.builder()
                    .id(10L).student(rubleStudent)
                    .totalClasses(5).classesRemaining(5)
                    .amountPaid(new BigDecimal("8000.00"))
                    .currency(Currency.RUBLES)
                    .paymentDate(LocalDate.of(2026, 3, 1))
                    .build();

            var pkg2 = PackagePurchase.builder()
                    .id(11L).student(rubleStudent)
                    .totalClasses(10).classesRemaining(10)
                    .amountPaid(new BigDecimal("14000.00"))
                    .currency(Currency.RUBLES)
                    .paymentDate(LocalDate.of(2026, 3, 15))
                    .build();

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of());
            when(packageRepository.findByPaymentDateRange(from, to))
                    .thenReturn(List.of(pkg1, pkg2));

            var result = earningsService.getMonthlyEarnings(month, null);

            assertThat(result.getTotalPackageCount()).isEqualTo(2);
            assertThat(result.getPackageEarningsByCurrency().get(Currency.RUBLES))
                    .isEqualByComparingTo("22000.00");
        }

        @Test
        void shouldIncludeDailyBreakdown() {
            var month = YearMonth.of(2026, 3);
            var from = month.atDay(1);
            var to = month.atEndOfMonth();

            var s1 = paidSession(1L, euroStudent, LocalDate.of(2026, 3, 5),
                    new BigDecimal("30.00"), Currency.EUROS);
            var s2 = paidSession(2L, euroStudent, LocalDate.of(2026, 3, 12),
                    new BigDecimal("30.00"), Currency.EUROS);

            when(sessionRepository.findPaidSessionsByDateRange(from, to))
                    .thenReturn(List.of(s1, s2));
            when(packageRepository.findByPaymentDateRange(from, to))
                    .thenReturn(List.of());

            var result = earningsService.getMonthlyEarnings(month, null);

            assertThat(result.getDailyBreakdown()).hasSize(2);
        }
    }
}

