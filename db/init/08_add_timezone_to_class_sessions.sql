-- ============================================================
--  Add timezone snapshot to class sessions
-- ============================================================

SET search_path TO studio;

ALTER TABLE studio.class_sessions
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);

UPDATE studio.class_sessions cs
SET timezone = s.timezone
FROM studio.students s
WHERE cs.student_id = s.id
  AND cs.timezone IS NULL;

ALTER TABLE studio.class_sessions
    ALTER COLUMN timezone SET NOT NULL;

