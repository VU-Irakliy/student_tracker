package com.studio.app.repository;

import com.studio.app.entity.ClassSession;
import com.studio.app.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ClassSession} entities.
 */
@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    /** All non-deleted sessions for a student ordered by date and time. */
    List<ClassSession> findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(Long studentId);

    /** Non-deleted sessions for a student within an inclusive date range. */
    @Query("""
            SELECT cs FROM ClassSession cs
            WHERE cs.student.id = :studentId
              AND cs.deleted     = false
              AND cs.classDate  >= :from
              AND cs.classDate  <= :to
            ORDER BY cs.classDate, cs.startTime
            """)
    List<ClassSession> findByStudentIdAndDateRange(
            @Param("studentId") Long studentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Calendar query — all non-deleted sessions across all students
     * within a date range, ordered chronologically.
     */
    @Query("""
            SELECT cs FROM ClassSession cs
            JOIN FETCH cs.student s
            WHERE cs.deleted    = false
              AND s.deleted     = false
              AND cs.classDate >= :from
              AND cs.classDate <= :to
            ORDER BY cs.classDate, cs.startTime
            """)
    List<ClassSession> findCalendarSessions(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** All paid sessions for a student (for payment summary). */
    List<ClassSession> findByStudentIdAndPaymentStatusAndDeletedFalse(
            Long studentId, PaymentStatus paymentStatus);

    /** Finds a non-deleted session by its ID. */
    Optional<ClassSession> findByIdAndDeletedFalse(Long id);

    /** All non-deleted sessions linked to a specific package purchase. */
    List<ClassSession> findByPackagePurchaseIdAndDeletedFalse(Long packagePurchaseId);

    /**
     * Finds all PAID (per-class) sessions within a date range, excluding PACKAGE payments.
     * Used for earnings aggregation.
     */
    @Query("""
            SELECT cs FROM ClassSession cs
            JOIN FETCH cs.student s
            WHERE cs.deleted        = false
              AND s.deleted         = false
              AND cs.paymentStatus  = 'PAID'
              AND cs.classDate     >= :from
              AND cs.classDate     <= :to
            ORDER BY cs.classDate, cs.startTime
            """)
    List<ClassSession> findPaidSessionsByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Finds per-class sessions in a date range that are collectible or already earned.
     * Includes PAID and UNPAID sessions, excludes CANCELLED and PACKAGE-covered sessions.
     */
    @Query("""
            SELECT cs FROM ClassSession cs
            JOIN FETCH cs.student s
            WHERE cs.deleted        = false
              AND s.deleted         = false
              AND cs.paymentStatus IN ('PAID', 'UNPAID')
              AND cs.status        <> 'CANCELLED'
              AND cs.classDate     >= :from
              AND cs.classDate     <= :to
            ORDER BY cs.classDate, cs.startTime
            """)
    List<ClassSession> findCollectiblePerClassSessionsByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Finds per-class sessions in a date range that represent potential earnings,
     * including cancelled sessions. Includes PAID and UNPAID sessions,
     * excludes PACKAGE-covered sessions.
     */
    @Query("""
            SELECT cs FROM ClassSession cs
            JOIN FETCH cs.student s
            WHERE cs.deleted        = false
              AND s.deleted         = false
              AND cs.paymentStatus IN ('PAID', 'UNPAID')
              AND cs.classDate     >= :from
              AND cs.classDate     <= :to
            ORDER BY cs.classDate, cs.startTime
            """)
    List<ClassSession> findPotentialPerClassSessionsIncludingCancellationsByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** True when student has at least one unpaid non-cancelled session that has already happened. */
    @Query("""
            SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END
            FROM ClassSession cs
            WHERE cs.deleted          = false
              AND cs.student.id       = :studentId
              AND cs.student.deleted  = false
              AND cs.paymentStatus    = 'UNPAID'
              AND cs.status          <> 'CANCELLED'
              AND (
                    cs.classDate < :localDate
                    OR (cs.classDate = :localDate AND cs.startTime <= :localTime)
                  )
            """)
    boolean existsUnpaidOccurredSessionForStudent(
            @Param("studentId") Long studentId,
            @Param("localDate") LocalDate localDate,
            @Param("localTime") java.time.LocalTime localTime);
}
