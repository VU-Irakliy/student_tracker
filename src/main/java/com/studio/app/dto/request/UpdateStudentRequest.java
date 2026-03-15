package com.studio.app.dto.request;

import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudentClassType;
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

    /** Updated first name, if changing it. */
    private String firstName;

    /** Updated last name, if changing it. */
    private String lastName;

    /** Updated contact phone number, if provided. */
    private String phoneNumber;

    /** Updated pricing model for future sessions. */
    private PricingType pricingType;

    /** Updated per-class price when using {@code PER_CLASS}. */
    @DecimalMin(value = "0.01", message = "Price per class must be positive")
    private BigDecimal pricePerClass;

    /** Updated currency for student pricing. */
    private Currency currency;

    /** Updated student timezone used for scheduling/display. */
    private StudioTimezone timezone;

    /** Updated class program type for the student. */
    private StudentClassType classType;

    /** Updated internal notes visible to teacher/admin. */
    private String notes;
}
