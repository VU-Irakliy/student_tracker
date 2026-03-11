package com.studio.app.integration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class PayerControllerIT extends BaseIntegrationTest {

    @Nested
    class AddPayer {

        @Test
        void shouldCreateAndReturn201() throws Exception {
            mockMvc.perform(post("/api/students/3/payers")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "fullName": "Jane Smith",
                                      "phoneNumber": "+12025559999",
                                      "note": "Wife"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.studentId").value(3))
                    .andExpect(jsonPath("$.fullName").value("Jane Smith"))
                    .andExpect(jsonPath("$.phoneNumber").value("+12025559999"))
                    .andExpect(jsonPath("$.note").value("Wife"));
        }

        @Test
        void shouldReturn400_whenFullNameMissing() throws Exception {
            mockMvc.perform(post("/api/students/3/payers")
                            .contentType(JSON)
                            .content("""
                                    { "phoneNumber": "+12025559999" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn404ForNonExistentStudent() throws Exception {
            mockMvc.perform(post("/api/students/999/payers")
                            .contentType(JSON)
                            .content("""
                                    { "fullName": "Nobody" }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetPayers {

        @Test
        void shouldReturnPayersForStudent() throws Exception {
            mockMvc.perform(get("/api/students/1/payers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].fullName").value("María García"))
                    .andExpect(jsonPath("$[0].note").value("Mother"));
        }

        @Test
        void shouldReturnEmptyForStudentWithNoPayers() throws Exception {
            mockMvc.perform(get("/api/students/3/payers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class UpdatePayer {

        @Test
        void shouldUpdatePayerDetails() throws Exception {
            mockMvc.perform(post("/api/students/1/payers/1")
                            .contentType(JSON)
                            .content("""
                                    {
                                      "fullName": "María García López",
                                      "phoneNumber": "+34600111333",
                                      "note": "Mother - updated"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("María García López"))
                    .andExpect(jsonPath("$.phoneNumber").value("+34600111333"))
                    .andExpect(jsonPath("$.note").value("Mother - updated"));
        }

        @Test
        void shouldReturn404ForNonExistentPayer() throws Exception {
            mockMvc.perform(post("/api/students/1/payers/999")
                            .contentType(JSON)
                            .content("""
                                    { "fullName": "Nobody" }
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class RemovePayer {

        @Test
        void shouldReturn204() throws Exception {
            mockMvc.perform(post("/api/students/1/payers/1/delete"))
                    .andExpect(status().isNoContent());

            // Verify soft-deleted
            mockMvc.perform(get("/api/students/1/payers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void shouldReturn404ForNonExistentPayer() throws Exception {
            mockMvc.perform(post("/api/students/1/payers/999/delete"))
                    .andExpect(status().isNotFound());
        }
    }
}

