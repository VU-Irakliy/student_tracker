SET search_path TO studio;

ALTER TABLE studio.class_sessions
    ADD COLUMN IF NOT EXISTS payment_date_time TIMESTAMP;

-- Backfill historical paid rows so existing data remains visible in payment history.
UPDATE studio.class_sessions
SET payment_date_time = COALESCE(payment_date_time, created_at)
WHERE deleted = FALSE
  AND payment_status IN ('PAID', 'PACKAGE')
  AND payment_date_time IS NULL;

CREATE INDEX IF NOT EXISTS idx_class_sessions_paid_feed
    ON studio.class_sessions (payment_date_time DESC)
    WHERE deleted = FALSE AND payment_status = 'PAID';

CREATE INDEX IF NOT EXISTS idx_package_purchases_payment_date
    ON studio.package_purchases (payment_date DESC)
    WHERE deleted = FALSE;


