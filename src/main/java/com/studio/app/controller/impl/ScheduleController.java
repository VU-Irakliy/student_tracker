package com.studio.app.controller.impl;
import com.studio.app.controller.ScheduleApi;
import com.studio.app.dto.request.WeeklyScheduleRequest;
import com.studio.app.dto.response.WeeklyScheduleResponse;
import com.studio.app.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * REST controller implementation for student weekly schedule operations.
 * Delegates to {@link ScheduleService} for business logic.
 */
@RestController
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApi {
    private final ScheduleService scheduleService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<WeeklyScheduleResponse> addSchedule(Long studentId, WeeklyScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.addSchedule(studentId, request));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<List<WeeklyScheduleResponse>> getSchedules(Long studentId) {
        return ResponseEntity.ok(scheduleService.getSchedulesForStudent(studentId));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<WeeklyScheduleResponse> updateSchedule(Long studentId, Long scheduleId, WeeklyScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(studentId, scheduleId, request));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<Void> removeSchedule(Long studentId, Long scheduleId) {
        scheduleService.removeSchedule(studentId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
