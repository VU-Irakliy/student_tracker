package com.studio.app.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregates all class sessions for a single calendar day.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarDayResponse {

    private LocalDate date;
    /** Sum of all session durations for this day, expressed in hours. */
    private BigDecimal totalHours;
    /** Sum of durations for sessions with status COMPLETED, expressed in hours. */
    private BigDecimal completedHours;
    private List<ClassSessionResponse> sessions;
}
