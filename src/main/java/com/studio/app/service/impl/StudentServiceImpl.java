package com.studio.app.service.impl;

import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.dto.response.StudentResponse;
import com.studio.app.entity.Student;
import com.studio.app.exception.ConflictException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.StudentMapper;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PayerRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.repository.WeeklyScheduleRepository;
import com.studio.app.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link StudentService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final ClassSessionRepository classSessionRepository;
    private final PayerRepository payerRepository;
    private final StudentMapper studentMapper;

    /** {@inheritDoc} */
    @Override
    public StudentResponse createStudent(CreateStudentRequest request) {
        Optional.ofNullable(request.getEmail())
                .filter(e -> studentRepository.existsByEmailAndDeletedFalse(e))
                .ifPresent(e -> { throw new ConflictException("Email already in use: " + e); });

        var student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .pricingType(request.getPricingType())
                .pricePerClass(request.getPricePerClass())
                .timezone(request.getTimezone())
                .notes(request.getNotes())
                .build();

        return studentMapper.toResponse(studentRepository.save(student));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentMapper.toResponseList(studentRepository.findAllByDeletedFalse());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        return studentMapper.toResponse(findActiveStudent(id));
    }

    /** {@inheritDoc} */
    @Override
    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        var student = findActiveStudent(id);

        Optional.ofNullable(request.getEmail())
                .filter(e -> !e.equals(student.getEmail()) && studentRepository.existsByEmailAndDeletedFalse(e))
                .ifPresent(e -> { throw new ConflictException("Email already in use: " + e); });

        Optional.ofNullable(request.getFirstName()).ifPresent(student::setFirstName);
        Optional.ofNullable(request.getLastName()).ifPresent(student::setLastName);
        Optional.ofNullable(request.getEmail()).ifPresent(student::setEmail);
        Optional.ofNullable(request.getPhoneNumber()).ifPresent(student::setPhoneNumber);
        Optional.ofNullable(request.getPricingType()).ifPresent(student::setPricingType);
        Optional.ofNullable(request.getPricePerClass()).ifPresent(student::setPricePerClass);
        Optional.ofNullable(request.getTimezone()).ifPresent(student::setTimezone);
        Optional.ofNullable(request.getNotes()).ifPresent(student::setNotes);

        return studentMapper.toResponse(studentRepository.save(student));
    }

    /** {@inheritDoc} */
    @Override
    public void deleteStudent(Long id) {
        var student = findActiveStudent(id);

        // Soft-delete all weekly schedules
        weeklyScheduleRepository.findByStudentIdAndDeletedFalse(id)
                .forEach(schedule -> schedule.setDeleted(true));

        // Soft-delete all class sessions
        classSessionRepository
                .findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(id)
                .forEach(session -> session.setDeleted(true));

        // Soft-delete all payers
        payerRepository.findByStudentIdAndDeletedFalse(id)
                .forEach(payer -> payer.setDeleted(true));

        student.setDeleted(true);
        studentRepository.save(student);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> searchStudents(String query) {
        return studentMapper.toResponseList(studentRepository.searchByName(query));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Student findActiveStudent(Long id) {
        return studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }
}
