package com.studio.app.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class EarningsPaymentsNoSeedIT extends BaseIntegrationNoSeedTest {

    @Test
    void shouldReturnSinglePaymentAfterTwoPackagePaidSessions() throws Exception {
        MvcResult studentResult = mockMvc.perform(post("/api/students")
                        .contentType(JSON)
                        .content("""
                                {
                                  "firstName": "Package",
                                  "lastName": "Student",
                                  "pricingType": "PACKAGE",
                                  "timezone": "SPAIN",
                                  "classType": "CASUAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Number studentId = JsonPath.read(studentResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/students/{id}/schedules", studentId.longValue())
                        .contentType(JSON)
                        .content("""
                                {
                                  "dayOfWeek": "MONDAY",
                                  "startTime": "10:00",
                                  "durationMinutes": 60
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/students/{id}/schedules", studentId.longValue())
                        .contentType(JSON)
                        .content("""
                                {
                                  "dayOfWeek": "WEDNESDAY",
                                  "startTime": "10:00",
                                  "durationMinutes": 60
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult purchaseResult = mockMvc.perform(post("/api/students/{id}/packages", studentId.longValue())
                        .contentType(JSON)
                        .content("""
                                {
                                  "totalClasses": 4,
                                  "amountPaid": 200.00,
                                  "currency": "EUROS",
                                  "paymentDate": "2026-04-01",
                                  "description": "4-class package"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Number packageId = JsonPath.read(purchaseResult.getResponse().getContentAsString(), "$.id");

        MvcResult firstSessionResult = mockMvc.perform(post("/api/students/{id}/sessions", studentId.longValue())
                        .contentType(JSON)
                        .content("""
                                {
                                  "classDate": "2026-04-06",
                                  "startTime": "10:00",
                                  "durationMinutes": 60,
                                  "note": "Week class 1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Number firstSessionId = JsonPath.read(firstSessionResult.getResponse().getContentAsString(), "$.id");

        MvcResult secondSessionResult = mockMvc.perform(post("/api/students/{id}/sessions", studentId.longValue())
                        .contentType(JSON)
                        .content("""
                                {
                                  "classDate": "2026-04-08",
                                  "startTime": "10:00",
                                  "durationMinutes": 60,
                                  "note": "Week class 2"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Number secondSessionId = JsonPath.read(secondSessionResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/sessions/{id}/completion", firstSessionId.longValue())
                        .param("completed", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sessions/{id}/completion", secondSessionId.longValue())
                        .param("completed", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sessions/{id}/pay", firstSessionId.longValue())
                        .contentType(JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PACKAGE"));

        mockMvc.perform(post("/api/sessions/{id}/pay", secondSessionId.longValue())
                        .contentType(JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PACKAGE"));

        mockMvc.perform(get("/api/packages/{id}", packageId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClasses").value(4))
                .andExpect(jsonPath("$.classesRemaining").value(2));

        mockMvc.perform(get("/api/earnings/payments")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].paymentType").value("PACKAGE"))
                .andExpect(jsonPath("$.content[0].packagePurchaseId").value(packageId.longValue()))
                .andExpect(jsonPath("$.content[0].sessionId", nullValue()));
    }
}

