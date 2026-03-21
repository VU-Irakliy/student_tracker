package com.studio.app.dto.response;

import com.studio.app.enums.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unified payment record for the global payment history feed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecordResponse {

    /** Source of this payment row: SESSION or PACKAGE. */
    private String paymentType;

    /** Date-time when payment was recorded. */
    private LocalDateTime paymentDateTime;

    /** Paid amount in original currency. */
    private BigDecimal amount;

    /** Original currency of the payment. */
    private Currency currency;

    /** Student identifier related to this payment. */
    private Long studentId;

    /** Student full name for list display. */
    private String studentName;

    /** Session identifier when paymentType is SESSION. */
    private Long sessionId;

    /** Package purchase identifier when paymentType is PACKAGE. */
    private Long packagePurchaseId;

    /** Optional human note/description from source record. */
    private String note;
}

