package com.studio.app.dto.request;

import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request body for creating a new student.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStudentRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

    @NotNull(message = "Pricing type is required")
    private PricingType pricingType;

    /**
     * Required when {@code pricingType == PER_CLASS}.
     * Must be a positive amount.
     */
    @DecimalMin(value = "0.01", message = "Price per class must be positive")
    private BigDecimal pricePerClass;

    @NotNull(message = "Timezone is required")
    private StudioTimezone timezone;

    private String notes;
}
