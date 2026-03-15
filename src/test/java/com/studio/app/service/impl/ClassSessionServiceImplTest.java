package com.studio.app.service.impl;

import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.MovePaymentRequest;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.entity.ClassSession;
import com.studio.app.entity.PackagePurchase;
import com.studio.app.entity.Student;
import com.studio.app.enums.*;
import com.studio.app.exception.BadRequestException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.ClassSessionMapper;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PackagePurchaseRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.service.CurrencyConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceImplTest {

    @Mock ClassSessionRepository sessionRepository;
    @Mock StudentRepository studentRepository;
    @Mock PackagePurchaseRepository packageRepository;
    @Mock ClassSessionMapper sessionMapper;
    @Mock CurrencyConversionService currencyConversionService;

    @InjectMocks ClassSessionServiceImpl sessionService;

    private Student perClassStudent;
    private Student packageStudent;
    private ClassSession session;
    private ClassSessionResponse sessionResponse;

    @BeforeEach
    void setUp() {
        perClassStudent = Student.builder()
                .id(1L).firstName("Ana").lastName("García")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("30.00"))
                .currency(Currency.EUROS)
                .timezone(StudioTimezone.SPAIN)
                .build();

        packageStudent = Student.builder()
                .id(2L).firstName("Ivan").lastName("Petrov")
                .pricingType(PricingType.PACKAGE)
                .currency(Currency.RUBLES)
                .timezone(StudioTimezone.RUSSIA_MOSCOW)
                .build();

        session = ClassSession.builder()
                .id(10L).student(perClassStudent)
                .classDate(LocalDate.of(2026, 3, 15))
                .startTime(LocalTime.of(10, 0))
                .durationMinutes(60)
                .priceCharged(new BigDecimal("30.00"))
                .currency(Currency.EUROS)
                .build();

        sessionResponse = ClassSessionResponse.builder()
                .id(10L).studentId(1L).studentName("Ana García")
                .classDate(LocalDate.of(2026, 3, 15))
                .startTime(LocalTime.of(10, 0))
                .durationMinutes(60)
                .priceCharged(new BigDecimal("30.00"))
                .currency(Currency.EUROS)
                .status(ClassStatus.SCHEDULED)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
    }

    @Nested
    class CreateOneOffSession {

        @Test
        void shouldCreateSessionWithStudentCurrency() {
            var request = OneOffSessionRequest.builder()
                    .classDate(LocalDate.of(2026, 3, 20))
                    .startTime(LocalTime.of(14, 0))
                    .durationMinutes(45)
                    .note("Extra class")
                    .build();

            when(studentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(perClassStudent));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(any())).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.createOneOffSession(1L, request);

            ArgumentCaptor<ClassSession> captor = ArgumentCaptor.forClass(ClassSession.class);
            verify(sessionRepository).save(captor.capture());

            var saved = captor.getValue();
            assertThat(saved.getCurrency()).isEqualTo(Currency.EUROS);
            assertThat(saved.getPriceCharged()).isEqualByComparingTo("30.00");
            assertThat(saved.isOneOff()).isTrue();
            assertThat(saved.getNote()).isEqualTo("Extra class");
        }

        @Test
        void shouldThrowNotFound_whenStudentMissing() {
            when(studentRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.createOneOffSession(99L,
                    OneOffSessionRequest.builder()
                            .classDate(LocalDate.now()).startTime(LocalTime.of(10, 0))
                            .durationMinutes(60).build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetSessions {

        @Test
        void getById_shouldEnrichWithConvertedPrices() {
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(new BigDecimal("30.00"), Currency.EUROS))
                    .thenReturn(Collections.emptyMap());

            sessionService.getSessionById(10L);

            verify(currencyConversionService).convertToAll(new BigDecimal("30.00"), Currency.EUROS);
        }

        @Test
        void getById_shouldThrowNotFound() {
            when(sessionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.getSessionById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void getForStudent_withDateRange_shouldUseRangeQuery() {
            var from = LocalDate.of(2026, 3, 1);
            var to = LocalDate.of(2026, 3, 31);

            when(sessionRepository.findByStudentIdAndDateRange(1L, from, to))
                    .thenReturn(List.of(session));
            when(sessionMapper.toResponseList(List.of(session)))
                    .thenReturn(List.of(sessionResponse));
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            var result = sessionService.getSessionsForStudent(1L, from, to);

            assertThat(result).hasSize(1);
            verify(sessionRepository).findByStudentIdAndDateRange(1L, from, to);
        }

        @Test
        void getForStudent_withoutDateRange_shouldUseAllQuery() {
            when(sessionRepository.findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(1L))
                    .thenReturn(List.of(session));
            when(sessionMapper.toResponseList(List.of(session)))
                    .thenReturn(List.of(sessionResponse));
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.getSessionsForStudent(1L, null, null);

            verify(sessionRepository).findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(1L);
        }
    }

    @Nested
    class GetCalendar {

        @Test
        void shouldCalculateTotalAndCompletedHoursPerDay() {
            var day1Completed = ClassSession.builder()
                    .id(100L).student(perClassStudent)
                    .classDate(LocalDate.of(2026, 3, 20))
                    .startTime(LocalTime.of(9, 0))
                    .durationMinutes(90)
                    .status(ClassStatus.COMPLETED)
                    .build();

            var day1Scheduled = ClassSession.builder()
                    .id(101L).student(perClassStudent)
                    .classDate(LocalDate.of(2026, 3, 20))
                    .startTime(LocalTime.of(11, 0))
                    .durationMinutes(30)
                    .status(ClassStatus.SCHEDULED)
                    .build();

            var day2Completed = ClassSession.builder()
                    .id(102L).student(perClassStudent)
                    .classDate(LocalDate.of(2026, 3, 21))
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
                    .status(ClassStatus.COMPLETED)
                    .build();

            when(sessionRepository.findCalendarSessions(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 21)))
                    .thenReturn(List.of(day1Completed, day1Scheduled, day2Completed));

            when(sessionMapper.toResponseList(anyList())).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                var entities = (List<ClassSession>) invocation.getArgument(0);
                return entities.stream().map(entity -> ClassSessionResponse.builder()
                        .id(entity.getId())
                        .classDate(entity.getClassDate())
                        .startTime(entity.getStartTime())
                        .durationMinutes(entity.getDurationMinutes())
                        .status(entity.getStatus())
                        .build()).toList();
            });

            var result = sessionService.getCalendar(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 21));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 3, 20));
            assertThat(result.get(0).getTotalHours()).isEqualByComparingTo("2.00");
            assertThat(result.get(0).getCompletedHours()).isEqualByComparingTo("1.50");
            assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2026, 3, 21));
            assertThat(result.get(1).getTotalHours()).isEqualByComparingTo("1.00");
            assertThat(result.get(1).getCompletedHours()).isEqualByComparingTo("1.00");
        }
    }

    @Nested
    class UpdateSession {

        @Test
        void shouldUpdateDateStatusAndNoteInOneRequest() {
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.updateSession(10L, UpdateSessionRequest.builder()
                    .classDate(LocalDate.of(2026, 3, 22))
                    .status(ClassStatus.COMPLETED)
                    .note("Conducted online")
                    .build());

            assertThat(session.getClassDate()).isEqualTo(LocalDate.of(2026, 3, 22));
            assertThat(session.getStatus()).isEqualTo(ClassStatus.COMPLETED);
            assertThat(session.getNote()).isEqualTo("Conducted online");
        }

        @Test
        void shouldTogglePaymentToPaidFromUpdateEndpoint() {
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.updateSession(10L, UpdateSessionRequest.builder()
                    .paid(true)
                    .amountOverride(new BigDecimal("28.00"))
                    .build());

            assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(session.getPriceCharged()).isEqualByComparingTo("28.00");
        }

        @Test
        void shouldTogglePaymentToUnpaidFromUpdateEndpoint() {
            session.setPaymentStatus(PaymentStatus.PAID);
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.updateSession(10L, UpdateSessionRequest.builder()
                    .paid(false)
                    .build());

            assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        }
    }

    @Nested
    class CancelSession {

        @Test
        void shouldCancelAndRevertToUnpaid_whenNotKeepPaid() {
            session.setPaymentStatus(PaymentStatus.PAID);

            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.cancelSession(10L, CancelSessionRequest.builder()
                    .keepAsPaid(false).build());

            assertThat(session.getStatus()).isEqualTo(ClassStatus.CANCELLED);
            assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        }

        @Test
        void shouldCancelAndKeepPaid() {
            session.setPaymentStatus(PaymentStatus.PAID);

            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.cancelSession(10L, CancelSessionRequest.builder()
                    .keepAsPaid(true).build());

            assertThat(session.getStatus()).isEqualTo(ClassStatus.CANCELLED);
            assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        void shouldReturnPackageSlot_whenCancellingPackageSession() {
            var pkg = PackagePurchase.builder().id(5L).classesRemaining(2).build();
            session.setPaymentStatus(PaymentStatus.PACKAGE);
            session.setPackagePurchase(pkg);

            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.cancelSession(10L, CancelSessionRequest.builder()
                    .keepAsPaid(false).build());

            assertThat(pkg.getClassesRemaining()).isEqualTo(3);
            assertThat(session.getPackagePurchase()).isNull();
            verify(packageRepository).save(pkg);
        }

        @Test
        void shouldThrow_whenAlreadyCancelled() {
            session.setStatus(ClassStatus.CANCELLED);
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> sessionService.cancelSession(10L,
                    CancelSessionRequest.builder().keepAsPaid(false).build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already cancelled");
        }
    }

    @Nested
    class MarkSessionPaid {

        @Test
        void shouldMarkPerClassAsPaid() {
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.markSessionPaid(10L, PaySessionRequest.builder().build());

            assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        void shouldApplyAmountOverride() {
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.markSessionPaid(10L, PaySessionRequest.builder()
                    .amountOverride(new BigDecimal("25.00")).build());

            assertThat(session.getPriceCharged()).isEqualByComparingTo("25.00");
        }

        @Test
        void shouldDeductFromPackage_forPackageStudent() {
            var pkgSession = ClassSession.builder()
                    .id(20L).student(packageStudent)
                    .classDate(LocalDate.of(2026, 3, 15))
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
                    .build();

            var pkg = PackagePurchase.builder()
                    .id(5L).student(packageStudent)
                    .totalClasses(10).classesRemaining(8)
                    .amountPaid(new BigDecimal("15000.00"))
                    .currency(Currency.RUBLES)
                    .build();

            when(sessionRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(pkgSession));
            when(packageRepository.findActivePackagesByStudent(2L)).thenReturn(List.of(pkg));
            when(sessionRepository.save(any())).thenReturn(pkgSession);
            when(sessionMapper.toResponse(pkgSession)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.markSessionPaid(20L, PaySessionRequest.builder().build());

            assertThat(pkgSession.getPaymentStatus()).isEqualTo(PaymentStatus.PACKAGE);
            assertThat(pkgSession.getPackagePurchase()).isEqualTo(pkg);
            assertThat(pkg.getClassesRemaining()).isEqualTo(7);
        }

        @Test
        void shouldThrow_whenAlreadyPaid() {
            session.setPaymentStatus(PaymentStatus.PAID);
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> sessionService.markSessionPaid(10L,
                    PaySessionRequest.builder().build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already paid");
        }

        @Test
        void shouldThrow_whenNoActivePackage() {
            var pkgSession = ClassSession.builder()
                    .id(20L).student(packageStudent)
                    .classDate(LocalDate.now()).startTime(LocalTime.of(10, 0))
                    .durationMinutes(60).build();

            when(sessionRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(pkgSession));
            when(packageRepository.findActivePackagesByStudent(2L)).thenReturn(List.of());

            assertThatThrownBy(() -> sessionService.markSessionPaid(20L,
                    PaySessionRequest.builder().build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No active package");
        }
    }

    @Nested
    class CancelPayment {

        @Test
        void shouldRevertPaidToUnpaid() {
            session.setPaymentStatus(PaymentStatus.PAID);
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.cancelSessionPayment(10L);

            assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        }

        @Test
        void shouldReturnSlotToPackage() {
            var pkg = PackagePurchase.builder().id(5L).classesRemaining(2).build();
            session.setPaymentStatus(PaymentStatus.PACKAGE);
            session.setPackagePurchase(pkg);

            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.cancelSessionPayment(10L);

            assertThat(pkg.getClassesRemaining()).isEqualTo(3);
            assertThat(session.getPackagePurchase()).isNull();
        }

        @Test
        void shouldThrow_whenAlreadyUnpaid() {
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> sessionService.cancelSessionPayment(10L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("no payment to cancel");
        }
    }

    @Nested
    class MovePayment {

        @Test
        void shouldTransferPaymentBetweenSessions() {
            session.setPaymentStatus(PaymentStatus.PAID);

            var target = ClassSession.builder()
                    .id(11L).student(perClassStudent)
                    .classDate(LocalDate.of(2026, 3, 16))
                    .startTime(LocalTime.of(10, 0))
                    .durationMinutes(60)
                    .build();

            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(target));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sessionMapper.toResponse(target)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.movePayment(10L, MovePaymentRequest.builder()
                    .targetSessionId(11L).build());

            assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(target.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(target.getPriceCharged()).isEqualByComparingTo("30.00");
        }

        @Test
        void shouldThrow_whenSourceNotPaid() {
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> sessionService.movePayment(10L,
                    MovePaymentRequest.builder().targetSessionId(11L).build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PAID status");
        }

        @Test
        void shouldThrow_whenTargetAlreadyPaid() {
            session.setPaymentStatus(PaymentStatus.PAID);
            var target = ClassSession.builder()
                    .id(11L).student(perClassStudent)
                    .classDate(LocalDate.now()).startTime(LocalTime.of(10, 0))
                    .durationMinutes(60).build();
            target.setPaymentStatus(PaymentStatus.PAID);

            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> sessionService.movePayment(10L,
                    MovePaymentRequest.builder().targetSessionId(11L).build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Target session is already paid");
        }
    }

    @Nested
    class CompleteSession {

        @Test
        void shouldMarkCompleted() {
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.setSessionCompletion(10L, true);

            assertThat(session.getStatus()).isEqualTo(ClassStatus.COMPLETED);
        }

        @Test
        void shouldMarkIncompletedToScheduled() {
            session.setStatus(ClassStatus.COMPLETED);
            when(sessionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
            when(currencyConversionService.convertToAll(any(), any())).thenReturn(Collections.emptyMap());

            sessionService.setSessionCompletion(10L, false);

            assertThat(session.getStatus()).isEqualTo(ClassStatus.SCHEDULED);
        }
    }
}

