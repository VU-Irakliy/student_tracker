package com.studio.app.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request for marking a {@code PER_CLASS} session as paid.
 * No body is strictly required — the price is taken from the session's captured price.
 * This DTO allows overriding the recorded amount if needed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaySessionRequest {

    /**
     * Optional override for the amount paid.
     * When null the session's {@code priceCharged} is used.
     */
    @Positive(message = "Override amount must be positive")
    private BigDecimal amountOverride;
}
