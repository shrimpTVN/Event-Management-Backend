# Notification Type Constraint Migration Rollout & Operator Runbook

**Date:** 2026-11-23  
**Target:** PostgreSQL Database (`notifications` table type CHECK constraint)  
**Issue Reference:** SQLState `23514` on `notifications_type_check` when inserting `EVENT_ABSENCE_ALERT`

---

## 1. Prerequisites

Before executing preflight or migration scripts, ensure:
1. **Target Schema**: Identified and verified (e.g., `public`).
2. **Access Privileges**: Migration role must have `ALTER TABLE`, `LOCK TABLE`, and `CREATE SEQUENCE` privileges on the target schema.
3. **Connection Configuration**: Use `PGSERVICE` or protected environment variables (`PGHOST`, `PGPORT`, `PGUSER`, `PGDATABASE`). **Do not pass plaintext passwords in command arguments.**
4. **Backup**: Verify database snapshot or pg_dump reference exists prior to executing DDL in production.
5. **Lock Contention**: The migration applies bounded timeouts:
   - `lock_timeout = '5s'`
   - `statement_timeout = '60s'`
   - Table is locked in `ACCESS EXCLUSIVE` mode during constraint swap. Schedule during an agreed maintenance window or low-traffic period.

---

## 2. Step 1: Preflight Inspection (Read-Only)

Execute the read-only preflight diagnostic script to inspect the existing layout, row count, constraints, and data distribution:

```sh
psql -X --set=ON_ERROR_STOP=1 --set=target_schema=public <<'SQL'
SELECT set_config('ddd.notification_migration_schema', :'target_schema', false);
\i ddd-infrastructure/src/main/resources/database/migration/notification-absence-alert-type-preflight.sql
SQL
```

### Preflight Decision Tree:
- **Case 1: Column `type` exists, no invalid historical values outside enum**:
  Proceed directly to Step 2 (Migration).
- **Case 2: Column `title` exists and `type` does NOT exist**:
  Execute legacy title-to-type migration first:
  ```sh
  psql -X --set=ON_ERROR_STOP=1 --set=target_schema=public <<'SQL'
  SELECT set_config('ddd.notification_migration_schema', :'target_schema', false);
  \i ddd-infrastructure/src/main/resources/database/migration/notification-type-refactor-migration.sql
  SQL
  ```
  Then rerun preflight to verify.
- **Case 3: Non-null values exist outside the allowed enum set**:
  **STOP.** Do not apply the migration. Review and normalize invalid values first (e.g. trimming whitespace or mapping legacy values).
- **Case 4: Unexpected/custom type constraints present**:
  **STOP.** Review custom constraints before replacing `notifications_type_check`.

---

## 3. Step 2: Transactional Migration Application

Execute the atomic migration script to replace `notifications_type_check` with the complete 10-value enum allowlist:

```sh
psql -X --set=ON_ERROR_STOP=1 --set=target_schema=public <<'SQL'
SELECT set_config('ddd.notification_migration_schema', :'target_schema', false);
\i ddd-infrastructure/src/main/resources/database/migration/notification-absence-alert-type-migration.sql
SQL
```

The script:
1. Acquires `ACCESS EXCLUSIVE` lock with a 5-second timeout.
2. Validates existing rows against all 10 allowed types.
3. Drops existing `notifications_type_check`.
4. Re-creates `notifications_type_check` containing:
   - `FANPAGE_APPROVED`
   - `FANPAGE_REJECTED`
   - `FANPAGE_MEMBER_INVITED`
   - `FANPAGE_INVITATION_ACCEPTED`
   - `FANPAGE_INVITATION_REJECTED`
   - `FANPAGE_ADMIN_APPOINTED`
   - `EVENT_CREATED`
   - `EVENT_CHECKOUT_AWARDED`
   - `EVENT_ABSENCE_ALERT`
   - `SYSTEM`
5. Confirms `convalidated = true` before committing.

---

## 4. Step 3: Post-Migration Verification

Run a verification block within a rollback-only transaction:

```sh
psql -X --set=ON_ERROR_STOP=1 --set=target_schema=public <<'SQL'
BEGIN;
SELECT set_config('ddd.notification_migration_schema', :'target_schema', false);

-- 1. Verify EVENT_ABSENCE_ALERT insertion succeeds
INSERT INTO notifications (type, message, status) 
VALUES ('EVENT_ABSENCE_ALERT', 'Verification test', 'UNREAD');

-- 2. Verify invalid type is rejected
DO $$
BEGIN
    BEGIN
        INSERT INTO notifications (type, message, status) VALUES ('UNKNOWN_TYPE', 'Test', 'UNREAD');
        RAISE EXCEPTION 'Constraint validation failed: UNKNOWN_TYPE was accepted!';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'SUCCESS: Invalid type properly rejected by notifications_type_check.';
    END;
END $$;

ROLLBACK;
SQL
```

---

## 5. Rollback & Failure Recovery Procedures

1. **Lock Timeout (`57014` / `55P03`)**:
   - The transaction aborts automatically without altering table state or locking writers. Reschedule when traffic is lower.
2. **Invalid Data Encountered**:
   - The transaction aborts with an actionable error listing the invalid types and row counts. No data is modified.
3. **Application Rollback (Code deployment reverted)**:
   - If application code is rolled back, **keep the updated constraint in place**. Retaining the expanded allowlist does not break older application code, whereas reverting to the restricted constraint would fail if any `EVENT_ABSENCE_ALERT` rows have already been committed.
4. **Idempotence**:
   - `notification-absence-alert-type-migration.sql` is repeatable. Re-executing it on an already migrated table safely verifies and retains the constraint.

---

## 6. Integration Test Evidence

The migration suite was verified against a live PostgreSQL 18 instance using `NotificationTypeMigrationIT`:
- `upgradesOldCheckWithoutChangingRows`: **PASSED** (asserted SQLState `23514` on old check, zero row alteration, allowed post-migration).
- `acceptsEveryCurrentEnumValue`: **PASSED** (all 10 `NotificationTypeEnum` values verified).
- `rejectsUnknownNonNullType`: **PASSED** (asserted SQLState `23514`).
- `preservesNullTypes`: **PASSED** (`NULL` type accepted).
- `abortsOnUnknownHistoricalType`: **PASSED** (aborts without altering constraint).
- `preservesUnrelatedConstraintsAndIndexes`: **PASSED** (unrelated checks and indexes intact).
- `isRepeatable`: **PASSED** (repeated execution verified).
- `targetsOnlyConfiguredSchema`: **PASSED** (schema isolation verified).
- `rollsBackConstraintChangeOnFailure`: **PASSED** (atomic rollback verified).
- `freshBootstrapProducesExpectedContract`: **PASSED** (clean install from `schema.sql` produces exact contract).
