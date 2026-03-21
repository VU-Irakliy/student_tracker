package com.studio.app.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class SessionControllerIT extends BaseIntegrationTest {

    // ── StudentSessionController ────────────────────────────────────────────

    @Nested
    class CreateOneOffSession {

        @Test
        void shouldCreateAndReturn201() throws Exception {
            mockMvc.perform(post("/api/students/1/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-20",
                                      "startTime": "14:00",
                                      "durationMinutes": 45,
                                      "note": "Extra class"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.studentId").value(1))
                    .andExpect(jsonPath("$.classDate").value("2026-03-20"))
                    .andExpect(jsonPath("$.durationMinutes").value(45))
                    .andExpect(jsonPath("$.oneOff").value(true))
                    .andExpect(jsonPath("$.note").value("Extra class"))
                    .andExpect(jsonPath("$.timezone").value("SPAIN"))
                    .andExpect(jsonPath("$.currency").value("EUROS"))
                    .andExpect(jsonPath("$.priceCharged").value(30.00))
                    .andExpect(jsonPath("$.convertedPrices").isMap());
        }

        @Test
        void shouldReturn404ForNonExistentStudent() throws Exception {
            mockMvc.perform(post("/api/students/999/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-20",
                                      "startTime": "14:00",
                                      "durationMinutes": 60
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn400ForMissingDate() throws Exception {
            mockMvc.perform(post("/api/students/1/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "startTime": "14:00",
                                      "durationMinutes": 60
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenClassDateIsBeforeStudentStartDate() throws Exception {
            mockMvc.perform(patch("/api/students/1")
                            .contentType(JSON)
                            .content("""
                                    { "startDate": "2026-03-15" }
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/students/1/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-10",
                                      "startTime": "14:00",
                                      "durationMinutes": 60
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("startDate")));
        }

        @Test
        void shouldReturn400WhenStudentIsOnHoliday() throws Exception {
            mockMvc.perform(patch("/api/students/1")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "holidayMode": true,
                                      "holidayFrom": "2026-03-10"
                                    }
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/students/1/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-20",
                                      "startTime": "14:00",
                                      "durationMinutes": 60
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("holiday")));
        }

        @Test
        void shouldReturn400WhenStudentStoppedAttending() throws Exception {
            mockMvc.perform(patch("/api/students/1")
                            .contentType(JSON)
                            .content("""
                                    { "stoppedAttending": true }
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/students/1/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-20",
                                      "startTime": "14:00",
                                      "durationMinutes": 60
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("stopped attending")));
        }
    }

    @Nested
    class GetSessionsForStudent {

        @Test
        void shouldReturnAllSessionsForStudent() throws Exception {
            mockMvc.perform(get("/api/students/1/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].studentId").value(1));
        }

        @Test
        void shouldFilterByDateRange() throws Exception {
            mockMvc.perform(get("/api/students/1/sessions")
                            .param("from", "2026-03-01")
                            .param("to", "2026-03-10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].classDate").value("2026-03-02"));
        }
    }

    @Nested
    class GetSessionsByPaymentStatus {

        @Test
        void shouldReturnPaidSessions() throws Exception {
            mockMvc.perform(get("/api/students/1/sessions/by-payment")
                            .param("paymentStatus", "PAID"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].paymentStatus").value("PAID"));
        }

        @Test
        void shouldReturnUnpaidSessions() throws Exception {
            mockMvc.perform(get("/api/students/1/sessions/by-payment")
                            .param("paymentStatus", "UNPAID"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    // ── SessionController (single-session ops) ──────────────────────────────

    @Nested
    class GetSessionById {

        @Test
        void shouldReturnSessionWithConvertedPrices() throws Exception {
            mockMvc.perform(get("/api/sessions/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.priceCharged").value(30.00))
                    .andExpect(jsonPath("$.currency").value("EUROS"))
                    .andExpect(jsonPath("$.timezone").value("SPAIN"))
                    .andExpect(jsonPath("$.convertedPrices.EUROS").isNumber())
                    .andExpect(jsonPath("$.convertedPrices.DOLLARS").isNumber())
                    .andExpect(jsonPath("$.convertedPrices.RUBLES").isNumber());
        }

        @Test
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/sessions/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdateSession {

        @Test
        void shouldUpdateInSingleEndpoint() throws Exception {
            mockMvc.perform(put("/api/sessions/2")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-18",
                                      "startTime": "15:30",
                                      "durationMinutes": 90,
                                      "status": "COMPLETED",
                                      "paid": true,
                                      "paymentDateTime": "2026-03-18T18:00:00",
                                      "amountOverride": 35.00,
                                      "note": "Conducted and paid"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classDate").value("2026-03-18"))
                    .andExpect(jsonPath("$.startTime").value("15:30:00"))
                    .andExpect(jsonPath("$.durationMinutes").value(90))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                    .andExpect(jsonPath("$.paymentDateTime").value("2026-03-18T18:00:00"))
                    .andExpect(jsonPath("$.priceCharged").value(35.00))
                    .andExpect(jsonPath("$.note").value("Conducted and paid"));
        }
    }

    @Nested
    class MarkPaid {

        @Test
        void shouldMarkPerClassSessionAsPaid() throws Exception {
            // Session 2 is UNPAID for student 1 (PER_CLASS)
            mockMvc.perform(post("/api/sessions/2/pay")
                            .contentType(JSON)
                            .content("""
                                    { "paymentDateTime": "2026-03-12T19:15:00" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                    .andExpect(jsonPath("$.paymentDateTime").value("2026-03-12T19:15:00"));
        }

        @Test
        void shouldApplyAmountOverride() throws Exception {
            mockMvc.perform(post("/api/sessions/2/pay")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "paymentDateTime": "2026-03-12T20:00:00",
                                      "amountOverride": 25.00
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                    .andExpect(jsonPath("$.priceCharged").value(25.00))
                    .andExpect(jsonPath("$.paymentDateTime").value("2026-03-12T20:00:00"));
        }

        @Test
        void shouldReturn400WhenAlreadyPaid() throws Exception {
            // Session 1 is already PAID
            mockMvc.perform(post("/api/sessions/1/pay")
                            .contentType(JSON)
                            .content("""
                                    { "paymentDateTime": "2026-03-13T10:00:00" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400WhenPaymentDateTimeMissing() throws Exception {
            mockMvc.perform(post("/api/sessions/2/pay")
                            .contentType(JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class CompleteIncompleted {

        @Test
        void shouldMarkCompleted() throws Exception {
            mockMvc.perform(post("/api/sessions/2/completion")
                            .param("completed", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void shouldMarkIncompletedToScheduled() throws Exception {
            mockMvc.perform(post("/api/sessions/2/completion")
                            .param("completed", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            mockMvc.perform(post("/api/sessions/2/completion")
                            .param("completed", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SCHEDULED"));
        }
    }

    @Nested
    class CancelSession {

        @Test
        void shouldCancelAndKeepPaid() throws Exception {
            mockMvc.perform(post("/api/sessions/1/cancel")
                            .contentType(JSON)
                            .content("""
                                    { "keepAsPaid": true }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        }

        @Test
        void shouldCancelAndRevertToUnpaid() throws Exception {
            mockMvc.perform(post("/api/sessions/1/cancel")
                            .contentType(JSON)
                            .content("""
                                    { "keepAsPaid": false }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
        }

        @Test
        void shouldCancelWithNote() throws Exception {
            mockMvc.perform(post("/api/sessions/2/cancel")
                            .contentType(JSON)
                            .content("""
                                    { "keepAsPaid": false, "note": "Student sick" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.note").value("Student sick"));
        }
    }

    @Nested
    class CancelPayment {

        @Test
        void shouldRevertPaidToUnpaid() throws Exception {
            mockMvc.perform(post("/api/sessions/1/cancel-payment"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
        }

        @Test
        void shouldReturn400WhenAlreadyUnpaid() throws Exception {
            mockMvc.perform(post("/api/sessions/2/cancel-payment"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ReassignPaymentFlow {

        @Test
        void shouldReassignPerClassPaymentUsingCancelPaymentAndPay() throws Exception {
            MvcResult createResult = mockMvc.perform(post("/api/students/1/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-20",
                                      "startTime": "14:00",
                                      "durationMinutes": 60,
                                      "note": "Moved from session 1"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                    .andReturn();

            Number targetSessionId = JsonPath.read(
                    createResult.getResponse().getContentAsString(), "$.id");

            mockMvc.perform(post("/api/sessions/1/cancel-payment"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));

            mockMvc.perform(post("/api/sessions/{id}/pay", targetSessionId.longValue())
                            .contentType(JSON)
                            .content("""
                                    {
                                      "paymentDateTime": "2026-03-20T18:45:00",
                                      "amountOverride": 30.00
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                    .andExpect(jsonPath("$.priceCharged").value(30.00));
        }

        @Test
        void shouldReassignPackageSlotUsingCancelPaymentAndPay() throws Exception {
            MvcResult firstSessionResult = mockMvc.perform(post("/api/students/2/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-20",
                                      "startTime": "14:00",
                                      "durationMinutes": 45,
                                      "note": "Original package session"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn();

            Number sourceSessionId = JsonPath.read(
                    firstSessionResult.getResponse().getContentAsString(), "$.id");

            MvcResult secondSessionResult = mockMvc.perform(post("/api/students/2/sessions")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "classDate": "2026-03-21",
                                      "startTime": "14:00",
                                      "durationMinutes": 45,
                                      "note": "Moved package session"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn();

            Number targetSessionId = JsonPath.read(
                    secondSessionResult.getResponse().getContentAsString(), "$.id");

            mockMvc.perform(post("/api/sessions/{id}/pay", sourceSessionId.longValue())
                            .contentType(JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("PACKAGE"));

            mockMvc.perform(get("/api/packages/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classesRemaining").value(7));

            mockMvc.perform(post("/api/sessions/{id}/cancel-payment", sourceSessionId.longValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));

            mockMvc.perform(get("/api/packages/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classesRemaining").value(8));

            mockMvc.perform(post("/api/sessions/{id}/pay", targetSessionId.longValue())
                            .contentType(JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("PACKAGE"));

            mockMvc.perform(get("/api/packages/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classesRemaining").value(7));
        }
    }
    
}

