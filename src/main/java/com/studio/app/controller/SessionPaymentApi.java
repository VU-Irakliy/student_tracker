package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.StudioTimezone;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST API contract for session payment and completion operations.
 */
@Tag(name = "Session Payments", description = "Session payment and completion operations")
@RequestMapping(ApiConstants.SESSIONS)
public interface SessionPaymentApi {

    /**
     * Marks a session as paid.
     * For PER_CLASS students client must provide {@code paymentDateTime};
     * for PACKAGE students it is optional.
     * PER_CLASS sessions are marked as PAID
     * (with an optional amount override). For PACKAGE students the oldest active
     * package is auto-deducted (FIFO).
     *
     * @param sessionId the ID of the session to pay
     * @param request   payment details including when the payment happened
     * @return the updated {@link ClassSessionResponse}
     */
    @Operation(summary = "Pay a session",
            description = "Marks a session as paid. PER_CLASS students: paymentDateTime is required and amountOverride is optional. "
                    + "PACKAGE students: paymentDateTime is optional and payment is auto-deducted from the oldest active package (FIFO).")
    @PostMapping("/{sessionId}/pay")
    ResponseEntity<ClassSessionResponse> markPaid(@PathVariable Long sessionId,
                                                  @Valid @RequestBody PaySessionRequest request,
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

