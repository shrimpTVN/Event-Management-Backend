-- ============================================================
-- Migration: Notification sequence configuration
-- Handles both GENERATED AS IDENTITY and ordinary defaults
-- ============================================================

DO $$
DECLARE
    v_schema text;
    v_is_identity text;
BEGIN
    BEGIN
        v_schema := current_setting('ddd.notification_migration_schema', true);
    EXCEPTION WHEN OTHERS THEN
        v_schema := NULL;
    END;

    IF v_schema IS NULL OR trim(v_schema) = '' THEN
        v_schema := 'public';
    END IF;

    -- 1. Check if id is an identity column or has default, and remove it
    SELECT is_identity INTO v_is_identity
    FROM information_schema.columns
    WHERE table_schema = v_schema AND table_name = 'notifications' AND column_name = 'id';

    IF v_is_identity = 'YES' THEN
        EXECUTE format('ALTER TABLE %I.notifications ALTER COLUMN id DROP IDENTITY IF EXISTS', v_schema);
    ELSE
        EXECUTE format('ALTER TABLE %I.notifications ALTER COLUMN id DROP DEFAULT', v_schema);
    END IF;

    -- 2. Create standalone sequence starting after current max id with increment 50
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.%I START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1', v_schema, 'notifications_id_seq');

    -- 3. Sync sequence to avoid PK collision with existing rows
    EXECUTE format('SELECT setval(''%I.%I''::regclass, COALESCE((SELECT MAX(id) FROM %I.notifications), 0) + 1, false)', v_schema, 'notifications_id_seq', v_schema);

    -- 4. Point id column to new sequence
    EXECUTE format('ALTER TABLE %I.notifications ALTER COLUMN id SET DEFAULT nextval(''%I.%I''::regclass)', v_schema, v_schema, 'notifications_id_seq');
END $$;
