package com.studio.app.controller.impl;
import com.studio.app.controller.SessionApi;
import com.studio.app.controller.SessionPaymentApi;
import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.StudioTimezone;
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
public class SessionController implements SessionApi, SessionPaymentApi {
    private final ClassSessionService sessionService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> getSessionById(Long sessionId, StudioTimezone timezone) {
        return ResponseEntity.ok(sessionService.getSessionById(sessionId, timezone));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> updateSession(Long sessionId, UpdateSessionRequest request,
                                                              StudioTimezone timezone) {
        return ResponseEntity.ok(sessionService.updateSession(sessionId, request, timezone));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> cancelSession(Long sessionId, CancelSessionRequest request,
                                                              StudioTimezone timezone) {
        return ResponseEntity.ok(sessionService.cancelSession(sessionId, request, timezone));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> markPaid(Long sessionId, PaySessionRequest request,
                                                         StudioTimezone timezone) {
        return ResponseEntity.ok(sessionService.markSessionPaid(sessionId, request, timezone));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> setCompletion(Long sessionId, boolean completed,
                                                              StudioTimezone timezone) {
        return ResponseEntity.ok(sessionService.setSessionCompletion(sessionId, completed, timezone));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ClassSessionResponse> cancelPayment(Long sessionId, StudioTimezone timezone) {
        return ResponseEntity.ok(sessionService.cancelSessionPayment(sessionId, timezone));
    }


}
