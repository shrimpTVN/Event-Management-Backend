-- Apply this migration to an existing PostgreSQL database before using the
-- student criteria progress endpoints. The project currently keeps schema.sql
-- as the canonical full-schema definition and uses ddl-auto=update locally.

BEGIN;

CREATE TABLE IF NOT EXISTS academic_years
(
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT pk_academic_years PRIMARY KEY (id),
    CONSTRAINT ck_academic_year_dates CHECK (start_date <= end_date)
);

ALTER TABLE criterias
    ADD COLUMN IF NOT EXISTS required_children INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_criterias_required_children') THEN
        ALTER TABLE criterias
            ADD CONSTRAINT ck_criterias_required_children
                CHECK (required_children IS NULL OR required_children > 0);
    END IF;
END $$;

UPDATE criterias SET required_children = 5 WHERE id = 1;
UPDATE criterias SET required_children = 3 WHERE id = 2;
UPDATE criterias SET required_children = 1 WHERE id = 5;
UPDATE criterias SET required_children = 2 WHERE id = 11;
UPDATE criterias SET required_children = 1 WHERE id = 14;
UPDATE criterias SET required_children = 1 WHERE id = 17;
UPDATE criterias SET required_children = 2 WHERE id = 21;
UPDATE criterias SET required_children = 1 WHERE id IN (22, 28, 35);

ALTER TABLE semesters
    ADD COLUMN IF NOT EXISTS academic_year_id BIGINT;

INSERT INTO academic_years (code, start_date, end_date)
SELECT academic_year, MIN(start_date), MAX(end_date)
FROM semesters
GROUP BY academic_year
ON CONFLICT (code) DO NOTHING;

UPDATE semesters AS semesters_table
SET academic_year_id = academic_years_table.id
FROM academic_years AS academic_years_table
WHERE semesters_table.academic_year = academic_years_table.code
  AND semesters_table.academic_year_id IS NULL;

CREATE TABLE IF NOT EXISTS student_criteria_progress
(
    id                BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id           BIGINT       NOT NULL,
    criteria_id       BIGINT       NOT NULL,
    academic_year_id  BIGINT       NOT NULL,
    event_id   BIGINT,
    is_completed      BOOLEAN      NOT NULL DEFAULT FALSE,
    completion_source VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    completed_at      TIMESTAMPTZ,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_student_criteria_progress PRIMARY KEY (id),
    CONSTRAINT uq_student_criteria_progress UNIQUE (user_id, criteria_id, academic_year_id)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_semesters_academic_year') THEN
        ALTER TABLE semesters
            ADD CONSTRAINT fk_semesters_academic_year
                FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_progress_users') THEN
        ALTER TABLE student_criteria_progress
            ADD CONSTRAINT fk_progress_users
                FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_progress_criteria') THEN
        ALTER TABLE student_criteria_progress
            ADD CONSTRAINT fk_progress_criteria
                FOREIGN KEY (criteria_id) REFERENCES criterias (id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_progress_academic_year') THEN
        ALTER TABLE student_criteria_progress
            ADD CONSTRAINT fk_progress_academic_year
                FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_progress_event') THEN
        ALTER TABLE student_criteria_progress
            ADD CONSTRAINT fk_progress_event
                FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_progress_user_year
    ON student_criteria_progress (user_id, academic_year_id);

CREATE INDEX IF NOT EXISTS idx_progress_criteria
    ON student_criteria_progress (criteria_id);

COMMIT;
