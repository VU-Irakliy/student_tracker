package com.studio.app.controller.impl;
import com.studio.app.controller.CalendarApi;
import com.studio.app.dto.response.CalendarDayResponse;
import com.studio.app.service.ClassSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller implementation for calendar operations.
 * Delegates to {@link ClassSessionService} to build the calendar view.
 */
@RestController
@RequiredArgsConstructor
public class CalendarController implements CalendarApi {
    private final ClassSessionService sessionService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<List<CalendarDayResponse>> getCalendar(LocalDate from, LocalDate to) {
        var start = from != null ? from : LocalDate.now();
        var end   = to   != null ? to   : start.plusDays(30);
        return ResponseEntity.ok(sessionService.getCalendar(start, end));
    }
}
