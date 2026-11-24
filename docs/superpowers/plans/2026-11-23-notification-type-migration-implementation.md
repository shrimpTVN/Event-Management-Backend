# Notification Type Migration Implementation Plan

> **For agentic workers:** Use the repository's `executing-plans` skill to implement this plan task-by-task. Steps use checkbox syntax for tracking. This plan expands Task 1 only; notification delivery changes belong to Task 2 of the parent plan.

**Goal:** Make PostgreSQL accept `EVENT_ABSENCE_ALERT` and every other current notification type, reject unknown non-null types, and preserve existing notifications during both upgrade and fresh installation.

**Architecture:** Add an explicitly applied SQL migration with catalog-based preflight and transactional constraint replacement. Keep fresh-install SQL and the existing title-to-type transition compatible. Verify the actual SQL scripts using JDBC against isolated PostgreSQL schemas, without booting the application or allowing Hibernate to conceal missing schema changes.

**Tech Stack:** Existing Maven, JUnit, PostgreSQL JDBC driver, PostgreSQL, and SQL resource files. No Flyway/Liquibase, Testcontainers, or dependency upgrade is required.

**Spec:** [Issue report, C](../../bug/backend-account-security-and-staff-profile-request.md); [parent plan, Task 1](2026-11-23-account-security-staff-profile-refactor.md).

**Status:** Planning only. Shared-database inspection and migration application have not been performed. Deployment uses the observed target definition; an unexpected constraint or legacy value is a stop condition for that target, not permission to overwrite it.

## Global constraints

- Retain all rows, notification IDs, relationships, messages, status values, and nullable fields.
- Preserve the Java enum and the endpoint/payload. Do not replace `EVENT_ABSENCE_ALERT` with a different value.
- Keep `type` nullable: current entity metadata permits null. Reject unknown non-null types without adding a new NOT NULL requirement.
- Preserve unrelated CHECK constraints, foreign keys, indexes, grants, and defaults on upgraded databases.
- Apply SQL explicitly; the inspected configuration uses `ddl-auto: update` and does not establish an automatic SQL migration runner.
- Do not run tests against a shared application database. Use a dedicated disposable PostgreSQL database with schema-creation rights.
- Keep runtime Java and SSE behavior out of scope. Passing this plan does not prove notifications are delivered only after commit.
- Use the existing build toolchain. The parent POM declares Java 21 while the infrastructure POM also contains Java 25 compiler properties; record the effective build/JDK requirements before running tests rather than changing them incidentally.

## Review focus

1. Extra legacy CHECK predicates must survive; ambiguous/custom type constraints must abort instead of being silently removed.
2. Null and invalid historical types must have different outcomes: preserve nulls, reject incompatible non-null values without changing rows.
3. Multiple schemas containing `notifications` must not cause the wrong table to change.
4. A failed or repeated migration must leave a valid constraint and all data intact.
5. Fresh bootstrap and legacy scripts must agree on column names, sequence generation, and indexes without relying on Hibernate repair.

## File map

| Action | Path | Responsibility |
| --- | --- | --- |
| Create | `ddd-infrastructure/src/main/resources/database/migration/notification-absence-alert-type-preflight.sql` | Read-only catalog and data report for operators |
| Create | `ddd-infrastructure/src/main/resources/database/migration/notification-absence-alert-type-migration.sql` | Atomic, repeatable type CHECK replacement |
| Modify | `ddd-infrastructure/src/main/resources/database/migration/notification-type-refactor-migration.sql` | Safe title-to-type compatibility on old and fresh schemas |
| Modify | `ddd-infrastructure/src/main/resources/database/schema.sql` | Fresh notification table type and CHECK definition |
| Modify if required by fresh-bootstrap tests | `ddd-infrastructure/src/main/resources/database/migration/notification-sequence-migration.sql` | Handle IDENTITY versus plain default when installing the existing notification sequence |
| Reference | `ddd-infrastructure/src/main/resources/database/migration/notification-fanpage-support-migration.sql` | Fanpage column, FK, and indexes in fresh bootstrap |
| Create | `ddd-infrastructure/src/test/java/com/ddd/infrastructure/migration/NotificationTypeMigrationIT.java` | PostgreSQL script verification and enum coverage |
| Create | `docs/guidelines/account-security-staff-profile-rollout.md` | Exact operator procedure, prerequisites, verification, and rollback limits |

