-- ============================================================
--  Studio Student Management — Add currency column
--  Safe to re-run: uses IF NOT EXISTS.
-- ============================================================

SET search_path TO studio;

ALTER TABLE studio.students
    ADD COLUMN IF NOT EXISTS currency VARCHAR(50);

