ALTER TABLE criterias ADD COLUMN IF NOT EXISTS required_occurrences INTEGER NOT NULL DEFAULT 1;
UPDATE criterias SET required_occurrences = 2 WHERE name ILIKE '%02 lần hiến máu%';
-- Existing event transactions and attendance records are retained.
