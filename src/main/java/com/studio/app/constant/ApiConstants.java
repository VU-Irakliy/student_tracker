package com.studio.app.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Centralised API path and application constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiConstants {

    /** Application version. */
    public static final String VERSION = "0.1";

    // ── Base paths ──────────────────────────────────────────────────────────

    public static final String STUDENTS  = "/api/students";
    public static final String SESSIONS  = "/api/sessions";
    public static final String PACKAGES  = "/api/packages";
    public static final String CALENDAR  = "/api/calendar";
    public static final String EARNINGS  = "/api/earnings";
    public static final String DATA      = "/api/data";
}

