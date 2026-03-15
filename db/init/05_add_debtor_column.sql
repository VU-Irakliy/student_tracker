-- ============================================================
--  Add debtor flag to students
-- ============================================================

SET search_path TO studio;

ALTER TABLE studio.students
    ADD COLUMN IF NOT EXISTS debtor BOOLEAN NOT NULL DEFAULT FALSE;

