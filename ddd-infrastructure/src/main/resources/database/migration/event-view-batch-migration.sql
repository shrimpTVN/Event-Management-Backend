BEGIN;

CREATE TABLE IF NOT EXISTS student_event_recommendation (
    student_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id bigint NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    read_penalty integer NOT NULL DEFAULT 0 CHECK (read_penalty BETWEEN 0 AND 20),
    last_read_at timestamptz,
    PRIMARY KEY (student_id, event_id)
);

CREATE TABLE IF NOT EXISTS recommendation_view_receipt (
    student_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id bigint NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    batch_id uuid NOT NULL,
    PRIMARY KEY (student_id, event_id, batch_id)
);

CREATE INDEX IF NOT EXISTS student_event_recommendation_event_idx
    ON student_event_recommendation (event_id);
CREATE INDEX IF NOT EXISTS recommendation_view_receipt_event_idx
    ON recommendation_view_receipt (event_id);

COMMIT;
