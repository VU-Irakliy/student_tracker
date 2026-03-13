-- ============================================================
--  Studio Student Management — Add currency to sessions & packages
--  Safe to re-run: uses IF NOT EXISTS / DO blocks.
-- ============================================================

SET search_path TO studio;

-- Add currency column to class_sessions
ALTER TABLE studio.class_sessions
    ADD COLUMN IF NOT EXISTS currency VARCHAR(50);

-- Add currency column to package_purchases
ALTER TABLE studio.package_purchases
    ADD COLUMN IF NOT EXISTS currency VARCHAR(50);

-- Backfill existing rows with the student's current currency
DO $$
BEGIN
    UPDATE studio.class_sessions cs
    SET currency = s.currency
    FROM studio.students s
    WHERE cs.student_id = s.id
      AND cs.currency IS NULL
      AND s.currency IS NOT NULL;

    UPDATE studio.package_purchases pp
    SET currency = s.currency
    FROM studio.students s
    WHERE pp.student_id = s.id
      AND pp.currency IS NULL
      AND s.currency IS NOT NULL;
END $$;

-- Index for earnings queries: PAID sessions by date
CREATE INDEX IF NOT EXISTS idx_class_sessions_paid_date
    ON studio.class_sessions (class_date, payment_status)
    WHERE deleted = FALSE AND payment_status = 'PAID';

