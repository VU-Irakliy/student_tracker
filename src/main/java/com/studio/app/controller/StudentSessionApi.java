package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.StudioTimezone;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API contract for student-scoped session operations.
 * Provides endpoints to create one-off classes and list sessions for a student.
 */
@Tag(name = "Student Sessions", description = "Student-scoped session operations: create one-off classes and list sessions")
@RequestMapping(ApiConstants.STUDENTS)
public interface StudentSessionApi {

    /**
     * Creates an extra or rescheduled class for a student outside their regular weekly schedule.
     *
     * @param studentId the ID of the student
     * @param request   the one-off session details
     * @return the created {@link ClassSessionResponse}
     */
    @Operation(summary = "Create a one-off session",
            description = "Creates an extra or rescheduled class for a student outside their regular weekly schedule.")
    @PostMapping("/{studentId}/sessions")
    ResponseEntity<ClassSessionResponse> createOneOffSession(@PathVariable Long studentId,
                                                              @Valid @RequestBody OneOffSessionRequest request,
                                                              @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);

    /**
     * Returns all sessions for a student, optionally filtered by a date range.
     *
     * @param studentId the ID of the student
     * @param from      the start date filter (inclusive), or {@code null} for no lower bound
     * @param to        the end date filter (inclusive), or {@code null} for no upper bound
     * @return a list of {@link ClassSessionResponse} objects
     */
    @Operation(summary = "List sessions for a student",
            description = "Returns all sessions for a student, optionally filtered by date range (from/to).")
    @GetMapping("/{studentId}/sessions")
    ResponseEntity<List<ClassSessionResponse>> getSessionsForStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);

    /**
     * Returns sessions for a student filtered by payment status.
     *
     * @param studentId     the ID of the student
     * @param paymentStatus the payment status to filter by (PAID, UNPAID, PACKAGE, or REFUNDED)
     * @return a list of matching {@link ClassSessionResponse} objects
     */
    @Operation(summary = "List sessions by payment status",
            description = "Returns sessions for a student filtered by payment status: PAID, UNPAID, PACKAGE, or REFUNDED.")
    @GetMapping("/{studentId}/sessions/by-payment")
    ResponseEntity<List<ClassSessionResponse>> getSessionsByPayment(@PathVariable Long studentId,
                                                                     @RequestParam PaymentStatus paymentStatus,
                                                                     @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);
}
