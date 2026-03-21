package com.studio.app.entity;

import com.studio.app.enums.ClassStatus;
import com.studio.app.enums.Currency;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.StudioTimezone;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a single concrete class session for a student.
 *
 * <p>Sessions are either generated from a {@link WeeklySchedule} (recurring)
 * or created as a one-off event (extra class or moved class).
 *
 * <p>Payment tracking:
 * <ul>
 *   <li>For {@code PER_CLASS} students: {@code paymentStatus} tracks individual payment.</li>
 *   <li>For {@code PACKAGE} students: payment is linked to a {@link PackagePurchase}.
 *       When deducted, {@code packagePurchase} is set and {@code paymentStatus} is {@code PACKAGE}.</li>
 * </ul>
 *
 * <p>Cancellation options:
 * <ul>
 *   <li>Keep as paid — {@code status=CANCELLED}, {@code paymentStatus} unchanged.</li>
 *   <li>Move payment — {@code paymentStatus=UNPAID} here; caller assigns it to another session.</li>
 *   <li>Package: never reduces remaining classes (package slot is simply freed).</li>
 * </ul>
 */
@Entity
@Table(name = "class_sessions", schema = "studio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSession extends BaseEntity {

    /** Database identifier of the concrete class session. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Student who attends this class session. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * The weekly schedule that generated this session, if any.
     * Null for one-off extra/moved classes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_schedule_id")
    private WeeklySchedule weeklySchedule;

    /** Date of the class (in the student's local timezone). */
    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    /** Start time of the class (in the student's local timezone). */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** Timezone snapshot inherited from the student at session creation/update time. */
    @Enumerated(EnumType.STRING)
    @Column(name = "timezone", nullable = false)
    private StudioTimezone timezone;

    /** Duration in minutes. */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /** Operational state of the class (scheduled, completed, cancelled, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ClassStatus status = ClassStatus.SCHEDULED;

    /** How this session is paid (unpaid, paid, or deducted from package). */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    /**
     * For {@code PER_CLASS}: the price charged for this specific class.
     * Captured at booking time so price changes don't affect historical records.
     */
    @Column(name = "price_charged", precision = 10, scale = 2)
    private BigDecimal priceCharged;

    /** Currency in which the price was charged. */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currency;

    /** Date and time when this session payment was received (timezone-agnostic local timestamp). */
    @Column(name = "payment_date_time")
    private LocalDateTime paymentDateTime;

    /**
     * Package purchase this session is deducted from.
     * Only set when {@code paymentStatus == PACKAGE}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_purchase_id")
    private PackagePurchase packagePurchase;

    /** True when this is a one-off session (extra class or a moved class). */
    @Column(name = "is_one_off", nullable = false)
    @Builder.Default
    private boolean oneOff = false;

    /** Optional note (e.g., "moved from Tuesday", "extra class"). */
    @Column(name = "note")
    private String note;
}
