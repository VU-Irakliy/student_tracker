package com.studio.app.repository;

import com.studio.app.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Student} entities.
 * All queries automatically exclude soft-deleted records.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /** Returns all active (non-deleted) students. */
    List<Student> findAllByDeletedFalse();

    /** Finds a non-deleted student by ID. */
    Optional<Student> findByIdAndDeletedFalse(Long id);

    /** Checks whether a non-deleted student with the given email exists. */
    boolean existsByEmailAndDeletedFalse(String email);

    /** Finds non-deleted students whose name contains the given query (case-insensitive). */
    @Query("""
            SELECT s FROM Student s
            WHERE s.deleted = false
              AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<Student> searchByName(String query);
}
