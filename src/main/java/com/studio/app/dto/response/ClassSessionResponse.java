package com.studio.app.dto.response;

import com.studio.app.enums.ClassStatus;
import com.studio.app.enums.Currency;
import com.studio.app.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * Response for a single class session.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSessionResponse {

    /** Identifier of the class session. */
    private Long id;

    /** Identifier of the student attending this session. */
    private Long studentId;

    /** Convenience student name for UI display. */
    private String studentName;

    /** Session date in the student's timezone. */
    private LocalDate classDate;

    /** Session start time in the student's timezone. */
    private LocalTime startTime;

    /** Session duration in minutes. */
    private Integer durationMinutes;

    /** Lifecycle status of the session. */
    private ClassStatus status;

    /** Payment state for this session. */
    private PaymentStatus paymentStatus;

    /** Charged amount for this session, when applicable. */
    private BigDecimal priceCharged;

    /** Currency of {@code priceCharged}. */
    private Currency currency;

    /** The same {@code priceCharged} converted into all supported currencies. */
    private Map<Currency, BigDecimal> convertedPrices;

    /** Linked package purchase ID when paid from a package. */
    private Long packagePurchaseId;

    /** Whether this is a one-off session (not a recurring generated one). */
    private boolean oneOff;

    /** Optional note attached to the session. */
    private String note;
}
