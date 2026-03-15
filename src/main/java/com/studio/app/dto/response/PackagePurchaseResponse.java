package com.studio.app.dto.response;

import com.studio.app.enums.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response for a package purchase.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePurchaseResponse {

    /** Identifier of the package purchase record. */
    private Long id;

    /** Identifier of the student who bought the package. */
    private Long studentId;

    /** Convenience student name for UI display. */
    private String studentName;

    /** Number of classes included in the package. */
    private Integer totalClasses;

    /** Number of classes still available in this package. */
    private Integer classesRemaining;

    /** Amount that was actually paid for the package. */
    private BigDecimal amountPaid;

    /** Currency in which the package was paid. */
    private Currency currency;

    /** The same {@code amountPaid} converted into all supported currencies. */
    private Map<Currency, BigDecimal> convertedAmountPaid;

    /** Date when payment for this package was received. */
    private LocalDate paymentDate;

    /** Optional free-form description of the package purchase. */
    private String description;

    /** Whether the package has no remaining classes. */
    private boolean exhausted;

    /** Creation timestamp of the purchase record. */
    private LocalDateTime createdAt;
}
