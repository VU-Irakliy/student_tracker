-- ============================================================
--  Add student class type for exam/casual categorisation
-- ============================================================

SET search_path TO studio;

ALTER TABLE studio.students
    ADD COLUMN IF NOT EXISTS class_type VARCHAR(50) NOT NULL DEFAULT 'CASUAL';

