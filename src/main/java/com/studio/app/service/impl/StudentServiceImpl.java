package com.studio.app.service.impl;

import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.dto.response.StudentResponse;
import com.studio.app.enums.ClassStatus;
import com.studio.app.entity.Student;
import com.studio.app.enums.StudentClassType;
import com.studio.app.exception.BadRequestException;
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

    private static final String HOLIDAY_AUTO_NOTE_PREFIX = "[AUTO_HOLIDAY]";

    private final StudentRepository studentRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final ClassSessionRepository classSessionRepository;
    private final PayerRepository payerRepository;
    private final StudentMapper studentMapper;
    private final CurrencyConversionService currencyConversionService;

    /** {@inheritDoc} */
    @Override
    public StudentResponse createStudent(CreateStudentRequest request) {
        validateCreateLifecycle(request);

        var student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .pricingType(request.getPricingType())
                .pricePerClass(request.getPricePerClass())
                .currency(request.getCurrency())
                .timezone(request.getTimezone())
                .classType(Optional.ofNullable(request.getClassType()).orElse(StudentClassType.CASUAL))
                .startDate(request.getStartDate())
                .holidayMode(Boolean.TRUE.equals(request.getHolidayMode()))
                .holidayFrom(request.getHolidayFrom())
                .holidayTo(request.getHolidayTo())
                .stoppedAttending(Boolean.TRUE.equals(request.getStoppedAttending()))
                .notes(request.getNotes())
                .build();

        return toResponse(studentRepository.save(student));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAllByDeletedFalse().stream()
                .map(this::toResponse)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        return toResponse(findActiveStudent(id));
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
        Optional.ofNullable(request.getClassType()).ifPresent(student::setClassType);
        Optional.ofNullable(request.getStartDate()).ifPresent(student::setStartDate);
        applyStoppedAttendingUpdate(student, request.getStoppedAttending());
        Optional.ofNullable(request.getNotes()).ifPresent(student::setNotes);

        applyHolidayStateUpdate(student, request);

        return toResponse(studentRepository.save(student));
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

        payerRepository.findByStudentIdAndDeletedFalse(id)
                .forEach(payer -> payer.setDeleted(true));

        student.setDeleted(true);
        studentRepository.save(student);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> searchStudents(String query) {
        return studentRepository.searchByName(query).stream()
                .map(this::toResponse)
                .toList();
    }


    private StudentResponse toResponse(Student student) {
        var response = studentMapper.toResponse(student);

        response.setClassType(student.getClassType());
        response.setStartDate(student.getStartDate());
        response.setHolidayMode(student.isHolidayMode());
        response.setHolidayFrom(student.getHolidayFrom());
        response.setHolidayTo(student.getHolidayTo());
        response.setStoppedAttending(student.isStoppedAttending());
        return enrichWithConvertedPrices(response);
    }

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

    private void validateCreateLifecycle(CreateStudentRequest request) {
        if (Boolean.TRUE.equals(request.getHolidayMode()) && request.getHolidayFrom() == null) {
            throw new BadRequestException("holidayFrom is required when holidayMode is true");
        }
        if (request.getHolidayFrom() != null && request.getHolidayTo() != null
                && request.getHolidayTo().isBefore(request.getHolidayFrom())) {
            throw new BadRequestException("holidayTo must be on or after holidayFrom");
        }
    }

    private void applyHolidayStateUpdate(Student student, UpdateStudentRequest request) {
        Optional.ofNullable(request.getHolidayFrom()).ifPresent(student::setHolidayFrom);
        Optional.ofNullable(request.getHolidayTo()).ifPresent(student::setHolidayTo);

        if (request.getHolidayMode() != null) {
            if (request.getHolidayMode()) {
                var holidayFrom = Optional.ofNullable(request.getHolidayFrom()).orElse(student.getHolidayFrom());
                if (holidayFrom == null) {
                    throw new BadRequestException("holidayFrom is required when holidayMode is true");
                }
                student.setHolidayMode(true);
                student.setHolidayFrom(holidayFrom);
                student.setHolidayTo(null);
                autoCancelSessionsForHoliday(student.getId(), holidayFrom);
                return;
            }

            if (!student.isHolidayMode()) {
                throw new BadRequestException("Student is not currently on holiday");
            }
            var returnDate = request.getHolidayTo();
            if (returnDate == null) {
                throw new BadRequestException("holidayTo is required when holidayMode is set to false");
            }
            if (student.getHolidayFrom() != null && returnDate.isBefore(student.getHolidayFrom())) {
                throw new BadRequestException("holidayTo must be on or after holidayFrom");
            }

            student.setHolidayMode(false);
            student.setHolidayTo(returnDate);
            restoreAutoCancelledSessionsFrom(student.getId(), returnDate);
            return;
        }

        if (student.getHolidayFrom() != null && student.getHolidayTo() != null
                && student.getHolidayTo().isBefore(student.getHolidayFrom())) {
            throw new BadRequestException("holidayTo must be on or after holidayFrom");
        }
    }

    private void autoCancelSessionsForHoliday(Long studentId, LocalDate holidayFrom) {
        var sessionsToCancel = classSessionRepository
                .findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(studentId)
                .stream()
                .filter(session -> !session.getClassDate().isBefore(holidayFrom))
                .filter(session -> session.getStatus() != ClassStatus.CANCELLED)
                .toList();

        sessionsToCancel.forEach(session -> {
            session.setStatus(ClassStatus.CANCELLED);
            session.setNote(HOLIDAY_AUTO_NOTE_PREFIX + " Cancelled due to student holiday");
        });
        classSessionRepository.saveAll(sessionsToCancel);
    }

    private void restoreAutoCancelledSessionsFrom(Long studentId, LocalDate returnDate) {
        var sessionsToRestore = classSessionRepository
                .findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(studentId)
                .stream()
                .filter(session -> !session.getClassDate().isBefore(returnDate))
                .filter(session -> session.getStatus() == ClassStatus.CANCELLED)
                .filter(session -> session.getNote() != null && session.getNote().startsWith(HOLIDAY_AUTO_NOTE_PREFIX))
                .toList();

        sessionsToRestore.forEach(session -> {
            session.setStatus(ClassStatus.SCHEDULED);
            session.setNote(null);
        });
        classSessionRepository.saveAll(sessionsToRestore);
    }

    private void applyStoppedAttendingUpdate(Student student, Boolean stoppedAttending) {
        if (stoppedAttending == null) {
            return;
        }

        boolean wasStopped = student.isStoppedAttending();
        student.setStoppedAttending(stoppedAttending);

        if (!wasStopped && stoppedAttending) {
            softDeleteSessionsFromCurrentDate(student.getId());
        }
    }

    private void softDeleteSessionsFromCurrentDate(Long studentId) {
        LocalDate today = LocalDate.now();
        classSessionRepository
                .findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(studentId)
                .stream()
                .filter(session -> !session.getClassDate().isBefore(today))
                .forEach(session -> session.setDeleted(true));
    }
}
