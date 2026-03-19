package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.StudioTimezone;
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
    ResponseEntity<ClassSessionResponse> getSessionById(@PathVariable Long sessionId,
                                                        @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);

    /**
     * Partially updates a session in one request (date/time/duration/status/payment/note).
     * Payment can be toggled via {@code paid=true|false}; dedicated pay/unpay endpoints remain available.
     *
     * @param sessionId the ID of the session
     * @param request   partial fields to update
     * @return the updated {@link ClassSessionResponse}
     */
    @Operation(summary = "Update a session",
            description = "Partially updates session data (date, time, duration, status, paid flag, note) in a single endpoint.")
    @PutMapping("/{sessionId}")
    ResponseEntity<ClassSessionResponse> updateSession(@PathVariable Long sessionId,
                                                       @Valid @RequestBody UpdateSessionRequest request,
                                                       @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);

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
                                                       @Valid @RequestBody CancelSessionRequest request,
                                                       @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);

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
                                                  @RequestBody(required = false) PaySessionRequest request,
                                                  @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);

    /**
     * Sets session completion state.
     *
     * @param sessionId  the session ID
     * @param completed  true -> COMPLETED, false -> SCHEDULED
     * @return the updated {@link ClassSessionResponse}
     */
    @Operation(summary = "Set session completion", description = "Sets session status to COMPLETED when completed=true, otherwise SCHEDULED.")
    @PostMapping("/{sessionId}/completion")
    ResponseEntity<ClassSessionResponse> setCompletion(@PathVariable Long sessionId,
                                                       @RequestParam boolean completed,
                                                       @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);

    /**
     * Reverts a session to UNPAID. For package sessions, the class is returned to the package.
     *
     * @param sessionId the ID of the session whose payment should be cancelled
     * @return the updated {@link ClassSessionResponse}
     */
    @Operation(summary = "Cancel a payment", description = "Reverts a session to UNPAID. For package sessions, the class is returned to the package.")
    @PostMapping("/{sessionId}/cancel-payment")
    ResponseEntity<ClassSessionResponse> cancelPayment(@PathVariable Long sessionId,
                                                       @RequestParam(defaultValue = "SPAIN") StudioTimezone timezone);


}
