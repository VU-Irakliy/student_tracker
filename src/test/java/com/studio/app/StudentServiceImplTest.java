package com.studio.app;

import com.studio.app.dto.request.CreateStudentRequest;
import com.studio.app.dto.request.UpdateStudentRequest;
import com.studio.app.dto.response.StudentResponse;
import com.studio.app.entity.ClassSession;
import com.studio.app.entity.Payer;
import com.studio.app.entity.Student;
import com.studio.app.entity.WeeklySchedule;
import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudentClassType;
import com.studio.app.enums.StudioTimezone;
import com.studio.app.exception.BadRequestException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.StudentMapper;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PayerRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.repository.WeeklyScheduleRepository;
import com.studio.app.service.CurrencyConversionService;
import com.studio.app.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    @Mock CurrencyConversionService currencyConversionService;

    @InjectMocks StudentServiceImpl studentService;

    private Student activeStudent;
    private StudentResponse studentResponse;

    @BeforeEach
    void setUp() {
        activeStudent = Student.builder()
                .id(1L)
                .firstName("Ana")
                .lastName("García")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("30.00"))
                .currency(Currency.EUROS)
                .timezone(StudioTimezone.SPAIN)
                .classType(StudentClassType.CASUAL)
                .build();

        studentResponse = StudentResponse.builder()
                .id(1L)
                .firstName("Ana")
                .lastName("García")
                .fullName("Ana García")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("30.00"))
                .currency(Currency.EUROS)
                .classType(StudentClassType.CASUAL)
                .build();
    }

    @Nested
    class CreateStudent {

        @Test
        void shouldPersistAndReturn() {
            var request = CreateStudentRequest.builder()
                    .firstName("Ana").lastName("García")
                    .pricingType(PricingType.PER_CLASS)
                    .pricePerClass(new BigDecimal("30.00"))
                    .currency(Currency.EUROS)
                    .timezone(StudioTimezone.SPAIN)
                    .classType(StudentClassType.EGE)
                    .build();

            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(studentResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = studentService.createStudent(request);

            verify(studentRepository).save(any(Student.class));
            assertThat(result.getFirstName()).isEqualTo("Ana");
        }

        @Test
        void shouldSetCurrencyFromRequest() {
            var request = CreateStudentRequest.builder()
                    .firstName("Ivan").lastName("Petrov")
                    .pricingType(PricingType.PER_CLASS)
                    .pricePerClass(new BigDecimal("2000.00"))
                    .currency(Currency.RUBLES)
                    .timezone(StudioTimezone.RUSSIA_MOSCOW)
                    .classType(StudentClassType.OGE)
                    .build();

            var rubStudent = Student.builder()
                    .id(2L).firstName("Ivan").lastName("Petrov")
                    .currency(Currency.RUBLES).pricePerClass(new BigDecimal("2000.00"))
                    .pricingType(PricingType.PER_CLASS).timezone(StudioTimezone.RUSSIA_MOSCOW)
                    .classType(StudentClassType.OGE)
                    .build();

            var rubResponse = StudentResponse.builder()
                    .pricePerClass(new BigDecimal("2000.00"))
                    .currency(Currency.RUBLES)
                    .build();

            when(studentRepository.save(any())).thenReturn(rubStudent);
            when(studentMapper.toResponse(rubStudent)).thenReturn(rubResponse);
            when(currencyConversionService.convertToAll(any(), eq(Currency.RUBLES)))
                    .thenReturn(Map.of(
                            Currency.RUBLES, new BigDecimal("2000.00"),
                            Currency.DOLLARS, new BigDecimal("21.60"),
                            Currency.EUROS, new BigDecimal("19.80")
                    ));

            var result = studentService.createStudent(request);

            assertThat(result.getConvertedPrices()).containsKey(Currency.DOLLARS);
        }

        @Test
        void shouldAllowPackageStudentWithoutPriceAndCurrency() {
            var request = CreateStudentRequest.builder()
                    .firstName("Pkg").lastName("Student")
                    .pricingType(PricingType.PACKAGE)
                    .timezone(StudioTimezone.SPAIN)
                    .classType(StudentClassType.CASUAL)
                    .build();

            var pkgStudent = Student.builder()
                    .id(3L).firstName("Pkg").lastName("Student")
                    .pricingType(PricingType.PACKAGE).timezone(StudioTimezone.SPAIN)
                    .classType(StudentClassType.CASUAL)
                    .build();

            var pkgResponse = StudentResponse.builder()
                    .pricingType(PricingType.PACKAGE).build();

            when(studentRepository.save(any())).thenReturn(pkgStudent);
            when(studentMapper.toResponse(pkgStudent)).thenReturn(pkgResponse);

            var result = studentService.createStudent(request);

            assertThat(result.getConvertedPrices()).isNull();
        }

        @Test
        void shouldRejectHolidayModeWithoutHolidayFrom() {
            var request = CreateStudentRequest.builder()
                    .firstName("Ana")
                    .lastName("Holiday")
                    .pricingType(PricingType.PER_CLASS)
                    .pricePerClass(new BigDecimal("30.00"))
                    .currency(Currency.EUROS)
                    .timezone(StudioTimezone.SPAIN)
                    .holidayMode(true)
                    .build();

            assertThatThrownBy(() -> studentService.createStudent(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("holidayFrom");
        }

        @Test
        void shouldRejectPerClassWithoutPrice() {
            var request = CreateStudentRequest.builder()
                    .firstName("Ana")
                    .lastName("NoPrice")
                    .pricingType(PricingType.PER_CLASS)
                    .currency(Currency.EUROS)
                    .timezone(StudioTimezone.SPAIN)
                    .build();

            assertThatThrownBy(() -> studentService.createStudent(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("pricePerClass");
        }

        @Test
        void shouldRejectPriceWithoutCurrency() {
            var request = CreateStudentRequest.builder()
                    .firstName("Ana")
                    .lastName("NoCurrency")
                    .pricingType(PricingType.PER_CLASS)
                    .pricePerClass(new BigDecimal("30.00"))
                    .timezone(StudioTimezone.SPAIN)
                    .build();

            assertThatThrownBy(() -> studentService.createStudent(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("currency");
        }

        @Test
        void shouldRejectHolidayToWithoutHolidayFrom() {
            var request = CreateStudentRequest.builder()
                    .firstName("Ana")
                    .lastName("Holiday")
                    .pricingType(PricingType.PER_CLASS)
                    .pricePerClass(new BigDecimal("30.00"))
                    .currency(Currency.EUROS)
                    .timezone(StudioTimezone.SPAIN)
                    .holidayTo(LocalDate.of(2026, 4, 1))
                    .build();

            assertThatThrownBy(() -> studentService.createStudent(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("holidayFrom");
        }
    }

    @Nested
    class GetStudent {

        @Test
        void getById_shouldReturnStudent() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(studentMapper.toResponse(activeStudent)).thenReturn(studentResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = studentService.getStudentById(1L);

            assertThat(result.getFirstName()).isEqualTo("Ana");
        }

        @Test
        void getById_shouldThrowNotFound_whenDeleted() {
            when(studentRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.getStudentById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Student not found with id: 99");
        }

        @Test
        void getAllStudents_shouldReturnOnlyActive() {
            when(studentRepository.findAllByDeletedFalse()).thenReturn(List.of(activeStudent));
            when(studentMapper.toResponse(activeStudent)).thenReturn(studentResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = studentService.getAllStudents();

            assertThat(result).hasSize(1);
        }

        @Test
        void getAllStudents_shouldFilterByDebtor_whenParamPresent() {
            when(studentRepository.findAllByDeletedFalseAndDebtor(true)).thenReturn(List.of(activeStudent));
            when(studentMapper.toResponse(activeStudent)).thenReturn(studentResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = studentService.getAllStudents(true);

            assertThat(result).hasSize(1);
            verify(studentRepository).findAllByDeletedFalseAndDebtor(true);
            verify(studentRepository, never()).findAllByDeletedFalse();
        }

        @Test
        void searchStudents_shouldDelegateToRepository() {
            when(studentRepository.searchByName("Ana")).thenReturn(List.of(activeStudent));
            when(studentMapper.toResponse(activeStudent)).thenReturn(studentResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = studentService.searchStudents("Ana");

            assertThat(result).hasSize(1);
            verify(studentRepository).searchByName("Ana");
        }

        @Test
        void searchStudents_shouldUseDebtorAwareQuery_whenDebtorFilterPresent() {
            when(studentRepository.searchByNameAndDebtor("Ana", true)).thenReturn(List.of(activeStudent));
            when(studentMapper.toResponse(activeStudent)).thenReturn(studentResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = studentService.searchStudents("Ana", true);

            assertThat(result).hasSize(1);
            verify(studentRepository).searchByNameAndDebtor("Ana", true);
            verify(studentRepository, never()).searchByName("Ana");
        }

        @Test
        void searchStudentsByStudentOrPayerName_shouldDelegateToRepository() {
            when(studentRepository.searchByStudentOrPayerName("María")).thenReturn(List.of(activeStudent));
            when(studentMapper.toResponse(activeStudent)).thenReturn(studentResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = studentService.searchStudentsByStudentOrPayerName("  María ");

            assertThat(result).hasSize(1);
            verify(studentRepository).searchByStudentOrPayerName("María");
        }

        @Test
        void searchStudentsByStudentOrPayerName_shouldRejectBlankQuery() {
            assertThatThrownBy(() -> studentService.searchStudentsByStudentOrPayerName("   "))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("query is required");
        }
    }

    @Nested
    class UpdateStudent {

        @Test
        void shouldApplyOnlyNonNullFields() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(studentResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var request = UpdateStudentRequest.builder()
                    .firstName("Anita")
                    .build();

            studentService.updateStudent(1L, request);

            assertThat(activeStudent.getFirstName()).isEqualTo("Anita");
            assertThat(activeStudent.getLastName()).isEqualTo("García");
        }

        @Test
        void shouldUpdateCurrency() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(
                    StudentResponse.builder()
                            .pricePerClass(new BigDecimal("30.00"))
                            .currency(Currency.DOLLARS)
                            .build());
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .currency(Currency.DOLLARS).build());

            assertThat(activeStudent.getCurrency()).isEqualTo(Currency.DOLLARS);
        }

        @Test
        void shouldUpdatePricePerClass() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(
                    StudentResponse.builder()
                            .pricePerClass(new BigDecimal("50.00"))
                            .currency(Currency.EUROS)
                            .build());
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .pricePerClass(new BigDecimal("50.00")).build());

            assertThat(activeStudent.getPricePerClass()).isEqualByComparingTo("50.00");
        }

        @Test
        void shouldUpdatePricingType() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(
                    StudentResponse.builder().pricingType(PricingType.PACKAGE).build());

            studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .pricingType(PricingType.PACKAGE).build());

            assertThat(activeStudent.getPricingType()).isEqualTo(PricingType.PACKAGE);
            assertThat(activeStudent.getPricePerClass()).isNull();
            assertThat(activeStudent.getCurrency()).isNull();
        }

        @Test
        void shouldUpdateTimezone() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(
                    StudentResponse.builder().build());

            studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .timezone(StudioTimezone.RUSSIA_MOSCOW).build());

            assertThat(activeStudent.getTimezone()).isEqualTo(StudioTimezone.RUSSIA_MOSCOW);
        }

        @Test
        void shouldUpdateNotes() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(
                    StudentResponse.builder().build());

            studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .notes("Updated note").build());

            assertThat(activeStudent.getNotes()).isEqualTo("Updated note");
        }

        @Test
        void shouldUpdateClassType() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(StudentResponse.builder().build());

            studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .classType(StudentClassType.IELTS).build());

            assertThat(activeStudent.getClassType()).isEqualTo(StudentClassType.IELTS);
        }

        @Test
        void shouldEnableHolidayModeAndCancelFutureSessions() {
            var futureSession = ClassSession.builder()
                    .id(12L).student(activeStudent)
                    .classDate(LocalDate.of(2026, 3, 20))
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(classSessionRepository.findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(1L))
                    .thenReturn(List.of(futureSession));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(StudentResponse.builder().build());

            studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .holidayMode(true)
                    .holidayFrom(LocalDate.of(2026, 3, 15))
                    .build());

            assertThat(activeStudent.isHolidayMode()).isTrue();
            assertThat(futureSession.getStatus().name()).isEqualTo("CANCELLED");
        }

        @Test
        void shouldUpdateStoppedAttendingFlag() {
            var pastSession = ClassSession.builder()
                    .id(20L).student(activeStudent)
                    .classDate(LocalDate.now().minusDays(1))
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
                    .build();
            var todaySession = ClassSession.builder()
                    .id(21L).student(activeStudent)
                    .classDate(LocalDate.now())
                    .startTime(LocalTime.of(11, 0))
                    .durationMinutes(60)
                    .build();
            var futureSession = ClassSession.builder()
                    .id(22L).student(activeStudent)
                    .classDate(LocalDate.now().plusDays(5))
                    .startTime(LocalTime.of(12, 0))
                    .durationMinutes(60)
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(classSessionRepository.findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(1L))
                    .thenReturn(List.of(pastSession, todaySession, futureSession));
            when(studentRepository.save(any())).thenReturn(activeStudent);
            when(studentMapper.toResponse(activeStudent)).thenReturn(StudentResponse.builder().build());

            studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .stoppedAttending(true)
                    .build());

            assertThat(activeStudent.isStoppedAttending()).isTrue();
            assertThat(pastSession.isDeleted()).isFalse();
            assertThat(todaySession.isDeleted()).isFalse();
            assertThat(futureSession.isDeleted()).isTrue();
        }

        @Test
        void shouldRejectBlankFirstNameOnUpdate() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));

            assertThatThrownBy(() -> studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .firstName("   ")
                    .build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("firstName and lastName are required");
        }

        @Test
        void shouldRejectPerClassUpdateWithoutPrice() {
            var packageStudent = Student.builder()
                    .id(10L)
                    .firstName("Pkg")
                    .lastName("Student")
                    .pricingType(PricingType.PACKAGE)
                    .pricePerClass(null)
                    .timezone(StudioTimezone.SPAIN)
                    .classType(StudentClassType.CASUAL)
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(packageStudent));

            assertThatThrownBy(() -> studentService.updateStudent(10L, UpdateStudentRequest.builder()
                    .pricingType(PricingType.PER_CLASS)
                    .build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("pricePerClass");
        }

        @Test
        void shouldRejectPriceUpdateWithoutCurrency() {
            var studentWithoutCurrency = Student.builder()
                    .id(11L)
                    .firstName("Ana")
                    .lastName("NoCurrency")
                    .pricingType(PricingType.PER_CLASS)
                    .pricePerClass(new BigDecimal("30.00"))
                    .currency(null)
                    .timezone(StudioTimezone.SPAIN)
                    .classType(StudentClassType.CASUAL)
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(studentWithoutCurrency));

            assertThatThrownBy(() -> studentService.updateStudent(11L, UpdateStudentRequest.builder()
                    .notes("touch")
                    .build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("currency");
        }

        @Test
        void shouldRejectPackageUpdateWithPriceOrCurrency() {
            var packageStudent = Student.builder()
                    .id(10L)
                    .firstName("Pkg")
                    .lastName("Student")
                    .pricingType(PricingType.PACKAGE)
                    .pricePerClass(null)
                    .currency(null)
                    .timezone(StudioTimezone.SPAIN)
                    .classType(StudentClassType.CASUAL)
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(packageStudent));

            assertThatThrownBy(() -> studentService.updateStudent(10L, UpdateStudentRequest.builder()
                    .pricePerClass(new BigDecimal("40.00"))
                    .build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("must be null");
        }

        @Test
        void shouldRejectHolidayToWithoutHolidayFromOnUpdate() {
            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));

            assertThatThrownBy(() -> studentService.updateStudent(1L, UpdateStudentRequest.builder()
                    .holidayTo(LocalDate.of(2026, 4, 1))
                    .build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("holidayFrom");
        }

        @Test
        void shouldThrowNotFound_whenStudentMissing() {
            when(studentRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.updateStudent(99L,
                    UpdateStudentRequest.builder().firstName("X").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class DeleteStudent {

        @Test
        void shouldSoftDeleteFutureSessionsOnly() {
            var pastSession = ClassSession.builder()
                    .id(10L).student(activeStudent)
                    .classDate(LocalDate.now().minusDays(3))
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
                    .build();

            var todaySession = ClassSession.builder()
                    .id(11L).student(activeStudent)
                    .classDate(LocalDate.now())
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
                    .build();

            var futureSession = ClassSession.builder()
                    .id(12L).student(activeStudent)
                    .classDate(LocalDate.now().plusDays(7))
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
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
        void shouldSoftDeleteWeeklySchedules() {
            var schedule = WeeklySchedule.builder()
                    .id(1L).student(activeStudent)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(weeklyScheduleRepository.findByStudentIdAndDeletedFalse(1L)).thenReturn(List.of(schedule));
            when(classSessionRepository.findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(1L))
                    .thenReturn(List.of());
            when(payerRepository.findByStudentIdAndDeletedFalse(1L)).thenReturn(List.of());

            studentService.deleteStudent(1L);

            assertThat(schedule.isDeleted()).isTrue();
        }

        @Test
        void shouldSoftDeletePayers() {
            var payer = Payer.builder()
                    .id(1L).student(activeStudent)
                    .fullName("Parent").build();

            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeStudent));
            when(weeklyScheduleRepository.findByStudentIdAndDeletedFalse(1L)).thenReturn(List.of());
            when(classSessionRepository.findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(1L))
                    .thenReturn(List.of());
            when(payerRepository.findByStudentIdAndDeletedFalse(1L)).thenReturn(List.of(payer));

            studentService.deleteStudent(1L);

            assertThat(payer.isDeleted()).isTrue();
        }

        @Test
        void shouldThrowNotFound_whenStudentMissing() {
            when(studentRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> studentService.deleteStudent(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
