package com.studio.app.controller;

import com.studio.app.dto.request.WeeklyScheduleRequest;
import com.studio.app.dto.response.WeeklyScheduleResponse;
import com.studio.app.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing a student's recurring weekly schedule.
 * Base path: {@code /api/students}
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Adds a new recurring weekly slot for the student.
     *
     * @param studentId the student ID
     * @param request   schedule details
     * @return 201 Created with the new schedule entry
     */
    @PostMapping("/{studentId}/schedules")
    public ResponseEntity<WeeklyScheduleResponse> addSchedule(
            @PathVariable Long studentId,
            @Valid @RequestBody WeeklyScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.addSchedule(studentId, request));
    }

    /**
     * Returns all active weekly schedule slots for the student.
     *
     * @param studentId the student ID
     * @return 200 OK with list of schedule entries
     */
    @GetMapping("/{studentId}/schedules")
    public ResponseEntity<List<WeeklyScheduleResponse>> getSchedules(@PathVariable Long studentId) {
        return ResponseEntity.ok(scheduleService.getSchedulesForStudent(studentId));
    }

    /**
     * Updates an existing weekly schedule slot.
     *
     * @param studentId  the student ID
     * @param scheduleId the schedule entry ID
     * @param request    updated details
     * @return 200 OK with updated entry
     */
    @PostMapping("/{studentId}/schedules/{scheduleId}")
    public ResponseEntity<WeeklyScheduleResponse> updateSchedule(
            @PathVariable Long studentId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody WeeklyScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(studentId, scheduleId, request));
    }

    /**
     * Soft-deletes a recurring schedule slot.
     *
     * @param studentId  the student ID
     * @param scheduleId the schedule entry ID
     * @return 204 No Content
     */
    @PostMapping("/{studentId}/schedules/{scheduleId}/delete")
    public ResponseEntity<Void> removeSchedule(
            @PathVariable Long studentId,
            @PathVariable Long scheduleId) {
        scheduleService.removeSchedule(studentId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
