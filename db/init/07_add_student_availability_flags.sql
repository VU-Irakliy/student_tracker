-- ============================================================
--  Add student lifecycle / availability fields
-- ============================================================

SET search_path TO studio;

ALTER TABLE IF EXISTS studio.students
    ADD COLUMN IF NOT EXISTS start_date DATE,
    ADD COLUMN IF NOT EXISTS holiday_mode BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS holiday_from DATE,
    ADD COLUMN IF NOT EXISTS holiday_to DATE,
    ADD COLUMN IF NOT EXISTS stopped_attending BOOLEAN NOT NULL DEFAULT FALSE;

