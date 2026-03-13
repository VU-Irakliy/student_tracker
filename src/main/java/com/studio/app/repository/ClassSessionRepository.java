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
}
