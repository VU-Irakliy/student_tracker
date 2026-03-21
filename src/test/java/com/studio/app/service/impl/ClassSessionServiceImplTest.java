package com.studio.app.service.impl;

import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.enums.ClassStatus;
import com.studio.app.enums.Currency;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.StudioTimezone;
import com.studio.app.exception.BadRequestException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PackagePurchaseRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.service.ClassSessionService;
import com.studio.app.support.StubCurrencyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(StubCurrencyTestConfig.class)
@Sql(scripts = "/testdata/service/classsession/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup-test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ClassSessionServiceImplTest {

    @Autowired
    private ClassSessionService sessionService;

    @Autowired
    private ClassSessionRepository sessionRepository;

    @Autowired
    private PackagePurchaseRepository packageRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void shouldCreateOneOffSessionWithStudentCurrencyAndTimezone() {
        var response = sessionService.createOneOffSession(1L, OneOffSessionRequest.builder()
                .classDate(LocalDate.of(2026, 3, 20))
                .startTime(LocalTime.of(14, 0))
                .durationMinutes(45)
                .note("Extra class")
                .build(), StudioTimezone.SPAIN);

        var persisted = sessionRepository.findByIdAndDeletedFalse(response.getId()).orElseThrow();
        assertThat(persisted.getCurrency()).isEqualTo(Currency.EUROS);
        assertThat(persisted.getTimezone()).isEqualTo(StudioTimezone.SPAIN);
        assertThat(persisted.isOneOff()).isTrue();
        assertThat(persisted.getNote()).isEqualTo("Extra class");
    }

    @Test
    void shouldThrowNotFoundWhenCreatingSessionForMissingStudent() {
        assertThatThrownBy(() -> sessionService.createOneOffSession(999L, OneOffSessionRequest.builder()
                .classDate(LocalDate.of(2026, 3, 20))
                .startTime(LocalTime.of(14, 0))
                .durationMinutes(45)
                .build(), StudioTimezone.SPAIN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldConvertSessionTimeFromRussiaToSpain() {
        var response = sessionService.getSessionById(300L, StudioTimezone.SPAIN);

        assertThat(response.getOriginalTimezone()).isEqualTo(StudioTimezone.RUSSIA_MOSCOW);
        assertThat(response.getViewerTimezone()).isEqualTo(StudioTimezone.SPAIN);
        assertThat(response.getOriginalClassDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(response.getOriginalStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.getClassDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void shouldUseRangeQueryWhenDatesProvided() {
        var result = sessionService.getSessionsForStudent(1L,
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 15), StudioTimezone.SPAIN);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(10L);
    }

    @Test
    void shouldCalculateCalendarDayTotals() {
        var result = sessionService.getCalendar(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 21), StudioTimezone.SPAIN);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getDate()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(result.getFirst().getTotalHours()).isEqualByComparingTo("2.00");
        assertThat(result.getFirst().getCompletedHours()).isEqualByComparingTo("1.50");
    }

    @Test
    void shouldUpdateSessionDateStatusAndNote() {
        sessionService.updateSession(10L, UpdateSessionRequest.builder()
                .classDate(LocalDate.of(2026, 3, 22))
                .status(ClassStatus.COMPLETED)
                .note("Conducted online")
                .build(), StudioTimezone.SPAIN);

        var persisted = sessionRepository.findByIdAndDeletedFalse(10L).orElseThrow();
        assertThat(persisted.getClassDate()).isEqualTo(LocalDate.of(2026, 3, 22));
        assertThat(persisted.getStatus()).isEqualTo(ClassStatus.COMPLETED);
        assertThat(persisted.getNote()).isEqualTo("Conducted online");
    }

    @Test
    void shouldTogglePaymentToPaidWithAmountOverride() {
        sessionService.updateSession(10L, UpdateSessionRequest.builder()
                .paid(true)
                .amountOverride(new BigDecimal("28.00"))
                .build(), StudioTimezone.SPAIN);

        var persisted = sessionRepository.findByIdAndDeletedFalse(10L).orElseThrow();
        assertThat(persisted.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(persisted.getPriceCharged()).isEqualByComparingTo("28.00");
    }

    @Test
    void shouldCancelPackageSessionAndReturnPackageSlot() {
        var before = packageRepository.findByIdAndDeletedFalse(1L).orElseThrow();

        sessionService.cancelSession(22L, CancelSessionRequest.builder().keepAsPaid(false).build(), StudioTimezone.SPAIN);

        var after = packageRepository.findByIdAndDeletedFalse(1L).orElseThrow();
        var session = sessionRepository.findByIdAndDeletedFalse(22L).orElseThrow();
        assertThat(after.getClassesRemaining()).isEqualTo(before.getClassesRemaining() + 1);
        assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(session.getPackagePurchase()).isNull();
    }

    @Test
    void shouldAllowCancelledSessionToRemainPaidForPerClassStudent() {
        sessionService.cancelSession(12L, CancelSessionRequest.builder().keepAsPaid(true).build(), StudioTimezone.SPAIN);

        var session = sessionRepository.findByIdAndDeletedFalse(12L).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(ClassStatus.CANCELLED);
        assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void shouldAllowCancelledSessionToRemainPaidForPackageStudent() {
        var before = packageRepository.findByIdAndDeletedFalse(1L).orElseThrow();

        sessionService.cancelSession(22L, CancelSessionRequest.builder().keepAsPaid(true).build(), StudioTimezone.SPAIN);

        var session = sessionRepository.findByIdAndDeletedFalse(22L).orElseThrow();
        var after = packageRepository.findByIdAndDeletedFalse(1L).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(ClassStatus.CANCELLED);
        assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.PACKAGE);
        assertThat(session.getPackagePurchase()).isNotNull();
        assertThat(after.getClassesRemaining()).isEqualTo(before.getClassesRemaining());
    }

    @Test
    void shouldMarkPackageStudentSessionAsPaidUsingOldestActivePackage() {
        sessionService.markSessionPaid(20L, PaySessionRequest.builder().build(), StudioTimezone.SPAIN);

        var session = sessionRepository.findByIdAndDeletedFalse(20L).orElseThrow();
        var pkg = packageRepository.findByIdAndDeletedFalse(1L).orElseThrow();
        assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.PACKAGE);
        assertThat(session.getPackagePurchase().getId()).isEqualTo(1L);
        assertThat(pkg.getClassesRemaining()).isEqualTo(7);
    }

    @Test
    void shouldThrowWhenMarkingPackageStudentSessionPaidWithoutActivePackages() {
        var packageStudent = studentRepository.findByIdAndDeletedFalse(2L).orElseThrow();
        packageRepository.findByStudentIdAndDeletedFalseOrderByPaymentDateDesc(2L).forEach(pkg -> {
            pkg.setClassesRemaining(0);
            packageRepository.save(pkg);
        });

        assertThatThrownBy(() -> sessionService.markSessionPaid(20L, PaySessionRequest.builder().build(), StudioTimezone.SPAIN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no active package assigned");

        // Sanity: dataset still points to PACKAGE student and session remains unpaid.
        assertThat(packageStudent.getPricingType().name()).isEqualTo("PACKAGE");
        assertThat(sessionRepository.findByIdAndDeletedFalse(20L).orElseThrow().getPaymentStatus())
                .isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void shouldThrowWhenCancellingPackagePaymentWithoutLinkedPackage() {
        var broken = sessionRepository.findByIdAndDeletedFalse(20L).orElseThrow();
        broken.setPaymentStatus(PaymentStatus.PACKAGE);
        broken.setPackagePurchase(null);
        sessionRepository.save(broken);

        assertThatThrownBy(() -> sessionService.cancelSessionPayment(20L, StudioTimezone.SPAIN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no linked package");
    }



    @Test
    void shouldCancelPaymentAndReturnSlotToPackage() {
        sessionService.cancelSessionPayment(22L, StudioTimezone.SPAIN);

        var session = sessionRepository.findByIdAndDeletedFalse(22L).orElseThrow();
        var pkg = packageRepository.findByIdAndDeletedFalse(1L).orElseThrow();
        assertThat(session.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(session.getPackagePurchase()).isNull();
        assertThat(pkg.getClassesRemaining()).isEqualTo(9);
    }

    @Test
    void shouldToggleSessionCompletion() {
        sessionService.setSessionCompletion(10L, true, StudioTimezone.SPAIN);
        assertThat(sessionRepository.findByIdAndDeletedFalse(10L).orElseThrow().getStatus())
                .isEqualTo(ClassStatus.COMPLETED);

        sessionService.setSessionCompletion(10L, false, StudioTimezone.SPAIN);
        assertThat(sessionRepository.findByIdAndDeletedFalse(10L).orElseThrow().getStatus())
                .isEqualTo(ClassStatus.SCHEDULED);
    }

    @Test
    void shouldThrowWhenCancellingAlreadyCancelledSession() {
        assertThatThrownBy(() -> sessionService.cancelSession(30L, CancelSessionRequest.builder().build(), StudioTimezone.SPAIN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }
}

