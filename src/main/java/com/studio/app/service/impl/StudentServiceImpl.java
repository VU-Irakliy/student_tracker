package com.studio.app.service.impl;

import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.dto.response.StudentResponse;
import com.studio.app.entity.Student;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.StudentMapper;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PayerRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.repository.WeeklyScheduleRepository;
import com.studio.app.service.CurrencyConversionService;
import com.studio.app.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final CurrencyConversionService currencyConversionService;

    /** {@inheritDoc} */
    @Override
    public StudentResponse createStudent(CreateStudentRequest request) {
        var student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .pricingType(request.getPricingType())
                .pricePerClass(request.getPricePerClass())
                .currency(request.getCurrency())
                .timezone(request.getTimezone())
                .notes(request.getNotes())
                .build();

        return enrichWithConvertedPrices(studentMapper.toResponse(studentRepository.save(student)));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentMapper.toResponseList(studentRepository.findAllByDeletedFalse())
                .stream().map(this::enrichWithConvertedPrices).toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        return enrichWithConvertedPrices(studentMapper.toResponse(findActiveStudent(id)));
    }

    /** {@inheritDoc} */
    @Override
    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        var student = findActiveStudent(id);

        Optional.ofNullable(request.getFirstName()).ifPresent(student::setFirstName);
        Optional.ofNullable(request.getLastName()).ifPresent(student::setLastName);
        Optional.ofNullable(request.getPhoneNumber()).ifPresent(student::setPhoneNumber);
        Optional.ofNullable(request.getPricingType()).ifPresent(student::setPricingType);
        Optional.ofNullable(request.getPricePerClass()).ifPresent(student::setPricePerClass);
        Optional.ofNullable(request.getCurrency()).ifPresent(student::setCurrency);
        Optional.ofNullable(request.getTimezone()).ifPresent(student::setTimezone);
        Optional.ofNullable(request.getNotes()).ifPresent(student::setNotes);

        return enrichWithConvertedPrices(studentMapper.toResponse(studentRepository.save(student)));
    }

    /** {@inheritDoc} */
    @Override
    public void deleteStudent(Long id) {
        var student = findActiveStudent(id);

        // Soft-delete all weekly schedules
        weeklyScheduleRepository.findByStudentIdAndDeletedFalse(id)
                .forEach(schedule -> schedule.setDeleted(true));

        // Soft-delete only future class sessions; past sessions are kept as historical records
        var today = LocalDate.now();
        classSessionRepository
                .findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(id)
                .stream()
                .filter(session -> session.getClassDate().isAfter(today))
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
        return studentMapper.toResponseList(studentRepository.searchByName(query))
                .stream().map(this::enrichWithConvertedPrices).toList();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /**
     * Populates the {@code convertedPrices} field on a response by delegating
     * to the {@link CurrencyConversionService}.
     */
    private StudentResponse enrichWithConvertedPrices(StudentResponse response) {
        if (response.getPricePerClass() != null && response.getCurrency() != null) {
            response.setConvertedPrices(
                    currencyConversionService.convertToAll(
                            response.getPricePerClass(), response.getCurrency()));
        }
        return response;
    }

    private Student findActiveStudent(Long id) {
        return studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }
}
