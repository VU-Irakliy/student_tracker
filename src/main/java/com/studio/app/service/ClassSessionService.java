package com.studio.app.service;

import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.MovePaymentRequest;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.dto.response.CalendarDayResponse;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.PaymentStatus;
import com.studio.app.enums.StudioTimezone;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing individual class sessions.
 */
public interface ClassSessionService {

    /**
     * Creates a one-off class session (extra or moved class).
     *
     * @param studentId the student ID
     * @param request   session details
     * @return the created session
     */
    default ClassSessionResponse createOneOffSession(Long studentId, OneOffSessionRequest request) {
        return createOneOffSession(studentId, request, StudioTimezone.SPAIN);
    }

    ClassSessionResponse createOneOffSession(Long studentId, OneOffSessionRequest request, StudioTimezone viewerTimezone);

    /**
     * Returns all active sessions for a student, optionally filtered by date range.
     *
     * @param studentId the student ID
     * @param from      optional start date (inclusive)
     * @param to        optional end date (inclusive)
     * @return list of sessions ordered by date/time
     */
    default List<ClassSessionResponse> getSessionsForStudent(Long studentId, LocalDate from, LocalDate to) {
        return getSessionsForStudent(studentId, from, to, StudioTimezone.SPAIN);
    }

    List<ClassSessionResponse> getSessionsForStudent(Long studentId, LocalDate from, LocalDate to, StudioTimezone viewerTimezone);

    /**
     * Returns a single session by ID.
     *
     * @param sessionId the session ID
     * @return the session
     */
    default ClassSessionResponse getSessionById(Long sessionId) {
        return getSessionById(sessionId, StudioTimezone.SPAIN);
    }

    ClassSessionResponse getSessionById(Long sessionId, StudioTimezone viewerTimezone);

    /**
     * Partially updates a session's date/time/duration/status/payment/note.
     *
     * @param sessionId the session ID
     * @param request   fields to update
     * @return the updated session
     */
    default ClassSessionResponse updateSession(Long sessionId, UpdateSessionRequest request) {
        return updateSession(sessionId, request, StudioTimezone.SPAIN);
    }

    ClassSessionResponse updateSession(Long sessionId, UpdateSessionRequest request, StudioTimezone viewerTimezone);

    /**
     * Cancels a class session with configurable payment handling.
     *
     * @param sessionId the session ID
     * @param request   cancellation options
     * @return the updated session
     */
    default ClassSessionResponse cancelSession(Long sessionId, CancelSessionRequest request) {
        return cancelSession(sessionId, request, StudioTimezone.SPAIN);
    }

    ClassSessionResponse cancelSession(Long sessionId, CancelSessionRequest request, StudioTimezone viewerTimezone);

    /**
     * Marks a session as paid.
     * <ul>
     *   <li>{@code PER_CLASS} students: marks as {@code PAID}, with optional amount override.</li>
     *   <li>{@code PACKAGE} students: auto-deducts from the oldest active package (FIFO).</li>
     * </ul>
     *
     * @param sessionId the session ID
     * @param request   optional amount override (PER_CLASS only)
     * @return the updated session
     */
    default ClassSessionResponse markSessionPaid(Long sessionId, PaySessionRequest request) {
        return markSessionPaid(sessionId, request, StudioTimezone.SPAIN);
    }

    ClassSessionResponse markSessionPaid(Long sessionId, PaySessionRequest request, StudioTimezone viewerTimezone);

    /**
     * Sets session completion state.
     *
     * @param sessionId  the session ID
     * @param completed  true -> COMPLETED, false -> SCHEDULED
     * @return the updated session
     */
    default ClassSessionResponse setSessionCompletion(Long sessionId, boolean completed) {
        return setSessionCompletion(sessionId, completed, StudioTimezone.SPAIN);
    }

    ClassSessionResponse setSessionCompletion(Long sessionId, boolean completed, StudioTimezone viewerTimezone);

    /**
     * Cancels the payment for a session, reverting it to {@code UNPAID}.
     * For package sessions, the class is returned to the package.
     *
     * @param sessionId the session ID
     * @return the updated session
     */
    default ClassSessionResponse cancelSessionPayment(Long sessionId) {
        return cancelSessionPayment(sessionId, StudioTimezone.SPAIN);
    }

    ClassSessionResponse cancelSessionPayment(Long sessionId, StudioTimezone viewerTimezone);

    /**
     * Moves a payment from a cancelled or unpaid session to another session.
     *
     * @param sessionId the source session ID
     * @param request   the target session details
     * @return the updated target session
     */
    default ClassSessionResponse movePayment(Long sessionId, MovePaymentRequest request) {
        return movePayment(sessionId, request, StudioTimezone.SPAIN);
    }

    ClassSessionResponse movePayment(Long sessionId, MovePaymentRequest request, StudioTimezone viewerTimezone);

    /**
     * Returns all sessions for a student filtered by payment status.
     *
     * @param studentId     the student ID
     * @param paymentStatus the desired payment status
     * @return matching sessions
     */
    default List<ClassSessionResponse> getSessionsByPaymentStatus(Long studentId, PaymentStatus paymentStatus) {
        return getSessionsByPaymentStatus(studentId, paymentStatus, StudioTimezone.SPAIN);
    }

    List<ClassSessionResponse> getSessionsByPaymentStatus(Long studentId, PaymentStatus paymentStatus, StudioTimezone viewerTimezone);


    /**
     * Returns a calendar view grouped by day for the given date range.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return list of calendar days with their sessions
     */
    default List<CalendarDayResponse> getCalendar(LocalDate from, LocalDate to) {
        return getCalendar(from, to, StudioTimezone.SPAIN);
    }

    List<CalendarDayResponse> getCalendar(LocalDate from, LocalDate to, StudioTimezone viewerTimezone);
}
