package com.studio.app.integration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class StudentControllerIT extends BaseIntegrationTest {

    @Nested
    class GetAllStudents {

        @Test
        void shouldReturnOnlyActiveStudents() throws Exception {
            mockMvc.perform(get("/api/students"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[*].firstName", not(hasItem("Deleted"))));
        }

        @Test
        void shouldSearchByFirstName() throws Exception {
            mockMvc.perform(get("/api/students").param("search", "Ana"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].firstName").value("Ana"));
        }

        @Test
        void shouldSearchByLastName() throws Exception {
            mockMvc.perform(get("/api/students").param("search", "Petrov"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].lastName").value("Petrov"));
        }

        @Test
        void shouldSearchCaseInsensitive() throws Exception {
            mockMvc.perform(get("/api/students").param("search", "GARCÍA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void shouldReturnEmptyForNoMatch() throws Exception {
            mockMvc.perform(get("/api/students").param("search", "xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class GetStudentById {

        @Test
        void shouldReturnStudentWithConvertedPrices() throws Exception {
            mockMvc.perform(get("/api/students/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Ana"))
                    .andExpect(jsonPath("$.lastName").value("García"))
                    .andExpect(jsonPath("$.classType").value("EGE"))
                    .andExpect(jsonPath("$.currency").value("EUROS"))
                    .andExpect(jsonPath("$.pricePerClass").value(30.00))
                    .andExpect(jsonPath("$.convertedPrices.EUROS").isNumber())
                    .andExpect(jsonPath("$.convertedPrices.DOLLARS").isNumber())
                    .andExpect(jsonPath("$.convertedPrices.RUBLES").isNumber())
                    .andExpect(jsonPath("$.weeklySchedules", hasSize(2)))
                    .andExpect(jsonPath("$.payers", hasSize(1)));
        }

        @Test
        void shouldReturn404ForDeletedStudent() throws Exception {
            mockMvc.perform(get("/api/students/4"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn404ForNonExistentStudent() throws Exception {
            mockMvc.perform(get("/api/students/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class CreateStudent {

        @Test
        void shouldCreateAndReturn201() throws Exception {
            mockMvc.perform(post("/api/students")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "firstName": "New",
                                      "lastName": "Student",
                                      "pricingType": "PER_CLASS",
                                      "pricePerClass": 50.00,
                                      "currency": "DOLLARS",
                                      "timezone": "SPAIN",
                                      "classType": "TOFEL",
                                      "startDate": "2026-03-01",
                                      "stoppedAttending": false
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstName").value("New"))
                    .andExpect(jsonPath("$.classType").value("TOFEL"))
                    .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                    .andExpect(jsonPath("$.currency").value("DOLLARS"))
                    .andExpect(jsonPath("$.convertedPrices").isMap());
        }

        @Test
        void shouldReturn400_whenHolidayModeWithoutHolidayFrom() throws Exception {
            mockMvc.perform(post("/api/students")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "firstName": "X",
                                      "lastName": "Y",
                                      "pricingType": "PER_CLASS",
                                      "timezone": "SPAIN",
                                      "holidayMode": true
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("holidayFrom")));
        }

        @Test
        void shouldReturn400_whenFirstNameMissing() throws Exception {
            mockMvc.perform(post("/api/students")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "lastName": "Student",
                                      "pricingType": "PER_CLASS",
                                      "timezone": "SPAIN"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400_whenTimezoneNull() throws Exception {
            mockMvc.perform(post("/api/students")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "firstName": "X",
                                      "lastName": "Y",
                                      "pricingType": "PER_CLASS"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class UpdateStudent {

        @Test
        void shouldUpdateFirstName() throws Exception {
            mockMvc.perform(put("/api/students/1")
                            .contentType(JSON)
                            .content("""
                                    { "firstName": "Anita" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Anita"))
                    .andExpect(jsonPath("$.lastName").value("García"));
        }

        @Test
        void shouldUpdateCurrency() throws Exception {
            mockMvc.perform(put("/api/students/1")
                            .contentType(JSON)
                            .content("""
                                    { "currency": "DOLLARS" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("DOLLARS"));
        }

        @Test
        void shouldUpdatePricePerClass() throws Exception {
            mockMvc.perform(put("/api/students/1")
                            .contentType(JSON)
                            .content("""
                                    { "pricePerClass": 45.00 }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricePerClass").value(45.00));
        }

        @Test
        void shouldUpdateNotes() throws Exception {
            mockMvc.perform(put("/api/students/1")
                            .contentType(JSON)
                            .content("""
                                    { "notes": "Advanced level now" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("Advanced level now"));
        }

        @Test
        void shouldReturn404ForNonExistentStudent() throws Exception {
            mockMvc.perform(put("/api/students/999")
                            .contentType(JSON)
                            .content("""
                                    { "firstName": "X" }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteStudent {

        @Test
        void shouldReturn204() throws Exception {
            mockMvc.perform(delete("/api/students/1"))
                    .andExpect(status().isNoContent());

            // Verify soft-deleted
            mockMvc.perform(get("/api/students/1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn404ForNonExistentStudent() throws Exception {
            mockMvc.perform(delete("/api/students/999"))
                    .andExpect(status().isNotFound());
        }
    }
}

