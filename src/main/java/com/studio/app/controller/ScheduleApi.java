package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.WeeklyScheduleRequest;
import com.studio.app.dto.response.WeeklyScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API contract for student weekly schedule operations.
 * Provides endpoints to create, list, update, and soft-delete recurring weekly class slots.
 */
@Tag(name = "Student Schedules", description = "Manage a student's recurring weekly class schedule")
@RequestMapping(ApiConstants.STUDENTS)
public interface ScheduleApi {

    /**
     * Creates a new recurring weekly class slot for the student.
     *
     * @param studentId the ID of the student
     * @param request   the schedule details (day, time, duration)
     * @return the created {@link WeeklyScheduleResponse}
     */
    @Operation(summary = "Add a weekly schedule", description = "Creates a new recurring weekly class slot for the student.")
    @PostMapping("/{studentId}/schedules")
    ResponseEntity<WeeklyScheduleResponse> addSchedule(@PathVariable Long studentId,
                                                       @Valid @RequestBody WeeklyScheduleRequest request);

    /**
     * Returns all active weekly schedule slots for the student.
     *
     * @param studentId the ID of the student
     * @return a list of {@link WeeklyScheduleResponse} objects
     */
    @Operation(summary = "List schedules", description = "Returns all active weekly schedule slots for the student.")
    @GetMapping("/{studentId}/schedules")
    ResponseEntity<List<WeeklyScheduleResponse>> getSchedules(@PathVariable Long studentId);

    /**
     * Updates an existing weekly schedule slot (day, time, duration).
     *
     * @param studentId  the ID of the student
     * @param scheduleId the ID of the schedule to update
     * @param request    the updated schedule details
     * @return the updated {@link WeeklyScheduleResponse}
     */
    @Operation(summary = "Update a schedule", description = "Updates an existing weekly schedule slot (day, time, duration).")
    @PostMapping("/{studentId}/schedules/{scheduleId}")
    ResponseEntity<WeeklyScheduleResponse> updateSchedule(@PathVariable Long studentId,
                                                          @PathVariable Long scheduleId,
                                                          @Valid @RequestBody WeeklyScheduleRequest request);

    /**
     * Soft-deletes a recurring schedule slot.
     *
     * @param studentId  the ID of the student
     * @param scheduleId the ID of the schedule to remove
     * @return 204 No Content on success
     */
    @Operation(summary = "Delete a schedule", description = "Soft-deletes a recurring schedule slot.")
    @PostMapping("/{studentId}/schedules/{scheduleId}/delete")
    ResponseEntity<Void> removeSchedule(@PathVariable Long studentId, @PathVariable Long scheduleId);
}
