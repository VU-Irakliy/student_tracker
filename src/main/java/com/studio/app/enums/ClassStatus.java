package com.studio.app.enums;

/**
 * Lifecycle state of a single scheduled class instance.
 *
 * <ul>
 *   <li>{@code SCHEDULED}  – class is planned and upcoming.</li>
 *   <li>{@code COMPLETED}  – class took place.</li>
 *   <li>{@code CANCELLED}  – class was cancelled (payment handling applies separately).</li>
 *   <li>{@code MOVED}      – class was rescheduled as a one-time event.</li>
 * </ul>
 */
public enum ClassStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    MOVED
}
