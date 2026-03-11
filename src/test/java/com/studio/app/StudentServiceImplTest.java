package com.studio.app;

import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.entity.ClassSession;
import com.studio.app.entity.Student;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import com.studio.app.exception.ConflictException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.StudentMapper;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PayerRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.repository.WeeklyScheduleRepository;
import com.studio.app.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock StudentRepository studentRepository;
    @Mock WeeklyScheduleRepository weeklyScheduleRepository;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock PayerRepository payerRepository;
    @Mock StudentMapper studentMapper;

    @InjectMocks StudentServiceImpl studentService;

    private Student activeStudent;

    @BeforeEach
    void setUp() {
        activeStudent = Student.builder()
                .id(1L)
                .firstName("Ana")
                .lastName("García")
                .email("ana@studio.com")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("30.00"))
                .timezone(StudioTimezone.SPAIN)
                .build();
    }

    @Test
    void createStudent_shouldPersistAndReturn() {
        var request = CreateStudentRequest.builder()
                .firstName("Ana").lastName("García")
                .email("ana@studio.com")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("30.00"))
                .timezone(StudioTimezone.SPAIN)
                .build();

        when(studentRepository.existsByEmailAndDeletedFalse("ana@studio.com")).thenReturn(false);
        when(studentRepository.save(any())).thenReturn(activeStudent);
        when(studentMapper.toResponse(activeStudent)).thenReturn(null);

        studentService.createStudent(request);

        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void createStudent_shouldThrowConflict_whenEmailExists() {
        var request = CreateStudentRequest.builder()
                .firstName("Ana").lastName("García")
                .email("ana@studio.com")
                .pricingType(PricingType.PER_CLASS)
                .timezone(StudioTimezone.SPAIN)
                .build();

        when(studentRepository.existsByEmailAndDeletedFalse("ana@studio.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void getStudentById_shouldThrowNotFound_whenDeleted() {
        when(studentRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found with id: 99");
    }

    @Test
    void deleteStudent_shouldSoftDeleteFutureSessionsOnly() {
        var pastSession = ClassSession.builder()
                .id(10L)
                .student(activeStudent)
                .classDate(LocalDate.now().minusDays(3))
                .build();

        var todaySession = ClassSession.builder()
                .id(11L)
                .student(activeStudent)
                .classDate(LocalDate.now())
                .build();

        var futureSession = ClassSession.builder()
                .id(12L)
                .student(activeStudent)
                .classDate(LocalDate.now().plusDays(7))
                .build();

        when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
        when(weeklyScheduleRepository.findByStudentIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(classSessionRepository.findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(1L))
                .thenReturn(List.of(pastSession, todaySession, futureSession));
        when(payerRepository.findByStudentIdAndDeletedFalse(1L)).thenReturn(List.of());

        studentService.deleteStudent(1L);

        assertThat(activeStudent.isDeleted()).isTrue();
        assertThat(pastSession.isDeleted()).isFalse();
        assertThat(todaySession.isDeleted()).isFalse();
        assertThat(futureSession.isDeleted()).isTrue();
        verify(studentRepository).save(activeStudent);
    }

    @Test
    void updateStudent_shouldApplyOnlyNonNullFields() {
        when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
        when(studentRepository.save(any())).thenReturn(activeStudent);
        when(studentMapper.toResponse(activeStudent)).thenReturn(null);

        var request = UpdateStudentRequest.builder()
                .firstName("Anita")
                .build();

        studentService.updateStudent(1L, request);

        assertThat(activeStudent.getFirstName()).isEqualTo("Anita");
        assertThat(activeStudent.getLastName()).isEqualTo("García"); // unchanged
    }
}
