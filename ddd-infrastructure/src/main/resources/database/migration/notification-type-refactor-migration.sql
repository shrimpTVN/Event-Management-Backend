-- ============================================================
-- Migration: Refactor Notification title to type (NotificationTypeEnum)
-- Safe title-to-type compatibility on old and fresh schemas
-- Requires session configuration: ddd.notification_migration_schema
-- ============================================================

DO $$
DECLARE
    v_schema text;
    v_has_title boolean;
    v_has_type boolean;
    v_oversized_count bigint;
BEGIN
    BEGIN
        v_schema := current_setting('ddd.notification_migration_schema', true);
    EXCEPTION WHEN OTHERS THEN
        v_schema := NULL;
    END;

    IF v_schema IS NULL OR trim(v_schema) = '' THEN
        v_schema := 'public';
    END IF;

    -- Check column existence
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = v_schema AND table_name = 'notifications' AND column_name = 'title'
    ) INTO v_has_title;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = v_schema AND table_name = 'notifications' AND column_name = 'type'
    ) INTO v_has_type;

    IF v_has_title AND NOT v_has_type THEN
        -- Check if any title exceeds 50 chars before narrowing
        EXECUTE format('SELECT count(*) FROM %I.notifications WHERE length(title) > 50', v_schema) INTO v_oversized_count;
        IF v_oversized_count > 0 THEN
            RAISE EXCEPTION 'Cannot rename title to type: % rows have title exceeding 50 characters', v_oversized_count;
        END IF;

        EXECUTE format('ALTER TABLE %I.notifications RENAME COLUMN title TO type', v_schema);
        EXECUTE format('ALTER TABLE %I.notifications ALTER COLUMN type TYPE VARCHAR(50)', v_schema);
    ELSIF v_has_type AND NOT v_has_title THEN
        -- Column type already exists, check if its length needs to be 50
        EXECUTE format('ALTER TABLE %I.notifications ALTER COLUMN type TYPE VARCHAR(50)', v_schema);
    ELSIF v_has_title AND v_has_type THEN
        -- Both exist: keep type
        RAISE NOTICE 'Both title and type columns exist in %.notifications. Keeping type.', v_schema;
    ELSE
        RAISE EXCEPTION 'Neither title nor type column exists in %.notifications!', v_schema;
    END IF;

    -- Create index safely if absent
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_notifications_type ON %I.notifications (type)', v_schema);
END $$;