The `IT` suffix intentionally keeps database-required tests outside Surefire's ordinary test-name selection. The explicit command below runs them; missing database configuration must fail that command, never mark these checks as passed/skipped. This refines the parent plan's proposed `NotificationTypeMigrationTest` filename.

## Task 1.1: Pin the deployed-schema prerequisites and capture baseline

**Interfaces:** The operator sets `PGSERVICE` using a protected libpq service entry and supplies `target_schema` as a psql variable. No passwords or raw connection URLs are copied into source, commands in logs, or the rollout report. Scripts use the explicitly supplied schema rather than assuming `public`.

- [ ] Record `java -version`, `mvn -version`, the effective compiler configuration, and `mvn -pl ddd-infrastructure -am test` results. Record pre-existing failures separately; do not fix unrelated build issues in this delivery.
- [ ] Create the read-only preflight script. Resolve the table by the supplied schema, report the database name, schema, PostgreSQL version, notification column definitions, row count, and constraint names/definitions from `pg_constraint` using `pg_get_constraintdef` and `convalidated`.
- [ ] Report whether `title`, `type`, both, or neither exists. Read distinct values/counts only from the existing applicable column. Report non-null values outside the enum and count nulls separately. Do not dump messages, recipient data, or credentials.
- [ ] Include type-related CHECK dependencies using `pg_attribute`/`pg_constraint.conkey`, sequence/default/identity metadata for `id`, notification indexes, and schema-qualified foreign-key targets. There is no established migration history table in the inspected configuration; capture the deployment's external record if one exists, otherwise state its absence.
- [ ] Verify that preflight performs no writes, including on missing/ambiguous table layouts. Add the operator command to the rollout document:

```sh
psql -X --set=ON_ERROR_STOP=1 --set=target_schema=public \
  --file=ddd-infrastructure/src/main/resources/database/migration/notification-absence-alert-type-preflight.sql
```

`public` is an example; replace it with the verified application schema. `PGSERVICE` selects the intended connection. A schema other than the intended one is a failed preflight.

- [ ] Classify a target as supported only after reviewing its type constraints: no CHECK, the expected `notifications_type_check` containing only an allowed-value restriction on `type`, or the equivalent expected constraint carried through the title rename. Any additional/custom type predicate requires a reviewed preservation strategy before applying the migration.

**Done when:** The rollout procedure identifies the target unambiguously and separates observed schema facts from historical report claims. Lack of shared DB access does not block writing/testing the scripts, but leaves deployment-specific preflight explicitly incomplete.

## Task 1.2: Build a failing PostgreSQL test harness

**Files:** New `NotificationTypeMigrationIT.java`; resources from the file map.

**Interfaces:** Test configuration uses `NOTIFICATION_MIGRATION_TEST_JDBC_URL`, `NOTIFICATION_MIGRATION_TEST_DB_USER`, and `NOTIFICATION_MIGRATION_TEST_DB_PASSWORD`. Missing configuration is a test failure with a redacted setup message. JDBC credentials are supplied separately and never printed. Each test creates two uniquely named schemas, sets the target explicitly, and drops only its own schemas in cleanup.

- [ ] Implement helpers `createFixture(Connection connection, String schema)`, `executeScript(Connection connection, String resourcePath, String schema)`, and `readNotificationSnapshot(Connection connection, String schema)`. Use full script execution rather than splitting on semicolons, because procedural SQL blocks contain semicolons. Keep generated schema identifiers restricted to a safe alphabet and quote them correctly.
- [ ] Make SQL resources runnable through JDBC and psql: pass the target through a session setting `ddd.notification_migration_schema`; the psql operator command initializes that setting through `set_config` using a safely quoted psql value. SQL scripts resolve it with `current_setting` and quote identifiers through `format('%I', ...)`. Fail if it is missing/empty. Preflight may use the same setting. Do not interpolate untrusted schema names into SQL.
- [ ] Create a minimal legacy fixture with users/notifications sufficient for real inserts, a type-only CHECK excluding `EVENT_ABSENCE_ALERT`, a seeded valid notification, an unrelated CHECK, and an unrelated index. Use explicit IDs in CHECK tests so sequence generation cannot hide or mimic type failures.
- [ ] Add tests named `upgradesOldCheckWithoutChangingRows`, `acceptsEveryCurrentEnumValue`, and `rejectsUnknownNonNullType`. Iterate `NotificationTypeEnum.values()` to assert each enum string can be inserted; assert an unknown value fails with SQLState `23514` on the type CHECK. Snapshot existing rows before/after and compare every column, not just counts.
- [ ] Confirm the acceptance test fails before applying the new migration because `EVENT_ABSENCE_ALERT` is rejected. Other tests may fail because the planned resource does not yet exist; record both stages clearly.

