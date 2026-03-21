package com.studio.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(BaseIntegrationTest.StubCurrencyConfig.class)
class DataPortabilityControllerIT extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExportFullSnapshot() throws Exception {
        var result = mockMvc.perform(get("/api/data/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"student-mgmt-export-")))
                .andExpect(header().string("Content-Disposition", containsString("-utc.json.gz\"")))
                .andReturn();

        var root = objectMapper.readTree(gunzip(result.getResponse().getContentAsByteArray()));
        assertThat(root.path("snapshotVersion").asText()).isEqualTo("2");
        assertThat(root.path("students")).hasSize(4);
        assertThat(root.path("weeklySchedules")).hasSize(3);
        assertThat(root.path("packagePurchases")).hasSize(2);
        assertThat(root.path("classSessions")).hasSize(5);
        assertThat(root.path("payers")).hasSize(2);
    }

    @Test
    void shouldImportSnapshotAndReplaceCurrentData() throws Exception {
        var exportResult = mockMvc.perform(get("/api/data/export"))
                .andExpect(status().isOk())
                .andReturn();

        var root = objectMapper.readTree(gunzip(exportResult.getResponse().getContentAsByteArray()));
        ((ObjectNode) root.withArray("students").get(0)).put("firstName", "AnaImported");
        var payload = objectMapper.writeValueAsString(root);

        mockMvc.perform(post("/api/data/import")
                        .contentType(JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").value(4))
                .andExpect(jsonPath("$.weeklySchedules").value(3))
                .andExpect(jsonPath("$.packagePurchases").value(2))
                .andExpect(jsonPath("$.classSessions").value(5))
                .andExpect(jsonPath("$.payers").value(2));

        mockMvc.perform(get("/api/students").param("search", "AnaImported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("AnaImported"));
    }

    @Test
    void shouldImportDirectlyFromExportedFileBytes() throws Exception {
        var exportResult = mockMvc.perform(get("/api/data/export"))
                .andExpect(status().isOk())
                .andReturn();

        var exportedBytes = exportResult.getResponse().getContentAsByteArray();

        mockMvc.perform(post("/api/data/import")
                        .contentType(MediaType.parseMediaType("application/gzip"))
                        .content(exportedBytes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").value(4))
                .andExpect(jsonPath("$.weeklySchedules").value(3))
                .andExpect(jsonPath("$.packagePurchases").value(2))
                .andExpect(jsonPath("$.classSessions").value(5))
                .andExpect(jsonPath("$.payers").value(2));
    }

    @Test
    void shouldImportSnapshotFromFile() throws Exception {
        var exportResult = mockMvc.perform(get("/api/data/export"))
                .andExpect(status().isOk())
                .andReturn();

        var payload = exportResult.getResponse().getContentAsByteArray();

        mockMvc.perform(multipart("/api/data/import-file")
                        .file("file", payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").value(4));
    }

    private String gunzip(byte[] compressed) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(compressed);
             GZIPInputStream gzipIn = new GZIPInputStream(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = gzipIn.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}




