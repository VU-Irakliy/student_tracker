package com.studio.app.service.impl;

import com.studio.app.entity.Student;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtorStatusServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    private Student spainStudent;
    private Student moscowStudent;

    @BeforeEach
    void setUp() {
        spainStudent = Student.builder()
                .id(1L)
                .firstName("Ana")
                .lastName("Garcia")
                .pricingType(PricingType.PER_CLASS)
                .timezone(StudioTimezone.SPAIN)
                .debtor(false)
                .build();

        moscowStudent = Student.builder()
                .id(2L)
                .firstName("Ivan")
                .lastName("Petrov")
                .pricingType(PricingType.PACKAGE)
                .timezone(StudioTimezone.RUSSIA_MOSCOW)
                .debtor(false)
                .build();
    }

    @Test
    void shouldProcessOnlyStudentsWhoseLocalTimeIsAfter22() {
        // 20:30 UTC => 21:30 in Spain, 23:30 in Moscow
        var clock = Clock.fixed(Instant.parse("2026-03-15T20:30:00Z"), ZoneOffset.UTC);
        var service = new DebtorStatusServiceImpl(studentRepository, classSessionRepository, clock);

        when(studentRepository.findAllByDeletedFalse()).thenReturn(List.of(spainStudent, moscowStudent));
        when(classSessionRepository.existsUnpaidOccurredSessionForStudent(
                eq(2L), eq(LocalDate.of(2026, 3, 15)), any(java.time.LocalTime.class)))
                .thenReturn(true);

        service.refreshDebtorStatuses();

        verify(classSessionRepository, never())
                .existsUnpaidOccurredSessionForStudent(eq(1L), any(LocalDate.class), any(java.time.LocalTime.class));
        verify(classSessionRepository)
                .existsUnpaidOccurredSessionForStudent(eq(2L), eq(LocalDate.of(2026, 3, 15)), any(java.time.LocalTime.class));
        verify(studentRepository).saveAll(anyList());
    }

    @Test
    void shouldClearDebtorWhenAllUnpaidSessionsAreResolved() {
        var clock = Clock.fixed(Instant.parse("2026-03-15T22:30:00Z"), ZoneOffset.UTC);
        var service = new DebtorStatusServiceImpl(studentRepository, classSessionRepository, clock);

        spainStudent.setDebtor(true);
        when(studentRepository.findAllByDeletedFalse()).thenReturn(List.of(spainStudent));
        when(classSessionRepository.existsUnpaidOccurredSessionForStudent(
                eq(1L), eq(LocalDate.of(2026, 3, 15)), any(java.time.LocalTime.class)))
                .thenReturn(false);

        service.refreshDebtorStatuses();

        verify(studentRepository).saveAll(anyList());
    }

    @Test
    void shouldNotSaveWhenNoDebtorFlagsChange() {
        var clock = Clock.fixed(Instant.parse("2026-03-15T22:30:00Z"), ZoneOffset.UTC);
        var service = new DebtorStatusServiceImpl(studentRepository, classSessionRepository, clock);

        when(studentRepository.findAllByDeletedFalse()).thenReturn(List.of(spainStudent));
        when(classSessionRepository.existsUnpaidOccurredSessionForStudent(
                eq(1L), eq(LocalDate.of(2026, 3, 15)), any(java.time.LocalTime.class)))
                .thenReturn(false);

        service.refreshDebtorStatuses();

        verify(studentRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldBypassCutoffWhenRequestedForStartupCatchUp() {
        // 20:30 UTC => 21:30 in Spain (before nightly cutoff)
        var clock = Clock.fixed(Instant.parse("2026-03-15T20:30:00Z"), ZoneOffset.UTC);
        var service = new DebtorStatusServiceImpl(studentRepository, classSessionRepository, clock);

        when(studentRepository.findAllByDeletedFalse()).thenReturn(List.of(spainStudent));
        when(classSessionRepository.existsUnpaidOccurredSessionForStudent(
                eq(1L), eq(LocalDate.of(2026, 3, 15)), any(java.time.LocalTime.class)))
                .thenReturn(true);

        service.refreshDebtorStatuses(true);

        verify(classSessionRepository)
                .existsUnpaidOccurredSessionForStudent(eq(1L), eq(LocalDate.of(2026, 3, 15)), any(java.time.LocalTime.class));
        verify(studentRepository).saveAll(anyList());
    }
}



