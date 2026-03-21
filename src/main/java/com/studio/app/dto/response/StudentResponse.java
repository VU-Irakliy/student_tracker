package com.studio.app.dto.response;

import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudentClassType;
import com.studio.app.enums.StudioTimezone;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Full student profile response, including their active weekly schedule and payers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {

    /** Student identifier. */
    private Long id;

    /** Student first name. */
    private String firstName;

    /** Student last name. */
    private String lastName;

    /** Convenience full name assembled from first and last name. */
    private String fullName;

    /** Contact phone number for the student or family. */
    private String phoneNumber;

    /** Active pricing model for the student. */
    private PricingType pricingType;

    /** Per-class price when pricing model is {@code PER_CLASS}. */
    private BigDecimal pricePerClass;

    /** Currency used for student pricing. */
    private Currency currency;

    /**
     * The same {@code pricePerClass} converted into all supported currencies.
     * Key = target {@link Currency}, value = converted amount.
     */
    private Map<Currency, BigDecimal> convertedPrices;

    /** Timezone used for scheduling and displaying class times. */
    private StudioTimezone timezone;

    /** Program type this student attends. */
    private StudentClassType classType;

    /** Date from which this student can start having classes. */
    private LocalDate startDate;

    /** True while the student is on holiday. */
    private boolean holidayMode;

    /** First holiday day (inclusive). */
    private LocalDate holidayFrom;

    /** Day the student returns from holiday (inclusive). */
    private LocalDate holidayTo;

    /** True when student stopped attending but remains visible in lists. */
    private boolean stoppedAttending;

    /** Optional internal notes visible to teacher/admin. */
    private String notes;

    /** Whether the student is currently flagged as debtor. */
    private boolean debtor;

    /** Creation timestamp of the student record. */
    private LocalDateTime createdAt;

    /** Recurring weekly schedule entries for this student. */
    private List<WeeklyScheduleResponse> weeklySchedules;

    /** People registered as payers for this student (e.g. parents, guardians). */
    private List<PayerResponse> payers;
}
