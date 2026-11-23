-- ============================================================
-- Preflight Check: Notifications Table Type Constraint Migration
-- Read-only inspection of schema, columns, rows, constraints, and indexes.
-- Requires session configuration: ddd.notification_migration_schema
-- ============================================================

DO $$
DECLARE
    v_schema text;
    v_table_exists boolean;
    v_has_title boolean;
    v_has_type boolean;
    v_row_count bigint;
    v_null_type_count bigint;
    v_invalid_type_count bigint;
    v_rec record;
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

    RAISE NOTICE '============================================================';
    RAISE NOTICE 'PREFLIGHT CHECK FOR NOTIFICATIONS TABLE MIGRATION';
    RAISE NOTICE 'Target Schema: % | Database: % | PostgreSQL Version: %',
        v_schema, current_database(), version();
    RAISE NOTICE '============================================================';

    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = v_schema AND table_name = 'notifications'
    ) INTO v_table_exists;

    IF NOT v_table_exists THEN
        RAISE NOTICE 'STATUS: Table %.notifications does NOT exist.', v_schema;
        RETURN;
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

    RAISE NOTICE 'Column Status: title=% | type=%', v_has_title, v_has_type;

    EXECUTE format('SELECT count(*) FROM %I.notifications', v_schema) INTO v_row_count;
    RAISE NOTICE 'Total notification rows: %', v_row_count;

    IF v_has_type THEN
        EXECUTE format('SELECT count(*) FROM %I.notifications WHERE type IS NULL', v_schema) INTO v_null_type_count;
        RAISE NOTICE 'Notifications with NULL type: %', v_null_type_count;

        EXECUTE format('SELECT count(*) FROM %I.notifications WHERE type IS NOT NULL AND type <> ALL($1)', v_schema)
            INTO v_invalid_type_count
            USING c_allowed_types;
        RAISE NOTICE 'Notifications with non-null types OUTSIDE current enum: %', v_invalid_type_count;

        RAISE NOTICE 'Distinct non-null types and counts:';
        FOR v_rec IN EXECUTE format('SELECT type, count(*) as cnt FROM %I.notifications WHERE type IS NOT NULL GROUP BY type ORDER BY cnt DESC', v_schema) LOOP
            RAISE NOTICE '  - %: %', v_rec.type, v_rec.cnt;
        END LOOP;
    END IF;

    RAISE NOTICE '------------------------------------------------------------';
    RAISE NOTICE 'Existing Constraints:';
    FOR v_rec IN 
        SELECT conname, contype, convalidated, pg_get_constraintdef(c.oid) as def
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        JOIN pg_class cl ON cl.oid = c.conrelid
        WHERE n.nspname = v_schema AND cl.relname = 'notifications'
    LOOP
        RAISE NOTICE '  - [%] (validated=%): %', v_rec.conname, v_rec.convalidated, v_rec.def;
    END LOOP;

    RAISE NOTICE '------------------------------------------------------------';
    RAISE NOTICE 'Existing Indexes:';
    FOR v_rec IN 
        SELECT indexname, indexdef
        FROM pg_indexes
        WHERE schemaname = v_schema AND tablename = 'notifications'
    LOOP
        RAISE NOTICE '  - %: %', v_rec.indexname, v_rec.indexdef;
    END LOOP;

    RAISE NOTICE '============================================================';
    RAISE NOTICE 'PREFLIGHT CHECK COMPLETED.';
    RAISE NOTICE '============================================================';
END $$;
