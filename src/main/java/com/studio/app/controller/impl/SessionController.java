package com.studio.app.controller.impl;
import com.studio.app.controller.SessionApi;
import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.MovePaymentRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.service.ClassSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller implementation for single-session operations.
 * Delegates to {@link ClassSessionService} for business logic.
 */
@RestController
@RequiredArgsConstructor
public class SessionController implements SessionApi {
    private final ClassSessionService sessionService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> getSessionById(Long sessionId) {
        return ResponseEntity.ok(sessionService.getSessionById(sessionId));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> cancelSession(Long sessionId, CancelSessionRequest request) {
        return ResponseEntity.ok(sessionService.cancelSession(sessionId, request));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> markPaid(Long sessionId, PaySessionRequest request) {
        return ResponseEntity.ok(sessionService.markSessionPaid(sessionId,
                request != null ? request : new PaySessionRequest()));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> cancelPayment(Long sessionId) {
        return ResponseEntity.ok(sessionService.cancelSessionPayment(sessionId));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> movePayment(Long sessionId, MovePaymentRequest request) {
        return ResponseEntity.ok(sessionService.movePayment(sessionId, request));
    }
}
