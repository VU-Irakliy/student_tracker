package com.studio.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a person who pays for a student's classes.
 *
 * <p>When a bank transfer arrives, the teacher can record the payer's name
 * and phone number here so they know who transferred and on whose behalf.
 * A student may have multiple payers (e.g. both parents, a guardian).
 *
 * <p>Soft-deleted along with the student when the student leaves.
 */
@Entity
@Table(name = "payers", schema = "studio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payer extends BaseEntity {

    /** Database identifier of the payer record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Student for whom this person makes payments. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Full name of the person who makes the transfer. */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Contact phone number — useful for matching incoming transfers. */
    @Column(name = "phone_number")
    private String phoneNumber;

    /** Optional note (e.g. "mother", "grandfather", "pays every Monday"). */
    @Column(name = "note")
    private String note;
}
