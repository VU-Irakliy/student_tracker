package com.studio.app.service.impl;

import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.dto.response.CalendarDayResponse;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.entity.ClassSession;
import com.studio.app.entity.Student;
import com.studio.app.enums.ClassStatus;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import com.studio.app.exception.BadRequestException;
import com.studio.app.exception.ResourceNotFoundException;
import com.studio.app.mapper.ClassSessionMapper;
import com.studio.app.repository.ClassSessionRepository;
import com.studio.app.repository.PackagePurchaseRepository;
import com.studio.app.repository.StudentRepository;
import com.studio.app.service.ClassSessionService;
import com.studio.app.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ClassSessionService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClassSessionServiceImpl implements ClassSessionService {

    private final ClassSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final PackagePurchaseRepository packageRepository;
    private final ClassSessionMapper sessionMapper;
    private final CurrencyConversionService currencyConversionService;

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse createOneOffSession(Long studentId, OneOffSessionRequest request,
                                                    StudioTimezone viewerTimezone) {
        var student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        ensureStudentCanHaveSessionOnDate(student, request.getClassDate());

        var session = ClassSession.builder()
                .student(student)
                .classDate(request.getClassDate())
                .startTime(request.getStartTime())
                .timezone(student.getTimezone())
                .durationMinutes(request.getDurationMinutes())
                .priceCharged(student.getPricePerClass())
                .currency(student.getCurrency())
                .oneOff(true)
                .note(request.getNote())
                .build();

        return toResponse(sessionRepository.save(session), viewerTimezone);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getSessionsForStudent(Long studentId, LocalDate from, LocalDate to,
                                                            StudioTimezone viewerTimezone) {
        findActiveStudent(studentId);

        var sessions = (from != null && to != null)
                ? sessionRepository.findByStudentIdAndDateRange(studentId, from, to)
                : (from != null)
                    ? sessionRepository.findByStudentIdAndClassDateGreaterThanEqualAndDeletedFalseOrderByClassDateAscStartTimeAsc(studentId, from)
                    : (to != null)
                        ? sessionRepository.findByStudentIdAndClassDateLessThanEqualAndDeletedFalseOrderByClassDateAscStartTimeAsc(studentId, to)
                        : sessionRepository.findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(studentId);
        return sessions.stream().map(session -> toResponse(session, viewerTimezone)).toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ClassSessionResponse getSessionById(Long sessionId, StudioTimezone viewerTimezone) {
        return toResponse(findActiveSession(sessionId), viewerTimezone);
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse updateSession(Long sessionId, UpdateSessionRequest request,
                                              StudioTimezone viewerTimezone) {
        var session = findActiveSession(sessionId);
        session.setTimezone(session.getStudent().getTimezone());

        Optional.ofNullable(request.getClassDate()).ifPresent(classDate -> {
            ensureStudentCanHaveSessionOnDate(session.getStudent(), classDate);
            session.setClassDate(classDate);
        });
        Optional.ofNullable(request.getStartTime()).ifPresent(session::setStartTime);
        Optional.ofNullable(request.getDurationMinutes()).ifPresent(session::setDurationMinutes);
        Optional.ofNullable(request.getStatus()).ifPresent(session::setStatus);
        Optional.ofNullable(request.getNote()).ifPresent(session::setNote);

        if (request.getPaid() != null) {
            if (request.getPaid()) {
                markPaidInternal(session, request.getAmountOverride());
            } else {
                markUnpaidInternal(session);
            }
        }

        return toResponse(sessionRepository.save(session), viewerTimezone);
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse cancelSession(Long sessionId, CancelSessionRequest request,
                                              StudioTimezone viewerTimezone) {
        var session = findActiveSession(sessionId);

        if (session.getStatus() == ClassStatus.CANCELLED) {
            throw new BadRequestException("Session is already cancelled");
        }

        session.setStatus(ClassStatus.CANCELLED);
        Optional.ofNullable(request.getNote()).ifPresent(session::setNote);

        var keepPaid = Boolean.TRUE.equals(request.getKeepAsPaid());

        if (!keepPaid) {
            // For package sessions: return class slot to the package
            if (session.getPaymentStatus() == PaymentStatus.PACKAGE && session.getPackagePurchase() != null) {
                var pkg = session.getPackagePurchase();
                pkg.setClassesRemaining(pkg.getClassesRemaining() + 1);
                packageRepository.save(pkg);
                session.setPackagePurchase(null);
            }
            // For per-class paid sessions: revert to unpaid so payment can be moved
            session.setPaymentStatus(PaymentStatus.UNPAID);
        }

        return toResponse(sessionRepository.save(session), viewerTimezone);
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse markSessionPaid(Long sessionId, PaySessionRequest request,
                                                StudioTimezone viewerTimezone) {
        var session = findActiveSession(sessionId);
        markPaidInternal(session, request.getAmountOverride());

        return toResponse(sessionRepository.save(session), viewerTimezone);
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse setSessionCompletion(Long sessionId, boolean completed,
                                                     StudioTimezone viewerTimezone) {
        var session = findActiveSession(sessionId);

        if (session.getStatus() == ClassStatus.CANCELLED) {
            throw new BadRequestException("Cancelled session cannot change completion state");
        }

        session.setStatus(completed ? ClassStatus.COMPLETED : ClassStatus.SCHEDULED);

        return toResponse(sessionRepository.save(session), viewerTimezone);
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse cancelSessionPayment(Long sessionId, StudioTimezone viewerTimezone) {
        var session = findActiveSession(sessionId);
        markUnpaidInternal(session);
        return toResponse(sessionRepository.save(session), viewerTimezone);
    }



    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getSessionsByPaymentStatus(Long studentId, PaymentStatus paymentStatus,
                                                                 StudioTimezone viewerTimezone) {
        findActiveStudent(studentId);
        return sessionRepository.findByStudentIdAndPaymentStatusAndDeletedFalse(studentId, paymentStatus)
                .stream().map(session -> toResponse(session, viewerTimezone)).toList();
    }


    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<CalendarDayResponse> getCalendar(LocalDate from, LocalDate to, StudioTimezone viewerTimezone) {
        var sessions = sessionRepository.findCalendarSessions(from, to);

        var converted = sessions.stream()
                .map(session -> toResponse(session, viewerTimezone))
                .toList();

        return converted.stream()
                .collect(Collectors.groupingBy(ClassSessionResponse::getClassDate))
                .entrySet()
                .stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> CalendarDayResponse.builder()
                        .date(entry.getKey())
                        .totalHours(toHours(entry.getValue().stream()
                                .mapToInt(ClassSessionResponse::getDurationMinutes)
                                .sum()))
                        .completedHours(toHours(entry.getValue().stream()
                                .filter(session -> session.getStatus() == ClassStatus.COMPLETED)
                                .mapToInt(ClassSessionResponse::getDurationMinutes)
                                .sum()))
                        .sessions(entry.getValue())
                        .build())
                .toList();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /**
     * Populates the {@code convertedPrices} field on a response by delegating
     * to the {@link CurrencyConversionService}.
     */
    private ClassSessionResponse toResponse(ClassSession session, StudioTimezone viewerTimezone) {
        var response = sessionMapper.toResponse(session);
        // Keep timezone explicit in case generated mapper code is stale in local IDE caches.
        response.setTimezone(session.getTimezone());

        StudioTimezone sourceTimezone = Optional.ofNullable(session.getTimezone()).orElse(StudioTimezone.SPAIN);
        StudioTimezone effectiveViewerTimezone = Optional.ofNullable(viewerTimezone).orElse(StudioTimezone.SPAIN);

        response.setOriginalClassDate(session.getClassDate());
        response.setOriginalStartTime(session.getStartTime());
        response.setOriginalTimezone(sourceTimezone);
        response.setViewerTimezone(effectiveViewerTimezone);

        if (session.getClassDate() != null && session.getStartTime() != null) {
            var sourceDateTime = LocalDateTime.of(session.getClassDate(), session.getStartTime())
                    .atZone(sourceTimezone.toZoneId());
            var viewerDateTime = sourceDateTime.withZoneSameInstant(effectiveViewerTimezone.toZoneId());
            response.setClassDate(viewerDateTime.toLocalDate());
            response.setStartTime(viewerDateTime.toLocalTime());
            response.setTimezone(effectiveViewerTimezone);
        }

        return enrichWithConvertedPrices(response);
    }

    private ClassSessionResponse enrichWithConvertedPrices(ClassSessionResponse response) {
        if (response.getPriceCharged() != null && response.getCurrency() != null) {
            response.setConvertedPrices(
                    currencyConversionService.convertToAll(
                            response.getPriceCharged(), response.getCurrency()));
        }
        return response;
    }

    private BigDecimal toHours(int minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private ClassSession findActiveSession(Long id) {
        return sessionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession", id));
    }

    private Student findActiveStudent(Long studentId) {
        return studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
    }

    private void markPaidInternal(ClassSession session, java.math.BigDecimal amountOverride) {
        var student = session.getStudent();

        if (session.getPaymentStatus() == PaymentStatus.PAID
                || session.getPaymentStatus() == PaymentStatus.PACKAGE) {
            throw new BadRequestException("Session is already paid");
        }

        if (student.getPricingType() == PricingType.PACKAGE) {
            // Auto-deduct from oldest active package (FIFO)
            var pkg = packageRepository.findActivePackagesByStudent(student.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "No active package with remaining classes found"));

            pkg.setClassesRemaining(pkg.getClassesRemaining() - 1);
            packageRepository.save(pkg);

            session.setPackagePurchase(pkg);
            session.setPaymentStatus(PaymentStatus.PACKAGE);
            return;
        }

        // PER_CLASS: use override amount if provided; otherwise keep captured price
        Optional.ofNullable(amountOverride).ifPresent(session::setPriceCharged);
        session.setPaymentStatus(PaymentStatus.PAID);
    }

    private void markUnpaidInternal(ClassSession session) {
        if (session.getPaymentStatus() == PaymentStatus.UNPAID) {
            throw new BadRequestException("Session has no payment to cancel");
        }

        // Return class to package if applicable
        if (session.getPaymentStatus() == PaymentStatus.PACKAGE && session.getPackagePurchase() != null) {
            var pkg = session.getPackagePurchase();
            pkg.setClassesRemaining(pkg.getClassesRemaining() + 1);
            packageRepository.save(pkg);
            session.setPackagePurchase(null);
        }

        session.setPaymentStatus(PaymentStatus.UNPAID);
    }

    private void ensureStudentCanHaveSessionOnDate(Student student, LocalDate classDate) {
        if (student.isStoppedAttending()) {
            throw new BadRequestException("Student is marked as stopped attending");
        }

        if (student.getStartDate() != null && classDate.isBefore(student.getStartDate())) {
            throw new BadRequestException("Class date cannot be before student's startDate");
        }

        if (student.isHolidayMode() && student.getHolidayFrom() != null && !classDate.isBefore(student.getHolidayFrom())) {
            throw new BadRequestException("Student is currently on holiday from " + student.getHolidayFrom());
        }
    }
}
