package com.studio.app.service;

import com.studio.app.dto.request.CancelSessionRequest;
import com.studio.app.dto.request.MovePaymentRequest;
import com.studio.app.dto.request.OneOffSessionRequest;
import com.studio.app.dto.request.PaySessionRequest;
import com.studio.app.dto.request.UpdateSessionRequest;
import com.studio.app.dto.response.CalendarDayResponse;
import com.studio.app.dto.response.ClassSessionResponse;
import com.studio.app.enums.PaymentStatus;

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
    ClassSessionResponse createOneOffSession(Long studentId, OneOffSessionRequest request);

    /**
     * Returns all active sessions for a student, optionally filtered by date range.
     *
     * @param studentId the student ID
     * @param from      optional start date (inclusive)
     * @param to        optional end date (inclusive)
     * @return list of sessions ordered by date/time
     */
    List<ClassSessionResponse> getSessionsForStudent(Long studentId, LocalDate from, LocalDate to);

    /**
     * Returns a single session by ID.
     *
     * @param sessionId the session ID
     * @return the session
     */
    ClassSessionResponse getSessionById(Long sessionId);

    /**
     * Partially updates a session's date/time/duration/status/payment/note.
     *
     * @param sessionId the session ID
     * @param request   fields to update
     * @return the updated session
     */
    ClassSessionResponse updateSession(Long sessionId, UpdateSessionRequest request);

    /**
     * Cancels a class session with configurable payment handling.
     *
     * @param sessionId the session ID
     * @param request   cancellation options
     * @return the updated session
     */
    ClassSessionResponse cancelSession(Long sessionId, CancelSessionRequest request);

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
    ClassSessionResponse markSessionPaid(Long sessionId, PaySessionRequest request);

    /**
     * Sets session completion state.
     *
     * @param sessionId  the session ID
     * @param completed  true -> COMPLETED, false -> SCHEDULED
     * @return the updated session
     */
    ClassSessionResponse setSessionCompletion(Long sessionId, boolean completed);

    /**
     * Cancels the payment for a session, reverting it to {@code UNPAID}.
     * For package sessions, the class is returned to the package.
     *
     * @param sessionId the session ID
     * @return the updated session
     */
    ClassSessionResponse cancelSessionPayment(Long sessionId);

    /**
     * Moves a payment from a cancelled or unpaid session to another session.
     *
     * @param sessionId the source session ID
     * @param request   the target session details
     * @return the updated target session
     */
    ClassSessionResponse movePayment(Long sessionId, MovePaymentRequest request);

    /**
     * Returns all sessions for a student filtered by payment status.
     *
     * @param studentId     the student ID
     * @param paymentStatus the desired payment status
     * @return matching sessions
     */
    List<ClassSessionResponse> getSessionsByPaymentStatus(Long studentId, PaymentStatus paymentStatus);


    /**
     * Returns a calendar view grouped by day for the given date range.
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     * @return list of calendar days with their sessions
     */
    List<CalendarDayResponse> getCalendar(LocalDate from, LocalDate to);
}
