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

    private Long id;
    private Long studentId;
    private String studentName;
    private Integer totalClasses;
    private Integer classesRemaining;
    private BigDecimal amountPaid;
    private Currency currency;

    /** The same {@code amountPaid} converted into all supported currencies. */
    private Map<Currency, BigDecimal> convertedAmountPaid;

    private LocalDate paymentDate;
    private String description;
    private boolean exhausted;
    private LocalDateTime createdAt;
}
