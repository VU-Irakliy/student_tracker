package com.studio.app.dto.response;

import com.studio.app.enums.ClassStatus;
import com.studio.app.enums.Currency;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.StudioTimezone;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /** Original stored session date before viewer-timezone conversion. */
    private LocalDate originalClassDate;

    /** Original stored session start time before viewer-timezone conversion. */
    private LocalTime originalStartTime;

    /** Timezone used for this session timing. */
    private StudioTimezone timezone;

    /** Original timezone captured on the session record. */
    private StudioTimezone originalTimezone;

    /** Timezone requested by the viewer (applied to classDate/startTime). */
    private StudioTimezone viewerTimezone;

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

    /** Date-time when this session payment was received. */
    private LocalDateTime paymentDateTime;

    /** The same {@code priceCharged} converted into all supported currencies. */
    private Map<Currency, BigDecimal> convertedPrices;

    /** Linked package purchase ID when paid from a package. */
    private Long packagePurchaseId;

    /** Whether this is a one-off session (not a recurring generated one). */
    private boolean oneOff;

    /** Optional note attached to the session. */
    private String note;
}
