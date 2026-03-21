package com.studio.app.integration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class EarningsControllerIT extends BaseIntegrationTest {

    @Nested
    class PaymentsFeed {

        @Test
        void shouldReturnPaginatedPaymentsAcrossSessionsAndPackages() throws Exception {
            mockMvc.perform(get("/api/earnings/payments")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.content[0].paymentType").value("SESSION"))
                    .andExpect(jsonPath("$.content[0].sessionId").isNumber())
                    .andExpect(jsonPath("$.content[0].paymentDateTime").value("2026-03-10T17:00:00"));
        }

        @Test
        void shouldIncludePackagePurchasesInPaymentsFeed() throws Exception {
            mockMvc.perform(get("/api/earnings/payments")
                            .param("page", "2")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].paymentType").value("PACKAGE"))
                    .andExpect(jsonPath("$.content[0].packagePurchaseId").isNumber())
                    .andExpect(jsonPath("$.content[0].sessionId").isEmpty());
        }
    }

    @Nested
    class DailyEarnings {

        @Test
        void shouldReturnDailyEarningsForRange() throws Exception {
            // Test data has paid sessions on 2026-03-02 (EUR), 2026-03-05 (USD), 2026-03-10 (USD)
            mockMvc.perform(get("/api/earnings/daily")
                            .param("from", "2026-03-01")
                            .param("to", "2026-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.from").value("2026-03-01"))
                    .andExpect(jsonPath("$.to").value("2026-03-31"))
                    .andExpect(jsonPath("$.dailyBreakdown", hasSize(3)))
                    .andExpect(jsonPath("$.dailyBreakdown[0].date").value("2026-03-02"))
                    .andExpect(jsonPath("$.dailyBreakdown[0].sessionCount").value(1))
                    .andExpect(jsonPath("$.dailyBreakdown[0].earningsByCurrency.EUROS").value(30.00))
                    .andExpect(jsonPath("$.dailyBreakdown[1].date").value("2026-03-05"))
                    .andExpect(jsonPath("$.dailyBreakdown[1].earningsByCurrency.DOLLARS").value(40.00))
                    .andExpect(jsonPath("$.dailyBreakdown[2].date").value("2026-03-10"))
                    .andExpect(jsonPath("$.dailyBreakdown[2].earningsByCurrency.DOLLARS").value(40.00))
                    .andExpect(jsonPath("$.totalEarnedByCurrency.EUROS").value(30.00))
                    .andExpect(jsonPath("$.totalEarnedByCurrency.DOLLARS").value(80.00))
                    .andExpect(jsonPath("$.totalEarnedByCurrency.RUBLES").value(15000.00));
        }

        @Test
        void shouldComputeBaseCurrencyTotal() throws Exception {
            mockMvc.perform(get("/api/earnings/daily")
                            .param("from", "2026-03-01")
                            .param("to", "2026-03-31")
                            .param("baseCurrency", "EUROS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.baseCurrency").value("EUROS"))
                    .andExpect(jsonPath("$.totalEarnedInBaseCurrency").isNumber())
                    .andExpect(jsonPath("$.totalCouldHaveEarnedExcludingCancellationsInBaseCurrency").isNumber())
                    .andExpect(jsonPath("$.totalCouldHaveEarnedIncludingCancellationsInBaseCurrency").isNumber());
        }

        @Test
        void shouldReturnEmptyForRangeWithNoPaidSessions() throws Exception {
            mockMvc.perform(get("/api/earnings/daily")
                            .param("from", "2026-06-01")
                            .param("to", "2026-06-30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyBreakdown", hasSize(0)))
                    .andExpect(jsonPath("$.totalEarnedByCurrency").isMap())
                    .andExpect(jsonPath("$.totalCouldHaveEarnedExcludingCancellationsByCurrency").isMap())
                    .andExpect(jsonPath("$.totalCouldHaveEarnedIncludingCancellationsByCurrency").isMap());
        }

        @Test
        void shouldIncludeConvertedTotals() throws Exception {
            mockMvc.perform(get("/api/earnings/daily")
                            .param("from", "2026-03-02")
                            .param("to", "2026-03-02"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyBreakdown", hasSize(1)))
                    .andExpect(jsonPath("$.dailyBreakdown[0].convertedTotals.EUROS").isNumber())
                    .andExpect(jsonPath("$.dailyBreakdown[0].convertedTotals.DOLLARS").isNumber())
                    .andExpect(jsonPath("$.dailyBreakdown[0].convertedTotals.RUBLES").isNumber())
                    .andExpect(jsonPath("$.convertedTotalEarned.EUROS").isNumber())
                    .andExpect(jsonPath("$.convertedTotalCouldHaveEarnedExcludingCancellations.EUROS").isNumber())
                    .andExpect(jsonPath("$.convertedTotalCouldHaveEarnedIncludingCancellations.EUROS").isNumber());
        }
    }

    @Nested
    class MonthlyEarnings {

        @Test
        void shouldIncludeBothSessionsAndPackages() throws Exception {
            // March 2026: 3 paid sessions + 1 package purchase (15000 RUB)
            mockMvc.perform(get("/api/earnings/monthly")
                            .param("year", "2026")
                            .param("month", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(2026))
                    .andExpect(jsonPath("$.month").value(3))
                    .andExpect(jsonPath("$.totalSessionCount").value(3))
                    .andExpect(jsonPath("$.totalPackageCount").value(1))
                    .andExpect(jsonPath("$.sessionEarningsByCurrency").isMap())
                    .andExpect(jsonPath("$.packageEarningsByCurrency.RUBLES").value(15000.00))
                    .andExpect(jsonPath("$.totalEarningsByCurrency").isMap())
                    .andExpect(jsonPath("$.dailyBreakdown").isArray())
                    .andExpect(jsonPath("$.dailyBreakdown", hasSize(3)));
        }

        @Test
        void shouldComputeBaseCurrencyTotal() throws Exception {
            mockMvc.perform(get("/api/earnings/monthly")
                            .param("year", "2026")
                            .param("month", "3")
                            .param("baseCurrency", "RUBLES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.baseCurrency").value("RUBLES"))
                    .andExpect(jsonPath("$.totalInBaseCurrency").isNumber());
        }

        @Test
        void shouldReturnConvertedTotals() throws Exception {
            mockMvc.perform(get("/api/earnings/monthly")
                            .param("year", "2026")
                            .param("month", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.convertedTotals.EUROS").isNumber())
                    .andExpect(jsonPath("$.convertedTotals.DOLLARS").isNumber())
                    .andExpect(jsonPath("$.convertedTotals.RUBLES").isNumber());
        }

        @Test
        void shouldReturnZerosForMonthWithNoActivity() throws Exception {
            mockMvc.perform(get("/api/earnings/monthly")
                            .param("year", "2026")
                            .param("month", "6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalSessionCount").value(0))
                    .andExpect(jsonPath("$.totalPackageCount").value(0))
                    .andExpect(jsonPath("$.dailyBreakdown", hasSize(0)));
        }

        @Test
        void shouldIncludePackageFromFebruary() throws Exception {
            // February 2026: 1 exhausted package (8000 RUB), no paid sessions
            mockMvc.perform(get("/api/earnings/monthly")
                            .param("year", "2026")
                            .param("month", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalSessionCount").value(0))
                    .andExpect(jsonPath("$.totalPackageCount").value(1))
                    .andExpect(jsonPath("$.packageEarningsByCurrency.RUBLES").value(8000.00));
        }
    }
}

