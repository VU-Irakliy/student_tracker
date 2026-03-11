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
    class DailyEarnings {

        @Test
        void shouldReturnDailyEarningsForRange() throws Exception {
            // Test data has paid sessions on 2026-03-02 (EUR), 2026-03-05 (USD), 2026-03-10 (USD)
            mockMvc.perform(get("/api/earnings/daily")
                            .param("from", "2026-03-01")
                            .param("to", "2026-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].date").value("2026-03-02"))
                    .andExpect(jsonPath("$[0].sessionCount").value(1))
                    .andExpect(jsonPath("$[0].earningsByCurrency.EUROS").value(30.00))
                    .andExpect(jsonPath("$[1].date").value("2026-03-05"))
                    .andExpect(jsonPath("$[1].earningsByCurrency.DOLLARS").value(40.00))
                    .andExpect(jsonPath("$[2].date").value("2026-03-10"))
                    .andExpect(jsonPath("$[2].earningsByCurrency.DOLLARS").value(40.00));
        }

        @Test
        void shouldComputeBaseCurrencyTotal() throws Exception {
            mockMvc.perform(get("/api/earnings/daily")
                            .param("from", "2026-03-01")
                            .param("to", "2026-03-31")
                            .param("baseCurrency", "EUROS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].baseCurrency").value("EUROS"))
                    .andExpect(jsonPath("$[0].totalInBaseCurrency").isNumber());
        }

        @Test
        void shouldReturnEmptyForRangeWithNoPaidSessions() throws Exception {
            mockMvc.perform(get("/api/earnings/daily")
                            .param("from", "2026-06-01")
                            .param("to", "2026-06-30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void shouldIncludeConvertedTotals() throws Exception {
            mockMvc.perform(get("/api/earnings/daily")
                            .param("from", "2026-03-02")
                            .param("to", "2026-03-02"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].convertedTotals.EUROS").isNumber())
                    .andExpect(jsonPath("$[0].convertedTotals.DOLLARS").isNumber())
                    .andExpect(jsonPath("$[0].convertedTotals.RUBLES").isNumber());
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

