package com.studio.app.dto.request;

import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudentClassType;
import com.studio.app.enums.StudioTimezone;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating a new student.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStudentRequest {

    /** Student first name. */
    @NotBlank(message = "First name is required")
    private String firstName;

    /** Student last name. */
    @NotBlank(message = "Last name is required")
    private String lastName;

    /** Optional contact phone number for the student or family. */
    private String phoneNumber;

    /** Pricing model used for this student. */
    @NotNull(message = "Pricing type is required")
    private PricingType pricingType;

    /**
     * Required when {@code pricingType == PER_CLASS}.
     * Must be a positive amount.
     */
    @DecimalMin(value = "0.01", message = "Price per class must be positive")
    private BigDecimal pricePerClass;

    /**
     * Currency of the price. Required when {@code pricePerClass} is set.
     */
    private Currency currency;

    /** Student timezone used to schedule and display class times. */
    @NotNull(message = "Timezone is required")
    private StudioTimezone timezone;

    /** Program type the student attends. Defaults to {@code CASUAL} when omitted. */
    private StudentClassType classType;

    /** First date when classes are allowed for this student. */
    private LocalDate startDate;

    /** True if the student is currently on holiday. Requires {@code holidayFrom}. */
    private Boolean holidayMode;

    /** Holiday start date (inclusive) when {@code holidayMode=true}. */
    private LocalDate holidayFrom;

    /** Return date when the student is back from holiday. */
    private LocalDate holidayTo;

    /** True when the student stopped attending but should remain visible in the list. */
    private Boolean stoppedAttending;

    /** Optional internal notes visible to teacher/admin. */
    private String notes;
}
