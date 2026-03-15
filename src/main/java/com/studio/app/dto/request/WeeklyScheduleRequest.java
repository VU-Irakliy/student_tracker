package com.studio.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Request body for adding or updating a recurring weekly schedule slot.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyScheduleRequest {

    /** Day of week for the recurring slot. */
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    /** Start time in the student's timezone. */
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    /** Duration of each class occurrence in minutes. */
    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 480 minutes")
    private Integer durationMinutes;
}
