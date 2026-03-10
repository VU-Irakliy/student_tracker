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

    private Long id;
    private Long studentId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private Integer durationMinutes;
}
