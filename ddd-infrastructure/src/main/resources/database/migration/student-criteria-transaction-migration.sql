-- ==============================================================================
-- Migration Script: Student Criteria Transactions & Progress Recalculation
-- Purpose:
--   1. Create `student_criteria_transactions` table to store event & manual completion records.
--   2. Migrate existing transaction data from `student_criteria_progress` into `student_criteria_transactions`.
--   3. Restructure `student_criteria_progress` into an aggregated state snapshot per criteria.
--   4. Recalculate criteria progress bottom-up (leaf -> root) for all students.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. CREATE student_criteria_transactions table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_criteria_transactions
(
    id                BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id           BIGINT      NOT NULL,
    criteria_id       BIGINT      NOT NULL,
    academic_year_id  BIGINT      NOT NULL,
    event_id          BIGINT,
    completion_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    note              TEXT,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by        BIGINT,
    CONSTRAINT pk_student_criteria_transaction PRIMARY KEY (id),
    CONSTRAINT uq_student_criteria_transaction
        UNIQUE (user_id, criteria_id, academic_year_id, event_id)
);

-- Add foreign key constraints safely if not already present
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transaction_event') THEN
            ALTER TABLE student_criteria_transactions
                ADD CONSTRAINT fk_transaction_event
                    FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE SET NULL;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transaction_users') THEN
            ALTER TABLE student_criteria_transactions
                ADD CONSTRAINT fk_transaction_users
                    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transaction_criteria') THEN
            ALTER TABLE student_criteria_transactions
                ADD CONSTRAINT fk_transaction_criteria
                    FOREIGN KEY (criteria_id) REFERENCES criterias (id) ON DELETE CASCADE;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transaction_academic_year') THEN
            ALTER TABLE student_criteria_transactions
                ADD CONSTRAINT fk_transaction_academic_year
                    FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT;
        END IF;
    END
$$;

-- Trigger to maintain updated_at timestamp
DROP TRIGGER IF EXISTS trg_student_criteria_transaction_updated_at ON student_criteria_transactions;
CREATE TRIGGER trg_student_criteria_transaction_updated_at
    BEFORE UPDATE
    ON student_criteria_transactions
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();

-- ------------------------------------------------------------------------------
-- 2. MIGRATE EXISTING DATA into student_criteria_transactions
-- ------------------------------------------------------------------------------
DO
$$
    BEGIN
        -- Check if student_criteria_progress still has the legacy columns before migrating
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'student_criteria_progress'
                     AND column_name = 'completion_source') THEN
            INSERT INTO student_criteria_transactions (user_id,
                                                       criteria_id,
                                                       academic_year_id,
                                                       event_id,
                                                       completion_source,
                                                       note,
                                                       is_active,
                                                       created_at,
                                                       created_by,
                                                       updated_at,
                                                       updated_by)
            SELECT scp.user_id,
                   scp.criteria_id,
                   scp.academic_year_id,
                   scp.event_id,
                   COALESCE(scp.completion_source, 'MANUAL'),
                   scp.note,
                   COALESCE(scp.is_active, TRUE),
                   COALESCE(scp.created_at, NOW()),
                   scp.created_by,
                   COALESCE(scp.updated_at, NOW()),
                   scp.updated_by
            FROM student_criteria_progress scp
            WHERE NOT EXISTS (SELECT 1
                              FROM student_criteria_transactions sct
                              WHERE sct.user_id = scp.user_id
                                AND sct.criteria_id = scp.criteria_id
                                AND sct.academic_year_id = scp.academic_year_id
                                AND (
                                  (sct.event_id IS NULL AND scp.event_id IS NULL)
                                      OR (sct.event_id = scp.event_id)
                                  )
                                AND sct.created_at = scp.created_at);
        END IF;
    END
$$;

-- ------------------------------------------------------------------------------
-- 3. RESTRUCTURE student_criteria_progress table
-- ------------------------------------------------------------------------------
-- Clear old progress records (they are now safely stored in student_criteria_transactions)
TRUNCATE TABLE student_criteria_progress;

-- Drop obsolete legacy columns
ALTER TABLE student_criteria_progress
    DROP COLUMN IF EXISTS completion_source,
    DROP COLUMN IF EXISTS note,
    DROP COLUMN IF EXISTS event_id;

-- Add aggregated progress state columns
ALTER TABLE student_criteria_progress
    ADD COLUMN IF NOT EXISTS is_completed             BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS completed_at             TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS completed_children_count INT     NOT NULL DEFAULT 0;

-- Ensure unique constraint exists for (user_id, criteria_id, academic_year_id)
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_constraint
                       WHERE conname = 'uq_student_criteria_progress') THEN
            ALTER TABLE student_criteria_progress
                ADD CONSTRAINT uq_student_criteria_progress
                    UNIQUE (user_id, criteria_id, academic_year_id);
        END IF;
    END
$$;

