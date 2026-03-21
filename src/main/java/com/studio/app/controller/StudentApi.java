package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.dto.response.StudentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API contract for student CRUD operations.
 * Provides endpoints to create, list, retrieve, update, and soft-delete students.
 */
@Tag(name = "Students", description = "CRUD operations for managing students")
@RequestMapping(ApiConstants.STUDENTS)
public interface StudentApi {

    /**
     * Registers a new student with pricing, class type, timezone, and lifecycle controls.
     *
     * <p>Optional lifecycle fields:
     * <ul>
     *   <li>{@code startDate} — earliest class date allowed for this student.</li>
     *   <li>{@code holidayMode=true} requires {@code holidayFrom}.</li>
     *   <li>{@code stoppedAttending=true} keeps the student visible but blocks new classes/schedules.</li>
     * </ul>
     *
     * @param request the student creation details
     * @return the created {@link StudentResponse}
     */
    @Operation(summary = "Create a student", description = "Registers a new student with pricing type, class type, price, and timezone.")
    @PostMapping
    ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request);

    /**
     * Returns all active students, optionally filtered by a search term.
     *
     * @param search an optional name filter; if {@code null} or blank, all students are returned
     * @return a list of {@link StudentResponse} objects
     */
    @Operation(summary = "List all students", description = "Returns all active students. Pass an optional 'search' param to filter by name.")
    @GetMapping
    ResponseEntity<List<StudentResponse>> getAllStudents(@RequestParam(required = false) String search);

    /**
     * Searches active students by student name or payer full name.
     *
     * @param query case-insensitive search string
     * @return matched students
     */
    @Operation(summary = "Search students by student or payer name",
            description = "Matches active students by student first/last/full name or active payer full name.")
    @GetMapping("/search")
    ResponseEntity<List<StudentResponse>> searchStudentsByStudentOrPayerName(@RequestParam String query);

    /**
     * Returns a single student by their ID.
     *
     * @param id the ID of the student
     * @return the {@link StudentResponse} for the requested student
     */
    @Operation(summary = "Get a student by ID", description = "Returns a single student by their ID.")
    @GetMapping("/{id}")
    ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id);

    /**
     * Partially updates a student. Only non-null fields in the request are applied.
     *
     * <p>Holiday state transitions:
     * <ul>
     *   <li>set {@code holidayMode=true} with {@code holidayFrom} to start holiday mode</li>
     *   <li>set {@code holidayMode=false} with {@code holidayTo} to return from holiday</li>
     * </ul>
     *
     * @param id      the ID of the student to update
     * @param request the fields to update
     * @return the updated {@link StudentResponse}
     */
    @Operation(summary = "Update a student", description = "Partially updates a student. Only non-null fields are applied.")
    @PatchMapping("/{id}")
    ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request);

    /**
     * Soft-deletes a student together with their schedules, payers, and future sessions.
     * Past sessions are preserved.
     *
     * @param id the ID of the student to delete
     * @return 204 No Content on success
     */
    @Operation(summary = "Delete a student", description = "Soft-deletes a student, their schedules, payers, and future sessions. Past sessions are preserved.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteStudent(@PathVariable Long id);
}
