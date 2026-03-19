package com.studio.app.controller.impl;
import com.studio.app.controller.StudentSessionApi;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.StudioTimezone;
import com.studio.app.service.ClassSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller implementation for student-scoped session operations.
 * Delegates to {@link ClassSessionService} for business logic.
 */
@RestController
@RequiredArgsConstructor
public class StudentSessionController implements StudentSessionApi {
    private final ClassSessionService sessionService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> createOneOffSession(Long studentId, OneOffSessionRequest request,
                                                                    StudioTimezone timezone) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createOneOffSession(studentId, request, timezone));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<List<ClassSessionResponse>> getSessionsForStudent(Long studentId, LocalDate from,
                                                                            LocalDate to, StudioTimezone timezone) {
        return ResponseEntity.ok(sessionService.getSessionsForStudent(studentId, from, to, timezone));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<List<ClassSessionResponse>> getSessionsByPayment(Long studentId,
                                                                           PaymentStatus paymentStatus,
                                                                           StudioTimezone timezone) {
        return ResponseEntity.ok(sessionService.getSessionsByPaymentStatus(studentId, paymentStatus, timezone));
    }
}
