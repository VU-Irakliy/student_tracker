package com.studio.app.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studio.app.controller.DataPortabilityApi;
import com.studio.app.dto.response.DataImportResultResponse;
import com.studio.app.dto.response.DataSnapshotResponse;
import com.studio.app.exception.BadRequestException;
import com.studio.app.service.DataPortabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * REST controller implementation for data import/export operations.
 */
@RestController
@RequiredArgsConstructor
public class DataPortabilityController implements DataPortabilityApi {

    private final DataPortabilityService dataPortabilityService;
    private final ObjectMapper objectMapper;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<byte[]> exportData() {
        var snapshot = dataPortabilityService.exportData();

        String json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Failed to serialize snapshot to JSON");
        }

        final byte[] compressed;
        try {
            compressed = gzip(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new BadRequestException("Failed to compress snapshot");
        }

        String timestamp = LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "student-mgmt-export-" + timestamp + "-utc.json.gz";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/gzip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(compressed);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<DataImportResultResponse> importData(byte[] payload) {
        return ResponseEntity.ok(dataPortabilityService.importData(parseSnapshot(payload)));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<DataImportResultResponse> importDataFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Snapshot file is required");
        }
        try {
            return ResponseEntity.ok(dataPortabilityService.importData(parseSnapshot(file.getBytes())));
        } catch (java.io.IOException ex) {
            throw new BadRequestException("Unable to read snapshot file");
        }
    }

    private DataSnapshotResponse parseSnapshot(byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new BadRequestException("Snapshot payload is required");
        }
        try {
            byte[] raw = isGzip(payload) ? gunzip(payload) : payload;
            return objectMapper.readValue(raw, DataSnapshotResponse.class);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid snapshot file format");
        }
    }

    private boolean isGzip(byte[] payload) {
        return payload.length >= 2
                && (payload[0] & 0xFF) == 0x1f
                && (payload[1] & 0xFF) == 0x8b;
    }

    private byte[] gzip(byte[] raw) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(out)) {
            gzipOut.write(raw);
            gzipOut.finish();
            return out.toByteArray();
        }
    }

    private byte[] gunzip(byte[] compressed) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(compressed);
             GZIPInputStream gzipIn = new GZIPInputStream(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = gzipIn.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}




