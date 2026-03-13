package com.studio.app.dto.request;

import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request body for updating an existing student.
 * All fields are optional — only non-null values are applied.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStudentRequest {

    private String firstName;
    private String lastName;


    private String phoneNumber;
    private PricingType pricingType;

    @DecimalMin(value = "0.01", message = "Price per class must be positive")
    private BigDecimal pricePerClass;

    private Currency currency;

    private StudioTimezone timezone;
    private String notes;
}
