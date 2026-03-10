package com.studio.app.enums;

/**
 * Payment state of a class session.
 *
 * <ul>
 *   <li>{@code UNPAID}   – no payment has been recorded.</li>
 *   <li>{@code PAID}     – payment confirmed for this session.</li>
 *   <li>{@code PACKAGE}  – covered by an active package (no individual payment).</li>
 *   <li>{@code REFUNDED} – payment was reversed / cancelled.</li>
 * </ul>
 */
public enum PaymentStatus {
    UNPAID,
    PAID,
    PACKAGE,
    REFUNDED
}
