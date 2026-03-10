package com.studio.app.controller;

import com.studio.app.dto.response.CalendarDayResponse;
import com.studio.app.service.ClassSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for the calendar view.
 *
 * <p>Endpoint lives under {@code /api/calendar}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final ClassSessionService sessionService;

    /**
     * Returns a calendar view of all students' classes grouped by day.
     * Each day lists its sessions with student name and payment status.
     *
     * @param from start date (ISO format: yyyy-MM-dd), defaults to today
     * @param to   end date (ISO format: yyyy-MM-dd), defaults to 30 days from today
     * @return 200 OK with a list of calendar days
     */
    @GetMapping
    public ResponseEntity<List<CalendarDayResponse>> getCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var start = from != null ? from : LocalDate.now();
        var end   = to   != null ? to   : start.plusDays(30);
        return ResponseEntity.ok(sessionService.getCalendar(start, end));
    }
}

