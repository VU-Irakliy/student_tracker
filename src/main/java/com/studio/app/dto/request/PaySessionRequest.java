package com.studio.app.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request for marking a {@code PER_CLASS} session as paid.
 * paymentDateTime is strictly required — the price is taken from the session's captured price.
 * This DTO allows overriding the recorded amount if needed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaySessionRequest {

    /** Date-time when the payment happened (required for PER_CLASS, optional for PACKAGE). */
    private LocalDateTime paymentDateTime;

    /**
     * Optional override for the amount paid.
     * When null the session's {@code priceCharged} is used.
     */
    @Positive(message = "Override amount must be positive")
    private BigDecimal amountOverride;
}
