package com.studio.app.integration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class PackageControllerIT extends BaseIntegrationTest {

    // ── StudentPackageController ────────────────────────────────────────────

    @Nested
    class PurchasePackage {

        @Test
        void shouldCreateAndReturn201() throws Exception {
            mockMvc.perform(post("/api/students/2/packages")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "totalClasses": 5,
                                      "amountPaid": 8000.00,
                                      "paymentDate": "2026-03-10",
                                      "description": "Mid-month top-up"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.studentId").value(2))
                    .andExpect(jsonPath("$.totalClasses").value(5))
                    .andExpect(jsonPath("$.classesRemaining").value(5))
                    .andExpect(jsonPath("$.amountPaid").value(8000.00))
                    .andExpect(jsonPath("$.currency").value("RUBLES"))
                    .andExpect(jsonPath("$.description").value("Mid-month top-up"))
                    .andExpect(jsonPath("$.convertedAmountPaid").isMap())
                    .andExpect(jsonPath("$.exhausted").value(false));
        }

        @Test
        void shouldUseCurrencyFromRequestWhenProvided() throws Exception {
            mockMvc.perform(post("/api/students/2/packages")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "totalClasses": 3,
                                      "amountPaid": 100.00,
                                      "currency": "DOLLARS",
                                      "paymentDate": "2026-03-12"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency").value("DOLLARS"));
        }

        @Test
        void shouldReturn404ForNonExistentStudent() throws Exception {
            mockMvc.perform(post("/api/students/999/packages")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "totalClasses": 5,
                                      "amountPaid": 100.00,
                                      "paymentDate": "2026-03-12"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn400_whenTotalClassesMissing() throws Exception {
            mockMvc.perform(post("/api/students/2/packages")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "amountPaid": 100.00,
                                      "paymentDate": "2026-03-12"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetPackagesForStudent {

        @Test
        void shouldReturnAllPackages() throws Exception {
            mockMvc.perform(get("/api/students/2/packages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].currency", everyItem(is("RUBLES"))))
                    .andExpect(jsonPath("$[*].convertedAmountPaid", everyItem(notNullValue())));
        }

        @Test
        void shouldReturnEmptyForStudentWithNoPackages() throws Exception {
            mockMvc.perform(get("/api/students/1/packages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class GetActivePackages {

        @Test
        void shouldReturnOnlyNonExhaustedPackages() throws Exception {
            mockMvc.perform(get("/api/students/2/packages/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].classesRemaining").value(8))
                    .andExpect(jsonPath("$[0].exhausted").value(false));
        }
    }

    // ── PackageController (single-resource) ─────────────────────────────────

    @Nested
    class GetPackageById {

        @Test
        void shouldReturnPackageWithConvertedAmounts() throws Exception {
            mockMvc.perform(get("/api/packages/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.studentName").value("Ivan Petrov"))
                    .andExpect(jsonPath("$.amountPaid").value(15000.00))
                    .andExpect(jsonPath("$.currency").value("RUBLES"))
                    .andExpect(jsonPath("$.convertedAmountPaid.EUROS").isNumber())
                    .andExpect(jsonPath("$.convertedAmountPaid.DOLLARS").isNumber())
                    .andExpect(jsonPath("$.convertedAmountPaid.RUBLES").isNumber());
        }

        @Test
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/packages/999"))
                    .andExpect(status().isNotFound());
        }
    }
}

