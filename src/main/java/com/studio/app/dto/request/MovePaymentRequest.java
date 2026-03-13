package com.studio.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request to move a payment from a cancelled session to another session.
 * Used when {@code keepAsPaid = false} was selected during cancellation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovePaymentRequest {

    /** ID of the destination session that should receive the payment. */
    @NotNull(message = "Target session ID is required")
    private Long targetSessionId;
}
