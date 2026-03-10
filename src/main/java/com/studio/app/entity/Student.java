package com.studio.app.entity;

import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", unique = true)
    private String email;

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

    /** Timezone used for displaying/scheduling this student's classes. */
    @Enumerated(EnumType.STRING)
    @Column(name = "timezone", nullable = false)
    private StudioTimezone timezone;

    /** Notes visible to the teacher/admin. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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
