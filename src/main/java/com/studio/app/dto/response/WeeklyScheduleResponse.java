package com.studio.app.dto.response;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Response for a recurring weekly schedule slot.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyScheduleResponse {

    /** Identifier of the schedule entry. */
    private Long id;

    /** Student identifier that owns this slot. */
    private Long studentId;

    /** Day of week for the recurring slot. */
    private DayOfWeek dayOfWeek;

    /** Start time of the recurring slot. */
    private LocalTime startTime;

    /** Duration of each occurrence in minutes. */
    private Integer durationMinutes;
}
