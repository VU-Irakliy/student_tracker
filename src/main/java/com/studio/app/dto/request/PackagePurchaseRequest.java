package com.studio.app.dto.request;

import com.studio.app.enums.Currency;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for recording a new package purchase by a student.
 *
 * <p>The {@code amountPaid} is what the student actually paid,
 * which may differ from any nominal list price for the bundle.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePurchaseRequest {

    /** Number of classes included in the purchased package. */
    @NotNull(message = "Total classes is required")
    @Min(value = 1, message = "Package must include at least 1 class")
    private Integer totalClasses;

    /** Actual amount received for the package. */
    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.00", message = "Amount paid cannot be negative")
    private BigDecimal amountPaid;

    /** Currency used for this package purchase. */
    @NotNull(message = "Currency is required")
    private Currency currency;

    /** Date the package payment was received. */
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    /** Optional free-form label for this package purchase. */
    private String description;
}
