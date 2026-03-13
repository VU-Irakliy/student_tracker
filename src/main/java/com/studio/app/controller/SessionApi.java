package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.MovePaymentRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API contract for single-session operations.
 * Provides endpoints to view, cancel, pay, cancel payment, and move payments on individual sessions.
 */
@Tag(name = "Sessions", description = "Single-session operations: view, cancel, pay, and move payments")
@RequestMapping(ApiConstants.SESSIONS)
public interface SessionApi {

    /**
     * Returns a single class session by its ID.
     *
     * @param sessionId the ID of the session
     * @return the {@link ClassSessionResponse} for the requested session
     */
    @Operation(summary = "Get a session by ID", description = "Returns a single class session.")
    @GetMapping("/{sessionId}")
    ResponseEntity<ClassSessionResponse> getSessionById(@PathVariable Long sessionId);

    /**
     * Cancels a class session. Use {@code keepAsPaid} to retain payment or let it revert to unpaid.
     *
     * @param sessionId the ID of the session to cancel
     * @param request   the cancellation options
     * @return the updated {@link ClassSessionResponse}
     */
    @Operation(summary = "Cancel a session", description = "Cancels a class session. Use 'keepAsPaid' to retain payment or let it revert to unpaid.")
    @PostMapping("/{sessionId}/cancel")
    ResponseEntity<ClassSessionResponse> cancelSession(@PathVariable Long sessionId,
                                                       @Valid @RequestBody CancelSessionRequest request);

    /**
     * Marks a session as paid. For PER_CLASS students the session is marked as PAID
     * (with an optional amount override). For PACKAGE students the oldest active
     * package is auto-deducted (FIFO).
     *
     * @param sessionId the ID of the session to pay
     * @param request   optional payment details (may be {@code null})
     * @return the updated {@link ClassSessionResponse}
     */
    @Operation(summary = "Pay a session",
            description = "Marks a session as paid. PER_CLASS students: marks as PAID (optional amount override). "
                    + "PACKAGE students: auto-deducts from the oldest active package (FIFO).")
    @PostMapping("/{sessionId}/pay")
    ResponseEntity<ClassSessionResponse> markPaid(@PathVariable Long sessionId,
                                                  @RequestBody(required = false) PaySessionRequest request);

    /**
     * Reverts a session to UNPAID. For package sessions, the class is returned to the package.
     *
     * @param sessionId the ID of the session whose payment should be cancelled
     * @return the updated {@link ClassSessionResponse}
     */
    @Operation(summary = "Cancel a payment", description = "Reverts a session to UNPAID. For package sessions, the class is returned to the package.")
    @PostMapping("/{sessionId}/cancel-payment")
    ResponseEntity<ClassSessionResponse> cancelPayment(@PathVariable Long sessionId);

    /**
     * Transfers payment from this session (must be PAID) to another session.
     *
     * @param sessionId the ID of the source session (currently paid)
     * @param request   the move-payment details including the target session ID
     * @return the updated {@link ClassSessionResponse} for the source session
     */
    @Operation(summary = "Move a payment", description = "Transfers payment from this session (must be PAID) to another session.")
    @PostMapping("/{sessionId}/move-payment")
    ResponseEntity<ClassSessionResponse> movePayment(@PathVariable Long sessionId,
                                                     @Valid @RequestBody MovePaymentRequest request);
}
