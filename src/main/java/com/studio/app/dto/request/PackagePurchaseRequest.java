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

    @NotNull(message = "Total classes is required")
    @Min(value = 1, message = "Package must include at least 1 class")
    private Integer totalClasses;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.00", message = "Amount paid cannot be negative")
    private BigDecimal amountPaid;

    /** Optional currency override. Defaults to the student's currency if not provided. */
    private Currency currency;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    private String description;
}