Run from repository root:

```sh
mvn -pl ddd-infrastructure -am \
  -Dtest=NotificationTypeMigrationIT \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

**Done when:** The harness exercises an actual PostgreSQL server, has a red/green path, fails without test DB configuration, and cannot drop arbitrary schemas.

## Task 1.3: Implement the transactional constraint migration

**Files:** New migration SQL; PostgreSQL tests.

**Interfaces:** Read the explicit schema setting, require an existing `notifications.type` column of the supported layout, and leave a validated `notifications_type_check`. The script owns its `BEGIN`/`COMMIT`; callers must use an initially auto-commit connection and must not add an outer transaction. On SQL error the caller rolls back/closes the failed transaction.

- [ ] Encode the complete current allowlist in the SQL:

```text
FANPAGE_APPROVED
FANPAGE_REJECTED
FANPAGE_MEMBER_INVITED
FANPAGE_INVITATION_ACCEPTED
FANPAGE_INVITATION_REJECTED
FANPAGE_ADMIN_APPOINTED
EVENT_CREATED
EVENT_CHECKOUT_AWARDED
EVENT_ABSENCE_ALERT
SYSTEM
```

Keep it explicit in SQL; the enum-driven tests detect future omissions. Do not generate a deployed allowlist from existing rows, which could preserve invalid values or omit unused valid types.

- [ ] Start the transaction with bounded `lock_timeout` (5 seconds) and `statement_timeout` (60 seconds), then lock the qualified notifications table with `ACCESS EXCLUSIVE` before validating data or replacing constraints. This provides a simple atomic repair for a bounded table; if validation cannot finish within the limit, abort and schedule a separately reviewed large-table procedure instead of silently relaxing timeouts.
- [ ] Recheck the table/column layout, constraint dependencies, and existing non-null values while holding the lock. Reject unknown values with counts/type identifiers and an actionable exception, without updating data. Preserve nulls. Reject both-column/neither-column layouts and non-type-only or unexpectedly named type CHECKs until reviewed.
- [ ] Drop only the verified replaceable type CHECK, then add `notifications_type_check CHECK (type IN (...))` and require it to be validated before commit. Never use a blanket loop that drops every table CHECK. A target without a type CHECK receives the new constraint.
- [ ] Define repeat behavior as transactional replacement of the same verified type-only constraint with the same allowed set. It may reacquire the lock, but must preserve rows and keep exactly one intended type CHECK. Retain unrelated objects.
- [ ] Add `preservesNullTypes`, `abortsOnUnknownHistoricalType`, `preservesUnrelatedConstraintsAndIndexes`, `rejectsCustomTypePredicates`, `isRepeatable`, `targetsOnlyConfiguredSchema`, and `rollsBackConstraintChangeOnFailure`. The rollback test forces failure after the old CHECK would have been dropped, rolls back, then verifies the old constraint and rows through a new connection.
- [ ] Add `failsWithinLockTimeout` using a second connection holding a conflicting lock. Assert timeout and unchanged state; do not assert an exact wall-clock duration. Run all tests and inspect the final `convalidated` flag.

**Done when:** Every enum value is accepted, unknown non-null values are rejected, and failures/repeats preserve data and unrelated schema objects.

## Task 1.4: Align fresh installations and legacy compatibility

**Files:** `schema.sql`, legacy type migration, sequence migration only as needed, integration tests.

- [ ] Change the fresh notifications declaration from `title VARCHAR(255)` to `type VARCHAR(50)` and add the same type CHECK. Preserve the existing nullable type semantics and unrelated table definitions. Do not change status values in this task.
- [ ] Make `notification-type-refactor-migration.sql` check column existence before renaming: title only → rename; type only → retain; both/neither → abort. Before narrowing to 50 characters, reject oversized existing values instead of truncating them. Create the existing type index safely if absent and detect an incompatible same-name index.
- [ ] Test a populated title-only fixture, an already-current fixture, repeated execution, both-column and neither-column layouts, and overlong legacy text. Assert failed upgrades preserve the original column/data and do not partly rename the table layout.
- [ ] Verify fresh bootstrap in a disposable database from actual repository resources: execute `schema.sql`, then the title/type compatibility script, notification fanpage-support script, notification sequence script, and the new CHECK migration. Identify and provide required prerequisite seed data only if the actual scripts require it. Assert the final table has `type`, nullable user linkage, fanpage FK/indexes, and the intended enum CHECK.
- [ ] Address the observed identity/sequence mismatch only if required by this test: fresh SQL creates an identity column, while the current sequence script uses `DROP DEFAULT`. Update that script to branch on identity metadata (`DROP IDENTITY` for identity, `DROP DEFAULT` for ordinary defaults), install the existing named sequence with increment 50, and position it above current IDs. Run this with writers stopped; do not reset a live allocator. Remove diagnostic `SHOW log_statement` from an automated bootstrap path if it obstructs execution.
- [ ] Test sequence initialization for both identity-based fresh fixtures and populated plain-default fixtures. Assert a generated insert has no PK collision and allocation increment matches `NotificationJpaEntity`'s 50. Do not undertake sequence-allocation redesign or promise arbitrary online re-execution of the legacy sequence migration.
- [ ] Run the new CHECK migration after fresh bootstrap, run it a second time, and repeat enum/invalid-value/data-preservation assertions. Verify bootstrapping without Hibernate; record unrelated bootstrap failures rather than claiming an application restart resolves them.

**Done when:** Both the upgraded and fresh schemas pass the same type contract. Additional legacy scripts may be one-time bootstrap steps; distinguish that from the new CHECK migration's repeatability.

## Task 1.5: Write the operator runbook and release evidence

**Files:** `docs/guidelines/account-security-staff-profile-rollout.md`.

- [ ] Document prerequisites: protected `PGSERVICE`, the verified target schema, migration role privileges, backup/restore reference, current row count, constraint report, expected write-blocking window, and the exact version/commit of SQL being applied. Require an agreed maintenance window for this locking strategy.
- [ ] Provide a schema-setting wrapper for both preflight and migration without passing credentials on the command line. For example, using a single psql session:

```sh
psql -X --set=ON_ERROR_STOP=1 --set=target_schema=public <<'SQL'
SELECT set_config('ddd.notification_migration_schema', :'target_schema', false);
\i ddd-infrastructure/src/main/resources/database/migration/notification-absence-alert-type-preflight.sql
SQL
```

Use a separate reviewed invocation replacing the included filename with the migration script to apply changes. Scripts execute from repository root. The quoted heredoc prevents shell expansion; psql quotes the schema value.

- [ ] Document upgrade branches explicitly: supported `type` layout → preflight/new migration; title-only → reviewed legacy transition first, then rerun preflight; both/neither, custom constraints, unknown existing values, or wrong schema → stop and investigate. Never apply the full fresh schema to a populated application database.
- [ ] Document the fresh sequence from Task 1.4 as one-time bootstrap. Do not execute the fanpage-support or sequence scripts blindly on an existing database. SQL files are not automatically run by a restart.
- [ ] Specify postconditions: row snapshot/count unchanged, every current type present in the validated constraint, unrelated constraints intact, unknown type rejected in a rollback-only verification transaction, and `EVENT_ABSENCE_ALERT` accepted using a valid test recipient in an isolated environment. Capture user-visible absence-alert smoke-test evidence during later integration; this task does not send real notifications.
- [ ] Document failure recovery: lock timeout → unchanged database and reschedule; bad data/custom CHECK → unchanged database and separate reviewed repair; script error → rollback and verify old state. After committing new-type rows, an application rollback must retain the expanded CHECK; restoring the old restriction would reject those rows. No down migration deletes notification data.
- [ ] Run the explicit PostgreSQL test command, ordinary module tests, and `git diff --check`. Record test count, PostgreSQL version, fixture variants, actual results, and any environment blocker. A skipped/missing DB run is unfinished verification.
- [ ] Review/stage only files belonging to this task and commit with `fix: align notification type constraints with enum`. Preserve unrelated workspace changes.

## Acceptance and handoff

- [ ] Populated old schema upgrades without row changes and supports all ten current enum values.
- [ ] Unknown non-null types are rejected; existing nulls remain allowed.
- [ ] Repeated CHECK migration, forced rollback, schema ambiguity, custom constraints, and lock contention are tested.
- [ ] Fresh SQL plus the documented bootstrap sequence produces the same verified type contract.
- [ ] Operator instructions identify the correct database/schema, apply scripts explicitly, and explain rollback limitations.
- [ ] Deployment-specific preflight/application is reported separately from isolated test success.

No API or FE change is needed for this constraint repair. After this delivery, proceed to the parent plan's Task 2 to address pre-commit notification pushes; do not mark the whole notification issue complete solely because inserts now succeed.
