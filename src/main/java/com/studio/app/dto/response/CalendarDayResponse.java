package com.studio.app.dto.response;

import lombok.*;

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
    private List<ClassSessionResponse> sessions;
}
