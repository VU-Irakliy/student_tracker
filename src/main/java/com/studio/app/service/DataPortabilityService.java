package com.studio.app.service;

import com.studio.app.dto.response.DataImportResultResponse;
import com.studio.app.dto.response.DataSnapshotResponse;

/**
 * Service for exporting/importing full application data snapshots.
 */
public interface DataPortabilityService {

    /** Exports all persisted records to a serializable snapshot object. */
    DataSnapshotResponse exportData();

    /**
     * Replaces current data with the given snapshot content.
     *
     * <p>Import runs in one transaction and rolls back fully on any failure.
     */
    DataImportResultResponse importData(DataSnapshotResponse snapshot);
}

