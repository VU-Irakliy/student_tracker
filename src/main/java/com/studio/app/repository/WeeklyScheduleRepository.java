package com.studio.app.repository;

import com.studio.app.entity.WeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link WeeklySchedule} entries.
 */
@Repository
public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long> {

    /** Returns all active weekly slots for a given student. */
    List<WeeklySchedule> findByStudentIdAndDeletedFalse(Long studentId);

    /** Returns a specific day slot for a student, if not deleted. */
    Optional<WeeklySchedule> findByIdAndStudentIdAndDeletedFalse(Long id, Long studentId);

    /** Checks whether a student already has a recurring slot on the given day. */
    boolean existsByStudentIdAndDayOfWeekAndDeletedFalse(Long studentId, DayOfWeek dayOfWeek);
}
