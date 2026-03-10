package com.studio.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Records a package purchase made by a student.
 *
 * <p>A package is a prepaid bundle of N classes at a total price.
 * {@code classesRemaining} is decremented each time a {@link ClassSession}
 * is linked to this purchase.  When a cancelled class releases a slot
 * (non-keep-paid flow), {@code classesRemaining} is incremented back.
 *
 * <p>The {@code amountPaid} may differ from any nominal package price —
 * the teacher accepts whatever the student pays and records it here.
 */
@Entity
@Table(name = "package_purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePurchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Total number of classes included in this package. */
    @Column(name = "total_classes", nullable = false)
    private Integer totalClasses;

    /** How many classes are still available (not yet consumed). */
    @Column(name = "classes_remaining", nullable = false)
    private Integer classesRemaining;

    /** The actual amount received from the student (may differ from a list price). */
    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    /** Date payment was received. */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /** Optional description (e.g., "Summer 10-class bundle"). */
    @Column(name = "description")
    private String description;

    /** Computed field — whether all classes have been consumed. */
    @Transient
    public boolean isExhausted() {
        return classesRemaining != null && classesRemaining <= 0;
    }
}
