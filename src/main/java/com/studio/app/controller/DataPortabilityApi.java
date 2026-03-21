package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.response.DataImportResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

/**
 * REST API contract for full data export/import operations.
 */
@Tag(name = "Data Portability", description = "Export and import full application data snapshots")
@RequestMapping(ApiConstants.DATA)
public interface DataPortabilityApi {

    /**
     * Exports all persisted data as a compressed JSON snapshot.
     *
     * @return downloadable GZIP file with full application snapshot
     */
    @Operation(summary = "Export all data", description = "Exports all records from all core tables as one downloadable compressed JSON (GZIP) snapshot.")
    @GetMapping("/export")
    ResponseEntity<byte[]> exportData();

    /**
     * Replaces current data with the provided snapshot.
     *
     * @param payload snapshot payload previously created by export endpoint
     * @return import summary counts
     */
    @Operation(summary = "Import all data", description = "Replaces current data with the supplied snapshot in one transaction. Accepts exported .json.gz directly and also plain JSON for compatibility.")
    @PostMapping(value = "/import", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, "application/gzip"})
    ResponseEntity<DataImportResultResponse> importData(@RequestBody byte[] payload);

    /**
     * Replaces current data with snapshot JSON uploaded as a file.
     *
     * @param file exported snapshot file
     * @return import summary counts
     */
    @Operation(summary = "Import data from file", description = "Replaces current data with uploaded snapshot file in one transaction. Supports .json.gz and plain .json.")
    @PostMapping(value = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DataImportResultResponse> importDataFile(@RequestPart("file") MultipartFile file);
}



