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

    private Long id;
    private Long studentId;
    private String studentName;
    private LocalDate classDate;
    private LocalTime startTime;
    private Integer durationMinutes;
    private ClassStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal priceCharged;
    private Currency currency;

    /** The same {@code priceCharged} converted into all supported currencies. */
    private Map<Currency, BigDecimal> convertedPrices;

    private Long packagePurchaseId;
    private boolean oneOff;
    private String note;
}
