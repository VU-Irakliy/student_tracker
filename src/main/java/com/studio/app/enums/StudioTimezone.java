package com.studio.app.enums;

import java.time.ZoneId;

/**
 * Supported timezones for class scheduling.
 * All times are stored in UTC in the database and converted on read.
 */
public enum StudioTimezone {

    SPAIN("Europe/Madrid"),
    RUSSIA_MOSCOW("Europe/Moscow");

    private final String zoneId;

    StudioTimezone(String zoneId) {
        this.zoneId = zoneId;
    }

    /** Returns the Java {@link ZoneId} for this timezone. */
    public ZoneId toZoneId() {
        return ZoneId.of(zoneId);
    }

    public String getZoneId() {
        return zoneId;
    }
}
