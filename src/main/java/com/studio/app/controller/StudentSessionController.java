package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.service.ClassSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for student-scoped session operations.
 *
 * <p>All endpoints live under {@code /api/students}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.STUDENTS)
public class StudentSessionController {

    private final ClassSessionService sessionService;

    /**
     * Creates a one-off (extra or moved) class for a specific student.
     *
     * @param studentId the student ID
     * @param request   session details
     * @return 201 Created with the new session
     */
    @PostMapping("/{studentId}/sessions")
    public ResponseEntity<ClassSessionResponse> createOneOffSession(
            @PathVariable Long studentId,
            @Valid @RequestBody OneOffSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createOneOffSession(studentId, request));
    }

    /**
     * Returns all sessions for a student, optionally filtered by date range.
     *
     * @param studentId the student ID
     * @param from      optional start date (ISO format: yyyy-MM-dd)
     * @param to        optional end date (ISO format: yyyy-MM-dd)
     * @return 200 OK with list of sessions
     */
    @GetMapping("/{studentId}/sessions")
    public ResponseEntity<List<ClassSessionResponse>> getSessionsForStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(sessionService.getSessionsForStudent(studentId, from, to));
    }

    /**
     * Returns sessions for a student filtered by payment status.
     *
     * @param studentId     the student ID
     * @param paymentStatus PAID | UNPAID | PACKAGE | REFUNDED
     * @return 200 OK with matching sessions
     */
    @GetMapping("/{studentId}/sessions/by-payment")
    public ResponseEntity<List<ClassSessionResponse>> getSessionsByPayment(
            @PathVariable Long studentId,
            @RequestParam PaymentStatus paymentStatus) {
        return ResponseEntity.ok(sessionService.getSessionsByPaymentStatus(studentId, paymentStatus));
    }
}
