-- ============================================================
--  Studio Student Management — Indexes
--  Speeds up the most common query patterns.
--  Every statement uses IF NOT EXISTS so re-runs are safe.
-- ============================================================

SET search_path TO studio;

-- ── students ────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_students_deleted
    ON studio.students (deleted);


-- ── weekly_schedules ────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_weekly_schedules_student
    ON studio.weekly_schedules (student_id)
    WHERE deleted = FALSE;

-- ── class_sessions ──────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_class_sessions_student_date
    ON studio.class_sessions (student_id, class_date)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_class_sessions_student_payment
    ON studio.class_sessions (student_id, payment_status)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_class_sessions_date
    ON studio.class_sessions (class_date)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_class_sessions_paid_feed
    ON studio.class_sessions (payment_date_time DESC)
    WHERE deleted = FALSE AND payment_status = 'PAID';

-- ── package_purchases ───────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_package_purchases_student
    ON studio.package_purchases (student_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_package_purchases_remaining
    ON studio.package_purchases (student_id, classes_remaining)
    WHERE deleted = FALSE AND classes_remaining > 0;

CREATE INDEX IF NOT EXISTS idx_package_purchases_payment_date
    ON studio.package_purchases (payment_date DESC)
    WHERE deleted = FALSE;

-- ── payers ──────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_payers_student
    ON studio.payers (student_id)
    WHERE deleted = FALSE;

