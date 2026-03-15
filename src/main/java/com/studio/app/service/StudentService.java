package com.studio.app.service;

import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.dto.response.StudentResponse;

import java.util.List;

/**
 * Service interface for managing students.
 */
public interface StudentService {

    /**
     * Creates a new student.
     *
     * @param request the student creation payload
     * @return the persisted student
     */
    StudentResponse createStudent(CreateStudentRequest request);

    /**
     * Returns all active (non-deleted) students.
     *
     * @return list of student responses
     */
    List<StudentResponse> getAllStudents();

    /**
     * Returns a student by ID.
     *
     * @param id the student ID
     * @return the student response
     */
    StudentResponse getStudentById(Long id);

    /**
     * Partially updates a student's profile.
     *
     * @param id      the student ID
     * @param request fields to update (null values are ignored)
     * @return the updated student
     */
    StudentResponse updateStudent(Long id, UpdateStudentRequest request);

    /**
     * Soft-deletes a student and all their future sessions and schedules.
     *
     * @param id the student ID
     */
    void deleteStudent(Long id);

    /**
     * Searches students by partial name match (case-insensitive).
     *
     * @param query the search string
     * @return matched students
     */
    List<StudentResponse> searchStudents(String query);
}
