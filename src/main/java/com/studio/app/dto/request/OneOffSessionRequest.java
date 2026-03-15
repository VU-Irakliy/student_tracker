package com.studio.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request body for creating a one-off (extra or moved) class session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneOffSessionRequest {

    /** Date of the one-off session. */
    @NotNull(message = "Date is required")
    private LocalDate classDate;

    /** Start time of the one-off session. */
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    /** Session duration in minutes. */
    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 480 minutes")
    private Integer durationMinutes;

    /** Optional human-readable note (e.g., "moved from Tuesday due to holiday"). */
    private String note;
}
