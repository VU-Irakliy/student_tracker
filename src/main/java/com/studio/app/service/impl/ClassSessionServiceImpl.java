package com.studio.app.service.impl;

import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.MovePaymentRequest;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.dto.response.CalendarDayResponse;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.entity.ClassSession;
import com.studio.app.enums.ClassStatus;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.PricingType;
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

import java.time.LocalDate;
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
    public ClassSessionResponse createOneOffSession(Long studentId, OneOffSessionRequest request) {
        var student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        var session = ClassSession.builder()
                .student(student)
                .classDate(request.getClassDate())
                .startTime(request.getStartTime())
                .durationMinutes(request.getDurationMinutes())
                .priceCharged(student.getPricePerClass())
                .currency(student.getCurrency())
                .oneOff(true)
                .note(request.getNote())
                .build();

        return enrichWithConvertedPrices(sessionMapper.toResponse(sessionRepository.save(session)));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getSessionsForStudent(Long studentId, LocalDate from, LocalDate to) {
        var sessions = (from != null && to != null)
                ? sessionRepository.findByStudentIdAndDateRange(studentId, from, to)
                : sessionRepository.findByStudentIdAndDeletedFalseOrderByClassDateAscStartTimeAsc(studentId);
        return sessionMapper.toResponseList(sessions).stream()
                .map(this::enrichWithConvertedPrices).toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ClassSessionResponse getSessionById(Long sessionId) {
        return enrichWithConvertedPrices(sessionMapper.toResponse(findActiveSession(sessionId)));
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse updateSession(Long sessionId, UpdateSessionRequest request) {
        var session = findActiveSession(sessionId);

        Optional.ofNullable(request.getClassDate()).ifPresent(session::setClassDate);
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

        return enrichWithConvertedPrices(sessionMapper.toResponse(sessionRepository.save(session)));
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse cancelSession(Long sessionId, CancelSessionRequest request) {
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

        return enrichWithConvertedPrices(sessionMapper.toResponse(sessionRepository.save(session)));
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse markSessionPaid(Long sessionId, PaySessionRequest request) {
        var session = findActiveSession(sessionId);
        markPaidInternal(session, request.getAmountOverride());

        return enrichWithConvertedPrices(sessionMapper.toResponse(sessionRepository.save(session)));
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse setSessionCompletion(Long sessionId, boolean completed) {
        var session = findActiveSession(sessionId);

        if (session.getStatus() == ClassStatus.CANCELLED) {
            throw new BadRequestException("Cancelled session cannot change completion state");
        }

        session.setStatus(completed ? ClassStatus.COMPLETED : ClassStatus.SCHEDULED);

        return enrichWithConvertedPrices(sessionMapper.toResponse(sessionRepository.save(session)));
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse cancelSessionPayment(Long sessionId) {
        var session = findActiveSession(sessionId);
        markUnpaidInternal(session);
        return enrichWithConvertedPrices(sessionMapper.toResponse(sessionRepository.save(session)));
    }

    /** {@inheritDoc} */
    @Override
    public ClassSessionResponse movePayment(Long sessionId, MovePaymentRequest request) {
        var source = findActiveSession(sessionId);

        if (source.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Source session must be in PAID status to move its payment");
        }

        var target = findActiveSession(request.getTargetSessionId());

        if (target.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Target session is already paid");
        }

        // Transfer payment
        target.setPriceCharged(source.getPriceCharged());
        target.setPaymentStatus(PaymentStatus.PAID);

        source.setPaymentStatus(PaymentStatus.UNPAID);

        sessionRepository.save(source);
        return enrichWithConvertedPrices(sessionMapper.toResponse(sessionRepository.save(target)));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getSessionsByPaymentStatus(Long studentId, PaymentStatus paymentStatus) {
        return sessionMapper.toResponseList(
                sessionRepository.findByStudentIdAndPaymentStatusAndDeletedFalse(studentId, paymentStatus))
                .stream().map(this::enrichWithConvertedPrices).toList();
    }


    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<CalendarDayResponse> getCalendar(LocalDate from, LocalDate to) {
        var sessions = sessionRepository.findCalendarSessions(from, to);

        return sessions.stream()
                .collect(Collectors.groupingBy(ClassSession::getClassDate))
                .entrySet()
                .stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> CalendarDayResponse.builder()
                        .date(entry.getKey())
                        .sessions(sessionMapper.toResponseList(entry.getValue()).stream()
                                .map(this::enrichWithConvertedPrices).toList())
                        .build())
                .toList();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /**
     * Populates the {@code convertedPrices} field on a response by delegating
     * to the {@link CurrencyConversionService}.
     */
    private ClassSessionResponse enrichWithConvertedPrices(ClassSessionResponse response) {
        if (response.getPriceCharged() != null && response.getCurrency() != null) {
            response.setConvertedPrices(
                    currencyConversionService.convertToAll(
                            response.getPriceCharged(), response.getCurrency()));
        }
        return response;
    }

    private ClassSession findActiveSession(Long id) {
        return sessionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession", id));
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
}
