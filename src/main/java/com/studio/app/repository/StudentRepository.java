package com.studio.app.repository;

import com.studio.app.entity.Student;
import com.studio.app.enums.PricingType;
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

    /** Returns active students filtered by debtor flag. */
    List<Student> findAllByDeletedFalseAndDebtor(boolean debtor);

    /** Returns active students filtered by pricing type. */
    List<Student> findAllByDeletedFalseAndPricingType(PricingType pricingType);

    /** Returns active students filtered by debtor flag and pricing type. */
    List<Student> findAllByDeletedFalseAndDebtorAndPricingType(boolean debtor, PricingType pricingType);

    /** Finds a non-deleted student by ID. */
    Optional<Student> findByIdAndDeletedFalse(Long id);


    /** Finds non-deleted students whose name contains the given query (case-insensitive). */
    @Query("""
            SELECT s FROM Student s
            WHERE s.deleted = false
              AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<Student> searchByName(String query);

    /** Finds non-deleted students by name and debtor flag (case-insensitive). */
    @Query("""
            SELECT s FROM Student s
            WHERE s.deleted = false
              AND s.debtor = :debtor
              AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<Student> searchByNameAndDebtor(String query, boolean debtor);

    /** Finds non-deleted students by name and pricing type (case-insensitive). */
    @Query("""
            SELECT s FROM Student s
            WHERE s.deleted = false
              AND s.pricingType = :pricingType
              AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<Student> searchByNameAndPricingType(String query, PricingType pricingType);

    /** Finds non-deleted students by name, debtor flag, and pricing type (case-insensitive). */
    @Query("""
            SELECT s FROM Student s
            WHERE s.deleted = false
              AND s.debtor = :debtor
              AND s.pricingType = :pricingType
              AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<Student> searchByNameAndDebtorAndPricingType(String query, boolean debtor, PricingType pricingType);

    /**
     * Finds active students by matching either student full name/parts or active payer full name.
     */
    @Query("""
            SELECT DISTINCT s FROM Student s
            LEFT JOIN Payer p
              ON p.student = s
             AND p.deleted = false
            WHERE s.deleted = false
              AND (
                    LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(COALESCE(p.fullName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY s.firstName, s.lastName, s.id
            """)
    List<Student> searchByStudentOrPayerName(String query);
}
