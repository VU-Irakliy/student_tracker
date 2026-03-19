package com.studio.app.entity;

import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudentClassType;
import com.studio.app.enums.StudioTimezone;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student enrolled in the studio.
 *
 * <p>Pricing is either per individual class ({@link PricingType#PER_CLASS})
 * or via packages ({@link PricingType#PACKAGE}).  Both can coexist — a student
 * might switch model over time, tracked through {@link PackagePurchase} history.
 *
 * <p>Soft-deleted when the student leaves; all related {@link ClassSession}s
 * are also soft-deleted.
 */
@Entity
@Table(name = "students", schema = "studio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    /** Database identifier of the student. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Student first name. */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /** Student last name. */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /** Optional contact phone number for the student or family. */
    @Column(name = "phone_number")
    private String phoneNumber;

    /** Current pricing model for this student. */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false)
    private PricingType pricingType;

    /**
     * Price per class when {@link PricingType#PER_CLASS}.
     * Null when using package pricing.
     */
    @Column(name = "price_per_class", precision = 10, scale = 2)
    private BigDecimal pricePerClass;

    /** Currency used for this student's pricing. */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currency;

    /** Timezone used for displaying/scheduling this student's classes. */
    @Enumerated(EnumType.STRING)
    @Column(name = "timezone", nullable = false)
    private StudioTimezone timezone;

    /** Program type this student studies (casual, exam prep, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", nullable = false)
    @Builder.Default
    private StudentClassType classType = StudentClassType.CASUAL;

    /** Date from which the student can start having classes. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** True while the student is on holiday; classes in that period are blocked/cancelled. */
    @Column(name = "holiday_mode", nullable = false)
    @Builder.Default
    private boolean holidayMode = false;

    /** First day of the holiday period (inclusive). */
    @Column(name = "holiday_from")
    private LocalDate holidayFrom;

    /** Day when the student is back from holiday (inclusive). */
    @Column(name = "holiday_to")
    private LocalDate holidayTo;

    /** True when the student stopped attending classes but should remain visible in lists. */
    @Column(name = "stopped_attending", nullable = false)
    @Builder.Default
    private boolean stoppedAttending = false;

    /** Notes visible to the teacher/admin. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** True when the student has at least one past unpaid session after nightly debtor check. */
    @Column(name = "debtor", nullable = false)
    @Builder.Default
    private boolean debtor = false;

    /** Recurring weekly schedule entries for this student. */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WeeklySchedule> weeklySchedules = new ArrayList<>();

    /** All concrete class sessions (past + future) for this student. */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassSession> classSessions = new ArrayList<>();

    /** Package purchases history. */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PackagePurchase> packagePurchases = new ArrayList<>();

    /** People who pay for this student's classes (e.g. parents, guardians). */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payer> payers = new ArrayList<>();

}
