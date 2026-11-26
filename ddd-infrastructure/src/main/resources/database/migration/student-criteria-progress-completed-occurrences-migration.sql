-- Migration: Add completed_occurrences to student_criteria_progress
ALTER TABLE student_criteria_progress
    ADD COLUMN IF NOT EXISTS completed_occurrences INTEGER NOT NULL DEFAULT 0;

-- Backfill existing completed leaf criteria progress rows to match required_occurrences
UPDATE student_criteria_progress scp
SET completed_occurrences = COALESCE(c.required_occurrences, 1)
FROM criterias c
WHERE scp.criteria_id = c.id
  AND scp.is_completed = TRUE
  AND scp.completed_occurrences = 0
  AND NOT EXISTS (
      SELECT 1 FROM criterias child
      WHERE child.parent_id = c.id AND child.is_active = TRUE
  );