-- ------------------------------------------------------------------------------
-- 4. RECALCULATE CRITERIA PROGRESS (Bottom-Up: Leaf -> Parent -> Root)
-- ------------------------------------------------------------------------------
DO
$$
    DECLARE
        curr_level INT;
        min_level  INT;
        max_level  INT;
    BEGIN
        -- Step 4.1: Populate Leaf Criteria Progress from transactions
        -- A leaf criteria is completed if there is at least one transaction record.
        INSERT INTO student_criteria_progress (user_id,
                                               criteria_id,
                                               academic_year_id,
                                               is_completed,
                                               completed_at,
                                               completed_children_count,
                                               is_active,
                                               created_at,
                                               updated_at)
        SELECT t.user_id,
               t.criteria_id,
               t.academic_year_id,
               TRUE              AS is_completed,
               MIN(t.created_at) AS completed_at,
               0                 AS completed_children_count,
               TRUE              AS is_active,
               MIN(t.created_at) AS created_at,
               NOW()             AS updated_at
        FROM student_criteria_transactions t
                 JOIN criterias c ON c.id = t.criteria_id
        WHERE c.is_active = TRUE
          -- Only leaf criteria (no active children)
          AND NOT EXISTS (SELECT 1
                          FROM criterias child
                          WHERE child.parent_id = c.id
                            AND child.is_active = TRUE)
        GROUP BY t.user_id, t.criteria_id, t.academic_year_id
        ON CONFLICT (user_id, criteria_id, academic_year_id)
            DO UPDATE SET is_completed             = EXCLUDED.is_completed,
                          completed_at             = EXCLUDED.completed_at,
                          completed_children_count = 0,
                          updated_at               = NOW();

        -- Step 4.2: Determine level boundaries for non-leaf criteria
        SELECT COALESCE(MAX(level), 0), COALESCE(MIN(level), 0)
        INTO max_level, min_level
        FROM criterias
        WHERE is_active = TRUE
          AND EXISTS (SELECT 1
                      FROM criterias child
                      WHERE child.parent_id = criterias.id
                        AND child.is_active = TRUE);

        -- Step 4.3: Bottom-up evaluation from (max_level) down to min_level (root)
        -- Evaluating level-by-level ensures children are always calculated before parents.
        FOR curr_level IN REVERSE max_level..min_level
            LOOP
                INSERT INTO student_criteria_progress (user_id,
                                                       criteria_id,
                                                       academic_year_id,
                                                       is_completed,
                                                       completed_at,
                                                       completed_children_count,
                                                       is_active,
                                                       created_at,
                                                       updated_at)
                SELECT student_context.user_id,
                       p.id                                              AS criteria_id,
                       student_context.academic_year_id,
                       -- Criteria is completed if completed_children_count >= required_children
                       (COALESCE(child_stats.completed_count, 0) >=
                        COALESCE(p.required_children, child_stats.total_count)
                           AND COALESCE(child_stats.total_count, 0) > 0) AS is_completed,
                       -- Timestamp when parent criteria was satisfied (latest child completion)
                       CASE
                           WHEN (COALESCE(child_stats.completed_count, 0) >=
                                 COALESCE(p.required_children, child_stats.total_count)
                               AND COALESCE(child_stats.total_count, 0) > 0)
                               THEN child_stats.latest_completed_at
                           ELSE NULL
                           END                                           AS completed_at,
                       COALESCE(child_stats.completed_count, 0)          AS completed_children_count,
                       TRUE                                              AS is_active,
                       NOW()                                             AS created_at,
                       NOW()                                             AS updated_at
                FROM criterias p
                         -- For all students who have criteria activity in the academic year
                         CROSS JOIN (SELECT DISTINCT user_id, academic_year_id
                                     FROM student_criteria_transactions) student_context
                    -- Aggregate completion statistics from direct active children
                         LEFT JOIN LATERAL (
                    SELECT COUNT(c.id)                                                         AS total_count,
                           COUNT(c.id) FILTER (WHERE COALESCE(scp.is_completed, FALSE) = TRUE) AS completed_count,
                           MAX(scp.completed_at)
                           FILTER (WHERE COALESCE(scp.is_completed, FALSE) = TRUE)             AS latest_completed_at
                    FROM criterias c
                             LEFT JOIN student_criteria_progress scp
                                       ON scp.criteria_id = c.id
                                           AND scp.user_id = student_context.user_id
                                           AND scp.academic_year_id = student_context.academic_year_id
                    WHERE c.parent_id = p.id
                      AND c.is_active = TRUE
                    ) child_stats ON TRUE
                WHERE p.level = curr_level
                  AND p.is_active = TRUE
                  AND EXISTS (SELECT 1
                              FROM criterias child
                              WHERE child.parent_id = p.id
                                AND child.is_active = TRUE)
                ON CONFLICT (user_id, criteria_id, academic_year_id)
                    DO UPDATE SET is_completed             = EXCLUDED.is_completed,
                                  completed_at             = EXCLUDED.completed_at,
                                  completed_children_count = EXCLUDED.completed_children_count,
                                  updated_at               = NOW();
            END LOOP;
    END
$$;
