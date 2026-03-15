package com.studio.app.dto.request;

import com.studio.app.enums.ClassStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Partial update payload for an existing class session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSessionRequest {

    private LocalDate classDate;
    private LocalTime startTime;

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 480 minutes")
    private Integer durationMinutes;

    /** Session lifecycle status (SCHEDULED, COMPLETED, CANCELLED, MOVED). */
    private ClassStatus status;

    /**
     * Optional payment toggle:
     * true -> mark paid, false -> mark unpaid.
     */
    private Boolean paid;

    /** Optional per-class amount override when paid=true. */
    @Positive(message = "Override amount must be positive")
    private BigDecimal amountOverride;

    private String note;
}

