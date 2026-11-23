# Staff Profile Uniqueness Implementation Plan

> **For agentic workers:** Use the repository's `executing-plans` skill when implementing this plan. Track the deliverables below without splitting them into individual coding actions.

**Goal:** Enforce one staff profile per user and one owner per staff ID in PostgreSQL, including under concurrent writes, without losing existing data.

**Architecture:** Add an explicitly applied migration that checks existing data before installing unique constraints. Align fresh-install SQL and JPA metadata with the database contract. Keep profile creation and API error translation in Task 5.

**Tech Stack:** Existing PostgreSQL, JPA/Hibernate, Maven, and JUnit. No new migration framework or runtime dependencies.

**Spec:** [Issue report, B.3](../../bug/backend-account-security-and-staff-profile-request.md) and [parent plan, Task 4](2026-11-23-account-security-staff-profile-refactor.md).

## Contract and constraints

The fresh schema already makes `user_id` unique; `staff_id` currently has no declared unique constraint. The existing application check for a duplicate staff ID does not protect concurrent inserts.

Enforce uniqueness across all staff profiles, including inactive profiles. Use trimmed, case-sensitive staff IDs as the proposed application contract. Confirm institutional identifiers do not require case-insensitive matching before deployment. Do not introduce lowercase conversion, case folding, or account merging.

Preserve profile IDs, user links, organization, title, DOB, phone, active status, and existing relationships. DOB and other currently optional fields remain nullable. This task does not require a new gender/date default, grant roles, or create an onboarding endpoint.

## Files

- Create `ddd-infrastructure/src/main/resources/database/migration/staff-profile-uniqueness-migration.sql`.
- Modify `ddd-infrastructure/src/main/resources/database/schema.sql`.
- Modify `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/FanpageAdminProfileJpaEntity.java`.
- Create `ddd-infrastructure/src/test/java/com/ddd/infrastructure/migration/StaffProfileUniquenessMigrationTest.java`.
- Create or update `docs/guidelines/account-security-staff-profile-rollout.md`.

## Implementation

- [ ] **Inspect the target schema and data.** Use an explicit database/schema and report the actual user/staff unique constraints, their backing indexes, column nullability, and any external dependencies. Check duplicate user IDs, exact staff-ID duplicates, collisions after trimming, null IDs, and blank IDs. Include inactive records. Do not assume the deployed schema matches `schema.sql`; stop on incompatible data with record IDs sufficient for an operator to investigate, without dumping unrelated personal details.

- [ ] **Define safe normalization.** Before changing existing staff IDs, identify every value that would change and every collision the change would create. Match the normalization used by the application; do not assume PostgreSQL's default `TRIM` handles every character Java `trim()` handles. Abort on collisions or ambiguous legacy identifiers rather than choosing a surviving profile. Apply any reviewed whitespace cleanup transactionally with the constraint installation. Task 5 must normalize new requests consistently; an exact-value UNIQUE constraint alone does not normalize future input.

- [ ] **Install constraints transactionally.** Lock the schema-qualified profile table before final duplicate checks and DDL so writers cannot introduce a conflict between preflight and installation. Use bounded lock/statement timeouts and abort cleanly if the table cannot be safely processed in the deployment window. Add `uk_staff_profile_staff_id` for `staff_id`. Preserve an equivalent existing `user_id` unique constraint; otherwise add `uk_staff_profile_user_id`. Detect equivalent constraints by their columns and semantics, not names alone. Fail on a conflicting same-name definition instead of dropping unrelated objects. Repeated execution must retain the same uniqueness guarantees without duplicate indexes or data changes.

- [ ] **Align fresh SQL and JPA metadata.** Give fresh installations the two explicit named constraints and represent them in entity table metadata. Keep existing NOT NULL requirements and the one-to-one user relationship. Verify Hibernate does not create redundant uniqueness structures when inspecting an upgraded schema with a legacy constraint name. The migration remains the authoritative deployment step; a restart or `ddl-auto: update` is not proof it ran.

- [ ] **Document the constraint contract for Task 5.** Record the staff-ID constraint name and the actual retained user-ID constraint name. Task 5 must distinguish these violations when returning `STAFF_ID_ALREADY_EXISTS` versus `STAFF_PROFILE_ALREADY_EXISTS`; do not classify every database integrity error as a duplicate profile. Document the reviewed normalization rule and database collation assumptions alongside the names.

## Verification

- [ ] **Test migration safety on PostgreSQL.** Cover populated valid data, missing user uniqueness, an equivalent legacy user constraint, duplicate users, duplicate staff IDs, trim collisions, null/blank legacy values, and conflicting constraint names. Assert failed migrations leave rows and original constraints unchanged. Verify repeat execution, the intended schema when two schemas have the table, and preservation of optional null values and unrelated schema objects. Compare all profile fields, allowing only an explicitly reviewed staff-ID normalization change.

- [ ] **Prove enforcement.** Test duplicate inserts and updates for both keys, with SQLState `23505` identifying the expected constraint. Race two connections inserting the same user with different staff IDs, then different users with the same staff ID: exactly one transaction may commit in each case. Also confirm distinct IDs succeed, inactive records still reserve their IDs, and case-distinct staff IDs behave according to the agreed case-sensitive policy and actual column collation.

- [ ] **Verify deployment and fresh installation.** Run the same uniqueness assertions against the upgraded fixture and fresh `schema.sql`. Run existing infrastructure tests and record actual PostgreSQL test results; missing database verification is incomplete. Document explicit SQL application, timeout/failure recovery, row-count verification, and the names found after migration. Stage only this task's files and preserve unrelated workspace changes.

## Done when

Both fresh and upgraded databases prevent duplicate user profiles and duplicate stored staff IDs under concurrent writes. Existing data is preserved, incompatible legacy data causes a clear rollback, and Task 5 has the constraint names and normalization contract needed for reliable conflict responses. No shared database changes are made merely by writing this plan.
