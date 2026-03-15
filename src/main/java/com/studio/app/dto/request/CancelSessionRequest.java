package com.studio.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request body for cancelling a class session.
 *
 * <p>Cancellation behaviour:
 * <ul>
 *   <li>{@code keepAsPaid = true}  — session is marked CANCELLED but payment status unchanged.</li>
 *   <li>{@code keepAsPaid = false} — payment status is reset to UNPAID (for PER_CLASS),
 *       or the package slot is returned (for PACKAGE students).</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelSessionRequest {

    /** Whether cancellation should preserve the current paid state. */
    @NotNull(message = "keepAsPaid flag is required")
    private Boolean keepAsPaid;

    /** Optional note explaining the cancellation reason. */
    private String note;
}
