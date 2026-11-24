-- ============================================================
-- Migration: Update notifications_type_check Constraint
-- Expands allowlist to include EVENT_ABSENCE_ALERT and all NotificationTypeEnum values
-- Requires session configuration: ddd.notification_migration_schema
-- ============================================================

BEGIN;

DO $$
DECLARE
    v_schema text;
    v_table_exists boolean;
    v_has_type boolean;
    v_invalid_count bigint;
    v_invalid_samples text;
    v_constraint_name text;
    v_is_valid boolean;
    c_allowed_types text[] := ARRAY[
        'FANPAGE_APPROVED',
        'FANPAGE_REJECTED',
        'FANPAGE_MEMBER_INVITED',
        'FANPAGE_INVITATION_ACCEPTED',
        'FANPAGE_INVITATION_REJECTED',
        'FANPAGE_ADMIN_APPOINTED',
        'EVENT_CREATED',
        'EVENT_CHECKOUT_AWARDED',
        'EVENT_ABSENCE_ALERT',
        'SYSTEM'
    ];
BEGIN
    BEGIN
        v_schema := current_setting('ddd.notification_migration_schema', true);
    EXCEPTION WHEN OTHERS THEN
        v_schema := NULL;
    END;

    IF v_schema IS NULL OR trim(v_schema) = '' THEN
        RAISE EXCEPTION 'Target schema is not configured! Please set ddd.notification_migration_schema.';
    END IF;

    -- Apply bounded lock and statement timeouts
    EXECUTE 'SET LOCAL lock_timeout = ''5s''';
    EXECUTE 'SET LOCAL statement_timeout = ''60s''';

    -- Check table existence
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = v_schema AND table_name = 'notifications'
    ) INTO v_table_exists;

    IF NOT v_table_exists THEN
        RAISE EXCEPTION 'Table %.notifications does not exist!', v_schema;
    END IF;

    -- Lock the table exclusively
    EXECUTE format('LOCK TABLE %I.notifications IN ACCESS EXCLUSIVE MODE', v_schema);

    -- Check columns: type must exist
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = v_schema AND table_name = 'notifications' AND column_name = 'type'
    ) INTO v_has_type;

    IF NOT v_has_type THEN
        RAISE EXCEPTION 'Column "type" does not exist in %.notifications! Run title-to-type migration first.', v_schema;
    END IF;

    -- Validate existing non-null data
    EXECUTE format(
        'SELECT count(*), string_agg(DISTINCT quote_literal(type), '', '') FROM %I.notifications WHERE type IS NOT NULL AND type <> ALL($1)',
        v_schema
    ) INTO v_invalid_count, v_invalid_samples USING c_allowed_types;

    IF v_invalid_count > 0 THEN
        RAISE EXCEPTION 'Cannot apply migration: % notification rows have invalid type values: [%]', v_invalid_count, v_invalid_samples;
    END IF;

    -- Find existing notifications_type_check if present
    SELECT c.conname, c.convalidated INTO v_constraint_name, v_is_valid
    FROM pg_constraint c
    JOIN pg_namespace n ON n.oid = c.connamespace
    JOIN pg_class cl ON cl.oid = c.conrelid
    WHERE n.nspname = v_schema AND cl.relname = 'notifications' AND c.conname = 'notifications_type_check';

    IF v_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %I.notifications DROP CONSTRAINT notifications_type_check', v_schema);
    END IF;

    -- Add the updated constraint
    EXECUTE format(
        'ALTER TABLE %I.notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (
            ''FANPAGE_APPROVED'',
            ''FANPAGE_REJECTED'',
            ''FANPAGE_MEMBER_INVITED'',
            ''FANPAGE_INVITATION_ACCEPTED'',
            ''FANPAGE_INVITATION_REJECTED'',
            ''FANPAGE_ADMIN_APPOINTED'',
            ''EVENT_CREATED'',
            ''EVENT_CHECKOUT_AWARDED'',
            ''EVENT_ABSENCE_ALERT'',
            ''SYSTEM''
        ))',
        v_schema
    );

    -- Verify constraint is validated
    SELECT c.convalidated INTO v_is_valid
    FROM pg_constraint c
    JOIN pg_namespace n ON n.oid = c.connamespace
    JOIN pg_class cl ON cl.oid = c.conrelid
    WHERE n.nspname = v_schema AND cl.relname = 'notifications' AND c.conname = 'notifications_type_check';

    IF NOT v_is_valid THEN
        RAISE EXCEPTION 'Constraint notifications_type_check was added but is not validated!';
    END IF;

    RAISE NOTICE 'Successfully applied notifications_type_check constraint on %.notifications', v_schema;
END $$;

COMMIT;
