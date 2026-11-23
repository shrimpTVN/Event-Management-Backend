-- ============================================================
-- Migration: Staff Profile Uniqueness Constraints
-- Enforces:
--   1. One staff profile per user (uk_staff_profile_user_id)
--   2. One owner per staff ID (uk_staff_profile_staff_id)
-- Safe, transactional, and repeatable.
-- ============================================================

DO $$
DECLARE
    v_schema text;
    v_user_dup_count bigint;
    v_staff_dup_count bigint;
    v_trim_dup_count bigint;
    v_blank_staff_count bigint;
    v_user_constraint_exists boolean;
    v_staff_constraint_exists boolean;
    v_legacy_user_uk text;
BEGIN
    -- 1. Resolve target schema
    BEGIN
        v_schema := current_setting('ddd.staff_profile_migration_schema', true);
        IF v_schema IS NULL OR trim(v_schema) = '' THEN
            v_schema := current_setting('ddd.notification_migration_schema', true);
        END IF;
    EXCEPTION WHEN OTHERS THEN
        v_schema := NULL;
    END;

    IF v_schema IS NULL OR trim(v_schema) = '' THEN
        v_schema := 'public';
    END IF;

    -- 2. Timeouts and locking
    PERFORM set_config('lock_timeout', '5s', true);
    PERFORM set_config('statement_timeout', '60s', true);

    -- 3. Check table existence
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = v_schema AND table_name = 'fanpage_admin_profiles'
    ) THEN
        RAISE EXCEPTION 'Table %.fanpage_admin_profiles does not exist!', v_schema;
    END IF;

    -- Lock table
    EXECUTE format('LOCK TABLE %I.fanpage_admin_profiles IN ACCESS EXCLUSIVE MODE', v_schema);

    -- 4. Preflight data integrity checks
    -- 4.1 Check duplicate user_id
    EXECUTE format(
        'SELECT count(*) FROM (SELECT user_id FROM %I.fanpage_admin_profiles WHERE user_id IS NOT NULL GROUP BY user_id HAVING count(*) > 1) t',
        v_schema
    ) INTO v_user_dup_count;

    IF v_user_dup_count > 0 THEN
        RAISE EXCEPTION 'Cannot apply uniqueness constraint: found % duplicate user_id(s) in %.fanpage_admin_profiles', v_user_dup_count, v_schema;
    END IF;

    -- 4.2 Check null or blank staff_id
    EXECUTE format(
        'SELECT count(*) FROM %I.fanpage_admin_profiles WHERE staff_id IS NULL OR trim(staff_id) = ''''',
        v_schema
    ) INTO v_blank_staff_count;

    IF v_blank_staff_count > 0 THEN
        RAISE EXCEPTION 'Cannot apply uniqueness constraint: found % null or blank staff_id(s) in %.fanpage_admin_profiles', v_blank_staff_count, v_schema;
    END IF;

    -- 4.3 Check exact duplicate staff_id
    EXECUTE format(
        'SELECT count(*) FROM (SELECT staff_id FROM %I.fanpage_admin_profiles WHERE staff_id IS NOT NULL GROUP BY staff_id HAVING count(*) > 1) t',
        v_schema
    ) INTO v_staff_dup_count;

    IF v_staff_dup_count > 0 THEN
        RAISE EXCEPTION 'Cannot apply uniqueness constraint: found % duplicate staff_id(s) in %.fanpage_admin_profiles', v_staff_dup_count, v_schema;
    END IF;

    -- 4.4 Check trim collisions on staff_id
    EXECUTE format(
        'SELECT count(*) FROM (SELECT trim(staff_id) FROM %I.fanpage_admin_profiles WHERE staff_id IS NOT NULL GROUP BY trim(staff_id) HAVING count(*) > 1) t',
        v_schema
    ) INTO v_trim_dup_count;

    IF v_trim_dup_count > 0 THEN
        RAISE EXCEPTION 'Cannot apply uniqueness constraint: found % duplicate/collision staff_id(s) after trimming in %.fanpage_admin_profiles', v_trim_dup_count, v_schema;
    END IF;

    -- 5. Safe whitespace normalization
    EXECUTE format(
        'UPDATE %I.fanpage_admin_profiles SET staff_id = trim(staff_id) WHERE staff_id <> trim(staff_id)',
        v_schema
    );

    -- 6. Install or align uk_staff_profile_user_id
    SELECT EXISTS (
        SELECT 1 
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        JOIN pg_class cl ON cl.oid = c.conrelid
        WHERE n.nspname = v_schema 
          AND cl.relname = 'fanpage_admin_profiles'
          AND c.conname = 'uk_staff_profile_user_id'
    ) INTO v_user_constraint_exists;

    IF NOT v_user_constraint_exists THEN
        SELECT c.conname INTO v_legacy_user_uk
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        JOIN pg_class cl ON cl.oid = c.conrelid
        JOIN pg_attribute a ON a.attrelid = cl.oid AND a.attnum = ANY(c.conkey)
        WHERE n.nspname = v_schema 
          AND cl.relname = 'fanpage_admin_profiles'
          AND c.contype = 'u'
          AND array_length(c.conkey, 1) = 1
          AND a.attname = 'user_id'
        LIMIT 1;

        IF v_legacy_user_uk IS NOT NULL THEN
            EXECUTE format('ALTER TABLE %I.fanpage_admin_profiles RENAME CONSTRAINT %I TO uk_staff_profile_user_id', v_schema, v_legacy_user_uk);
        ELSE
            EXECUTE format('ALTER TABLE %I.fanpage_admin_profiles ADD CONSTRAINT uk_staff_profile_user_id UNIQUE (user_id)', v_schema);
        END IF;
    END IF;

    -- 7. Install uk_staff_profile_staff_id
    SELECT EXISTS (
        SELECT 1 
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        JOIN pg_class cl ON cl.oid = c.conrelid
        WHERE n.nspname = v_schema 
          AND cl.relname = 'fanpage_admin_profiles'
          AND c.conname = 'uk_staff_profile_staff_id'
    ) INTO v_staff_constraint_exists;

    IF NOT v_staff_constraint_exists THEN
        EXECUTE format('ALTER TABLE %I.fanpage_admin_profiles ADD CONSTRAINT uk_staff_profile_staff_id UNIQUE (staff_id)', v_schema);
    END IF;

    RAISE NOTICE 'Staff profile uniqueness migration successfully completed for schema %', v_schema;
END $$;
