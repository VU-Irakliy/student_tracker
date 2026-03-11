package com.studio.app.controller;

import com.studio.app.constant.ApiConstants;
import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.dto.response.StudentResponse;
import com.studio.app.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for student CRUD operations.
 * Base path: {@code /api/students}
 */
@RestController
@RequestMapping(ApiConstants.STUDENTS)
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /**
     * Creates a new student.
     *
     * @param request student creation payload
     * @return 201 Created with the new student
     */
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    /**
     * Returns all active students, with optional name search.
     *
     * @param search optional partial name filter
     * @return 200 OK with list of students
     */
    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents(
            @RequestParam(required = false) String search) {
        var result = (search != null && !search.isBlank())
                ? studentService.searchStudents(search)
                : studentService.getAllStudents();
        return ResponseEntity.ok(result);
    }

    /**
     * Returns a single student by ID.
     *
     * @param id the student ID
     * @return 200 OK or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    /**
     * Partially updates a student.
     *
     * @param id      the student ID
     * @param request fields to update
     * @return 200 OK with updated student
     */
    @PostMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    /**
     * Soft-deletes a student and all their associated data.
     *
     * @param id the student ID
     * @return 204 No Content
     */
    @PostMapping("/{id}/delete")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
