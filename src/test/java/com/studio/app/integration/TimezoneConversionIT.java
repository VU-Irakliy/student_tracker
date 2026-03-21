package com.studio.app.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class TimezoneConversionIT extends BaseIntegrationNoSeedTest {

    @Test
    @Sql(scripts = "/testdata/timezone/standard/russia_to_spain.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldConvertRussiaToSpain_withoutDst() throws Exception {
        mockMvc.perform(get("/api/sessions/1").param("timezone", "SPAIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalTimezone").value("RUSSIA_MOSCOW"))
                .andExpect(jsonPath("$.viewerTimezone").value("SPAIN"))
                .andExpect(jsonPath("$.originalClassDate").value("2026-01-15"))
                .andExpect(jsonPath("$.originalStartTime").value("10:00:00"))
                .andExpect(jsonPath("$.classDate").value("2026-01-15"))
                .andExpect(jsonPath("$.startTime").value("08:00:00"));
    }

    @Test
    @Sql(scripts = "/testdata/timezone/standard/spain_to_russia.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldConvertSpainToRussia_withoutDst() throws Exception {
        mockMvc.perform(get("/api/sessions/1").param("timezone", "RUSSIA_MOSCOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalTimezone").value("SPAIN"))
                .andExpect(jsonPath("$.viewerTimezone").value("RUSSIA_MOSCOW"))
                .andExpect(jsonPath("$.originalClassDate").value("2026-01-15"))
                .andExpect(jsonPath("$.originalStartTime").value("10:00:00"))
                .andExpect(jsonPath("$.classDate").value("2026-01-15"))
                .andExpect(jsonPath("$.startTime").value("12:00:00"));
    }

    @Test
    @Sql(scripts = "/testdata/timezone/dst/russia_to_spain.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldConvertRussiaToSpain_withDst() throws Exception {
        mockMvc.perform(get("/api/sessions/1").param("timezone", "SPAIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalTimezone").value("RUSSIA_MOSCOW"))
                .andExpect(jsonPath("$.viewerTimezone").value("SPAIN"))
                .andExpect(jsonPath("$.originalClassDate").value("2026-06-15"))
                .andExpect(jsonPath("$.originalStartTime").value("10:00:00"))
                .andExpect(jsonPath("$.classDate").value("2026-06-15"))
                .andExpect(jsonPath("$.startTime").value("09:00:00"));
    }

    @Test
    @Sql(scripts = "/testdata/timezone/dst/spain_to_russia.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldConvertSpainToRussia_withDst() throws Exception {
        mockMvc.perform(get("/api/sessions/1").param("timezone", "RUSSIA_MOSCOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalTimezone").value("SPAIN"))
                .andExpect(jsonPath("$.viewerTimezone").value("RUSSIA_MOSCOW"))
                .andExpect(jsonPath("$.originalClassDate").value("2026-06-15"))
                .andExpect(jsonPath("$.originalStartTime").value("10:00:00"))
                .andExpect(jsonPath("$.classDate").value("2026-06-15"))
                .andExpect(jsonPath("$.startTime").value("11:00:00"));
    }
}

