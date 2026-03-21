package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.StudioTimezone;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API contract for basic single-session operations.
 * Provides endpoints to view, update, and cancel individual sessions.
 */
@Tag(name = "Sessions", description = "Single-session CRUD-style operations: view, update, cancel")
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
     * <p>When {@code keepAsPaid=true}, the session is cancelled but payment is intentionally kept.
     * This is allowed for both PER_CLASS and PACKAGE students.
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


}
