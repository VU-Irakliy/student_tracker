package com.studio.app.controller.impl;
import com.studio.app.controller.StudentApi;
import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.dto.response.StudentResponse;
import com.studio.app.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * REST controller implementation for student CRUD operations.
 * Delegates to {@link StudentService} for business logic.
 */
@RestController
@RequiredArgsConstructor
public class StudentController implements StudentApi {
    private final StudentService studentService;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<StudentResponse> createStudent(CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<List<StudentResponse>> getAllStudents(String search, Boolean debtor, Boolean packagePricing) {
        var result = (search != null && !search.isBlank())
                ? studentService.searchStudents(search, debtor, packagePricing)
                : studentService.getAllStudents(debtor, packagePricing);
        return ResponseEntity.ok(result);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<List<StudentResponse>> searchStudentsByStudentOrPayerName(String query) {
        return ResponseEntity.ok(studentService.searchStudentsByStudentOrPayerName(query));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<StudentResponse> getStudentById(Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<StudentResponse> updateStudent(Long id, UpdateStudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<Void> deleteStudent(Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
