package com.studio.app.repository;

import com.studio.app.entity.Payer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Payer} entities.
 */
@Repository
public interface PayerRepository extends JpaRepository<Payer, Long> {

    /** Returns all active payers for a given student. */
    List<Payer> findByStudentIdAndDeletedFalse(Long studentId);

    /** Finds a specific payer belonging to a student. */
    Optional<Payer> findByIdAndStudentIdAndDeletedFalse(Long id, Long studentId);
}
