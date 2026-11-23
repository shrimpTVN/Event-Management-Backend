-- Apply event-view-batch-migration.sql first.
BEGIN;

CREATE TABLE IF NOT EXISTS event_recommendation_summary (
    event_id bigint PRIMARY KEY REFERENCES events(id) ON DELETE CASCADE,
    scoring_version bigint NOT NULL DEFAULT 1,
    features jsonb NOT NULL
);

CREATE TABLE IF NOT EXISTS student_recommendation_summary (
    student_id bigint PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    snapshot jsonb
);

ALTER TABLE student_event_recommendation
    ADD COLUMN IF NOT EXISTS score_day date,
    ADD COLUMN IF NOT EXISTS evaluated_event_version bigint,
    ADD COLUMN IF NOT EXISTS score jsonb;

CREATE INDEX IF NOT EXISTS recommendation_upcoming_events_idx ON events(date_happen)
    WHERE is_active AND status = 'PUBLISHED';
CREATE INDEX IF NOT EXISTS recommendation_active_registrations_idx ON registrations(event_id)
    WHERE status <> 'CANCELLED';

CREATE OR REPLACE FUNCTION sync_event_recommendation_summary(target bigint) RETURNS void
LANGUAGE sql AS $$
    INSERT INTO event_recommendation_summary AS summary (event_id, features)
    SELECT e.id, jsonb_build_object(
        'title', e.name, 'description', e.description,
        'semesterId', e.semester_id, 'yearId', s.academic_year_id,
        'criterionId', e.criteria_id, 'criterionName', c.name,
        'fanpageId', e.fanpage_id, 'typeId', e.event_type_id,
        'points', COALESCE((
            SELECT jsonb_object_agg(point_category_id, offered) FROM (
                SELECT point_category_id, SUM(point) AS offered
                FROM event_points WHERE event_id = e.id AND is_active GROUP BY point_category_id
            ) grouped
        ), '{}'::jsonb)
    )
    FROM events e
    LEFT JOIN semesters s ON s.id = e.semester_id
    LEFT JOIN criterias c ON c.id = e.criteria_id
    WHERE e.id = target
    ON CONFLICT (event_id) DO UPDATE SET
        features = EXCLUDED.features,
        scoring_version = summary.scoring_version + 1
    WHERE summary.features IS DISTINCT FROM EXCLUDED.features;
$$;

CREATE OR REPLACE FUNCTION refresh_event_recommendation_summary() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'events' THEN
        PERFORM sync_event_recommendation_summary(NEW.id);
    ELSIF TG_TABLE_NAME = 'semesters' THEN
        PERFORM sync_event_recommendation_summary(id) FROM events WHERE semester_id = NEW.id;
    ELSIF TG_TABLE_NAME = 'criterias' THEN
        PERFORM sync_event_recommendation_summary(id) FROM events WHERE criteria_id = NEW.id;
    ELSE
        IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND OLD.event_id <> NEW.event_id) THEN
            PERFORM sync_event_recommendation_summary(OLD.event_id);
        END IF;
        IF TG_OP <> 'DELETE' THEN
            PERFORM sync_event_recommendation_summary(NEW.event_id);
        END IF;
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE TRIGGER recommendation_event_features
    AFTER INSERT OR UPDATE OF name, description, semester_id, criteria_id, fanpage_id, event_type_id ON events
    FOR EACH ROW EXECUTE FUNCTION refresh_event_recommendation_summary();
CREATE OR REPLACE TRIGGER recommendation_event_point_features
    AFTER INSERT OR UPDATE OR DELETE ON event_points
    FOR EACH ROW EXECUTE FUNCTION refresh_event_recommendation_summary();
CREATE OR REPLACE TRIGGER recommendation_semester_features
    AFTER UPDATE OF academic_year_id ON semesters
    FOR EACH ROW EXECUTE FUNCTION refresh_event_recommendation_summary();
CREATE OR REPLACE TRIGGER recommendation_criterion_features
    AFTER UPDATE OF name ON criterias
    FOR EACH ROW EXECUTE FUNCTION refresh_event_recommendation_summary();

SELECT sync_event_recommendation_summary(id) FROM events;
COMMIT;
