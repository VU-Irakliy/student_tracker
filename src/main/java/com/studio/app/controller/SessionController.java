package com.studio.app.controller;

import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.MovePaymentRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.service.ClassSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for single-session operations.
 *
 * <p>All endpoints live under {@code /api/sessions}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class SessionController {

    private final ClassSessionService sessionService;

    /**
     * Returns a single session by its ID.
     *
     * @param sessionId the session ID
     * @return 200 OK or 404 Not Found
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ClassSessionResponse> getSessionById(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getSessionById(sessionId));
    }

    /**
     * Cancels a class session with configurable payment handling.
     *
     * @param sessionId the session ID
     * @param request   {@code keepAsPaid} flag and optional note
     * @return 200 OK with updated session
     */
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<ClassSessionResponse> cancelSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody CancelSessionRequest request) {
        return ResponseEntity.ok(sessionService.cancelSession(sessionId, request));
    }

    /**
     * Marks a session as paid.
     * <ul>
     *   <li>{@code PER_CLASS} students: marks as {@code PAID}. Body is optional — omit to use the captured price.</li>
     *   <li>{@code PACKAGE} students: auto-deducts from the oldest active package (FIFO). Body is ignored.</li>
     * </ul>
     *
     * @param sessionId the session ID
     * @param request   optional amount override (PER_CLASS only)
     * @return 200 OK with updated session
     */
    @PostMapping("/{sessionId}/pay")
    public ResponseEntity<ClassSessionResponse> markPaid(
            @PathVariable Long sessionId,
            @RequestBody(required = false) PaySessionRequest request) {
        return ResponseEntity.ok(sessionService.markSessionPaid(sessionId,
                request != null ? request : new PaySessionRequest()));
    }

    /**
     * Cancels the payment for a session (reverts to UNPAID or returns class to package).
     *
     * @param sessionId the session ID
     * @return 200 OK with updated session
     */
    @PostMapping("/{sessionId}/cancel-payment")
    public ResponseEntity<ClassSessionResponse> cancelPayment(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.cancelSessionPayment(sessionId));
    }

    /**
     * Moves the payment from this session to another session.
     *
     * @param sessionId the source session ID (must be in PAID state)
     * @param request   target session ID
     * @return 200 OK with the updated target session
     */
    @PostMapping("/{sessionId}/move-payment")
    public ResponseEntity<ClassSessionResponse> movePayment(
            @PathVariable Long sessionId,
            @Valid @RequestBody MovePaymentRequest request) {
        return ResponseEntity.ok(sessionService.movePayment(sessionId, request));
    }
}
