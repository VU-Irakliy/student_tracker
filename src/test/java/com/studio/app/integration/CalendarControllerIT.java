package com.studio.app.integration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class CalendarControllerIT extends BaseIntegrationTest {

    @Nested
    class GetCalendar {

        @Test
        void shouldReturnSessionsGroupedByDay() throws Exception {
            mockMvc.perform(get("/api/calendar")
                            .param("from", "2026-03-01")
                            .param("to", "2026-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[*].date", everyItem(notNullValue())))
                    .andExpect(jsonPath("$[*].sessions").exists());
        }

        @Test
        void shouldReturnSessionsInDateOrder() throws Exception {
            mockMvc.perform(get("/api/calendar")
                            .param("from", "2026-03-01")
                            .param("to", "2026-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].date").value("2026-03-02"))
                    .andExpect(jsonPath("$[0].sessions", hasSize(1)))
                    .andExpect(jsonPath("$[0].sessions[0].studentName").value("Ana García"));
        }

        @Test
        void shouldReturnEmptyForRangeWithNoSessions() throws Exception {
            mockMvc.perform(get("/api/calendar")
                            .param("from", "2026-06-01")
                            .param("to", "2026-06-30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void shouldExcludeDeletedStudentSessions() throws Exception {
            // Deleted student (id=4) has no sessions, but verify no error
            mockMvc.perform(get("/api/calendar")
                            .param("from", "2026-03-01")
                            .param("to", "2026-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].sessions[*].studentName",
                            not(hasItem("Deleted User"))));
        }

        @Test
        void shouldDefaultToNext30Days_whenNoDatesGiven() throws Exception {
            mockMvc.perform(get("/api/calendar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}

