-- Migration to add school_id to events table
ALTER TABLE events ADD COLUMN IF NOT EXISTS school_id BIGINT;

ALTER TABLE events
    DROP CONSTRAINT IF EXISTS fk_events_schools,
    ADD CONSTRAINT fk_events_schools
        FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_events_school_id ON events (school_id);

