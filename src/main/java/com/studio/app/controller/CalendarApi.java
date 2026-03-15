package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.response.CalendarDayResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API contract for calendar operations.
 * Provides endpoints to retrieve a calendar view of all classes grouped by day.
 */
@Tag(name = "Calendar", description = "Calendar view of all classes grouped by day")
@RequestMapping(ApiConstants.CALENDAR)
public interface CalendarApi {

    /**
     * Retrieves all classes grouped by day for the given date range.
     * Defaults to today through the next 30 days if dates are omitted.
     *
     * @param from the start date of the range (inclusive), or {@code null} for today
     * @param to   the end date of the range (inclusive), or {@code null} for 30 days after {@code from}
     * @return a list of {@link CalendarDayResponse} objects grouped by day,
     * including per-day total hours and completed hours
     */
    @Operation(summary = "Get calendar",
            description = "Returns all classes grouped by day for the given date range. "
                    + "Defaults to today through the next 30 days if dates are omitted. "
                    + "Each day includes total scheduled hours and completed hours.")
    @GetMapping
    ResponseEntity<List<CalendarDayResponse>> getCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);
}
