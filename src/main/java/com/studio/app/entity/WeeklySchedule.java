package com.studio.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Defines a recurring weekly slot for a student.
 *
 * <p>Each record represents one day-of-week entry.
 * A student attending Monday and Wednesday would have two rows.
 * The {@code startTime} and {@code durationMinutes} are stored in the
 * student's own timezone (see {@link Student#getTimezone()}).
 *
 * <p>Soft-deleted when the schedule day is removed or the student leaves.
 */
@Entity
@Table(name = "weekly_schedules", schema = "studio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Day of week this recurring slot falls on. */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /**
     * Class start time in the student's timezone.
     * Stored as {@code TIME} (no timezone) in PostgreSQL.
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** Duration of the class in minutes. */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /**
     * Effective from date — allows changing the schedule
     * without breaking historical session records.
     * Stored as epoch day for simplicity.
     */
    @Column(name = "effective_from_epoch_day")
    private Long effectiveFromEpochDay;
}
