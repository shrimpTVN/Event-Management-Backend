# Account Security, Staff Profiles, and Notifications Implementation Plan

> **For agentic workers:** Use the repository's `executing-plans` skill to implement this plan task-by-task. Steps use checkbox syntax for tracking. This document plans the work; it does not authorize deployment, migration execution against shared databases, or decide the unresolved business policies below.

**Goal:** Resolve the five backend gaps in the issue report while preserving existing account identities, profiles, authorization boundaries, and notification data.

**Architecture:** Keep controllers responsible for validation and response mapping, application services responsible for transactions and business rules, and infrastructure responsible for persistence and notification delivery. Add focused account-security and staff-profile services rather than expanding the existing general-purpose user command service further. Separate profile existence, system roles, registration eligibility, and membership in a specific fanpage.

**Tech Stack:** Existing Java 21, Spring Boot/Security, JPA/Hibernate, PostgreSQL, MapStruct, Maven, JUnit, and Mockito. No framework upgrades in this work.

**Spec:** [Backend issue report](../../bug/backend-account-security-and-staff-profile-request.md), sections A–E. Treat the APIs in that report as proposed contracts and the runtime observations as historical evidence until reproduced.

**Status:** Proposed implementation plan. Tasks 1–6 and 8 are concrete backend work. Task 7 follows the user-confirmed no-verification profile-creation policy and target-fanpage-admin-only absence alerts. No product code or database changes have been made by this plan.

## Global constraints

- Preserve `BaseResponse` and the project's `exceptionCode` error structure. Introduce specific error codes without changing existing unrelated error responses.
- Derive the current user from authentication. Self-service DTOs accept no target user ID, email, password, or roles for profile creation.
- Compute `hasPassword` from a non-null, non-blank stored password; do not add a password-status column or expose the hash.
- Keep email, Google linkage, password, avatar, existing roles, and both kinds of profile unless the requested operation explicitly changes a permitted field.
- Preserve existing refresh-session behavior: password creation replaces refresh sessions; password change revokes them. Do not claim existing access JWTs become immediately invalid.
- Every authenticated user may create their first staff profile without verification. Creation grants no global role; Task 7 opens only authenticated registration submissions with a complete profile. Only active admins of the target fanpage may send absence alerts, without a global ADMIN bypass.
- Preserve `/api` routing, existing registration contracts, student-profile endpoint semantics, and fanpage approval through `PENDING`.
- Use PostgreSQL for constraint, concurrency, and commit/rollback tests; mocks cannot prove these properties.
- Do not delete conflicting records, reset databases, invent profile values, or relabel absence notifications to bypass constraints.
- Do not broaden the whole `/fanpage-admin/**` namespace to students or all authenticated users.

## Review focus

1. Two concurrent password-creation requests must produce one successful setup and one stable conflict without overwriting the winner: task 3.
2. Two profile requests racing on either user ID or staff ID must preserve names and existing profiles on the losing request: tasks 4–5.
3. Notifications must not be pushed when insert/commit fails; push failure after commit must not invalidate persisted notifications: task 2.
4. Admin pagination must preserve users with no profile and must not expose staff details through shared search DTOs: task 6.
5. JWT authorities can lag DB role changes; a refreshed token must authorize consistently while an old token must not be treated as updated: task 7.

## Delivery structure and alternatives

Recommended: ship small, independently reviewable changes in the order below. The notification fix is independent of account onboarding. Password status and admin visibility can ship without changing registration policy. Staff-profile creation can ship without granting new roles, but must not be presented as resolving registration eligibility by itself.

A single large rewrite would couple database repair, security changes, and FE contracts, making regressions harder to isolate. An endpoint-only patch would be smaller but leave races, pre-commit pushes, and onboarding authorization unresolved. Keep targeted refactoring within the flows being repaired.

| Delivery | Report coverage | Dependencies |
| --- | --- | --- |
| Notification schema and commit-safe delivery | C | Live constraint inspection; isolated PostgreSQL |
| Password state and atomic setup | A | Existing password/session contracts |
| Staff uniqueness and creation | B | Schema preflight; profile eligibility policy if restricted |
| Admin list and detail | D | Existing staff mapper; can proceed independently of creation |
| Registration eligibility and alert authorization | E | Confirmed open profile creation; B for onboarding |

## Task 1: Repair notification type constraints and document migration execution

**Files:**
- Create: `ddd-infrastructure/src/main/resources/database/migration/notification-absence-alert-type-migration.sql`
- Modify: `ddd-infrastructure/src/main/resources/database/schema.sql`
- Reference: `ddd-infrastructure/src/main/resources/database/migration/notification-type-refactor-migration.sql`
- Reference: `ddd-domain/src/main/java/com/ddd/domain/enums/NotificationTypeEnum.java`
- Create: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/migration/NotificationTypeMigrationTest.java`
- Create: `docs/guidelines/account-security-staff-profile-rollout.md`

**Interfaces:** Consume the deployed `notifications.type` schema and the complete current Java enum. Produce a checked `type` column that accepts every current enum value, rejects unknown values, and retains existing rows.

- [ ] Record baseline test results, then inspect the intended database/schema read-only: connection target without credentials, server schema, column names, `pg_get_constraintdef`, existing distinct types, invalid/null values, and migration history. Do not infer the deployed constraint from Java.
- [ ] Write PostgreSQL tests for a populated old constraint rejecting `EVENT_ABSENCE_ALERT`, migration preserving all rows, every current enum value inserting successfully, and `NOT_A_NOTIFICATION_TYPE` being rejected. Execute these against an isolated test database and confirm the new expectations fail before the migration.
- [ ] Implement a transactional migration that checks existing data, replaces the actual type CHECK constraint with the full valid set, and validates it. Abort on incompatible rows with an actionable diagnostic; do not discard rows. Define repeat execution as a safe no-op or an explicit detected already-applied state.
- [ ] Update fresh-install SQL so the final notification schema uses `type` and the same allowed values. Make the legacy title-to-type migration safely detect the already-renamed fresh schema. Document the complete fresh-install sequence, including existing fanpage and sequence migrations; do not assume `schema.sql` alone is already a complete installation.
- [ ] Document an explicit SQL application and verification procedure using the deployment's connection configuration. Current configuration uses `ddl-auto: update`; no Flyway/Liquibase dependency was found in the inspected Maven files. Do not introduce a migration framework as an incidental fix or claim SQL files run automatically.
- [ ] Run the PostgreSQL migration tests on both an upgraded populated schema and the documented fresh schema. Record row counts and constraint definitions before and after; commit this delivery independently.

## Task 2: Publish notifications after commit

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/notification/impl/NotificationCommandServiceImpl.java`
- Create: `ddd-application/src/main/java/com/ddd/application/service/notification/NotificationDispatchAfterCommit.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/notification/impl/NotificationCommandServiceImplTest.java`
- Create: `ddd-application/src/test/java/com/ddd/application/service/notification/impl/NotificationTransactionIntegrationTest.java`

**Interfaces:** Add `void dispatchAfterCommit(List<Notification> notifications)` to the new component, consuming the existing `NotificationPushPort`. Existing `createNotification(CreateNotificationDto)` and `createNotifications(List<CreateNotificationDto>)` signatures remain unchanged.

- [ ] Add failing tests asserting `pushCount == 0` before commit, `pushCount == recipientCount` after successful commit, and `pushCount == 0` after rollback or a commit-time constraint failure. Cover single and batch creation with real transaction boundaries.
- [ ] Register an after-commit synchronization from inside the active notification transaction; dispatch a captured immutable list of saved notification payloads. Require an active transaction instead of silently pushing immediately. Do not depend on lazy database access after commit.
- [ ] Route both creation methods through the component. Keep their existing `REQUIRES_NEW` behavior for this fix and document that notification transactions remain independent of callers. Batch writes remain atomic within that transaction.
- [ ] Catch and log delivery failures per recipient after commit, continue remaining recipients, and retain committed rows. This preserves best-effort SSE semantics; durable delivery/outbox and automatic retries are outside this change.
- [ ] Test one recipient's push failure while later recipients still receive pushes; verify all rows remain persisted. Run unit and PostgreSQL integration tests, then commit.

## Task 3: Expose password status and make initial setup atomic

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/UserController.java`
- Create: `ddd-api/src/main/java/com/ddd/api/dto/user/res/AccountSecurityResponseDto.java`
- Create: `ddd-application/src/main/java/com/ddd/application/service/user/AccountSecurityQueryService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/user/impl/UserCommandServiceImpl.java`
- Modify: existing domain user repository, infrastructure implementation, and JPA repository resolved through CodeGraph at execution time
- Modify: existing exception-code registry and exception handler resolved through CodeGraph at execution time
- Test: `ddd-application/src/test/java/com/ddd/application/service/user/impl/UserCommandServiceImplTest.java`
- Create: `ddd-api/src/test/java/com/ddd/api/controller/UserSecurityControllerTest.java`
- Create: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/repository/PasswordCreationConcurrencyTest.java`

**Interfaces:** `AccountSecurityResponseDto(boolean hasPassword)`; `boolean AccountSecurityQueryService.hasPassword()`; `GET /api/users/me/security` returns `BaseResponse<AccountSecurityResponseDto>`. Add repository `boolean setPasswordIfAbsent(Long userId, String encodedPassword)`, returning true only when a row with null/blank password was changed.

- [ ] Write failing status tests for local users, Google users before/after password creation, Google login into an account with a password, every authenticated role, missing user, and unauthenticated access. Assert response data contains only `hasPassword`.
- [ ] Implement the read service using the authenticated user ID and current database state. Return 401 without authentication and the existing user-not-found response when the authenticated ID no longer exists.
- [ ] Implement initial setup with an atomic conditional update matching Java's null/blank semantics. Inspect actual stored password formats during preflight; do not overwrite legacy nonblank placeholders without a separate remediation decision. Ensure the conditional operation and refresh-token replacement participate in the existing transaction.
- [ ] Map an already-set password to a stable conflict code, proposed `PASSWORD_ALREADY_SET`; explicitly return `PASSWORD_NOT_SET` for change attempts without a stored password. Preserve current-password verification and existing session behavior. Record actual status/code pairs in the rollout document.
- [ ] Add a synchronized PostgreSQL race test: two different candidate passwords, exactly one setup succeeds, the loser receives the documented conflict, and only the winner matches the stored hash. Also test sequential repeat, whitespace-only legacy data, incorrect old password, and session-operation rollback.
- [ ] Run controller, service, repository concurrency, and existing auth tests; commit. No password-status field is added to public user responses.

## Task 4: Enforce staff-profile uniqueness without data loss

**Files:**
- Create: `ddd-infrastructure/src/main/resources/database/migration/staff-profile-uniqueness-migration.sql`
- Modify: `ddd-infrastructure/src/main/resources/database/schema.sql`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/FanpageAdminProfileJpaEntity.java`
- Create: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/migration/StaffProfileUniquenessMigrationTest.java`
- Modify: rollout document from task 1

**Interfaces:** Named unique constraints `uk_staff_profile_user_id` and `uk_staff_profile_staff_id`, or an explicitly documented reuse of an equivalent existing user constraint. Error translation must distinguish the two constraints.

- [ ] Add failing migration tests with duplicate user IDs, duplicate staff IDs, valid populated data, and an already-existing equivalent user constraint.
- [ ] Preflight existing values and collisions after trimming. Use trimmed, case-sensitive staff IDs for this plan; confirm that institutional identifiers do not require case-insensitive uniqueness before applying the migration. Do not silently merge identifiers or delete duplicate profiles.
- [ ] Add the missing staff-ID uniqueness constraint and preserve user uniqueness in deployed and fresh schemas. Canonicalize existing whitespace only after a collision-free preflight and through an explicit transactional update. Align JPA metadata with those constraints.
- [ ] Keep DOB nullable; the inspected profile entity/schema already allow it. Inspect user gender nullability before introducing request validation rather than inventing a gender default.
- [ ] Verify existing rows and nullable fields survive migration, duplicate inserts fail on the expected named constraints, and repeated deployment detects the applied state; commit independently.

## Task 5: Create a staff profile for the authenticated user

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/UserController.java`
- Create: `ddd-api/src/main/java/com/ddd/api/dto/user/req/StaffProfileCreateRequestDto.java`
- Create: `ddd-api/src/main/java/com/ddd/api/dto/user/req/StaffProfileDetailsCreateRequestDto.java`
- Create: `ddd-application/src/main/java/com/ddd/application/dto/user/StaffProfileCreateDto.java`
- Create: `ddd-application/src/main/java/com/ddd/application/dto/user/StaffProfileDetailsCreateDto.java`
- Create: `ddd-application/src/main/java/com/ddd/application/service/userprofile/StaffProfileCreationService.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/mapper/StaffProfileApiMapper.java`
- Reference: existing staff summary mapper and JPA user/profile repositories
- Create: `ddd-application/src/test/java/com/ddd/application/service/userprofile/StaffProfileCreationServiceTest.java`
- Create: `ddd-api/src/test/java/com/ddd/api/controller/StaffProfileControllerTest.java`
- Create: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/repository/StaffProfileCreationConcurrencyTest.java`

**Interfaces:** `StaffProfileSummaryDto StaffProfileCreationService.createForCurrentUser(StaffProfileCreateDto dto)`; `POST /api/users/me/staff-profile` returns HTTP 201 and existing `StaffProfileResponseDto` in `BaseResponse`. Request shape follows report B.2, with optional `gender` at the user level and optional DOB/phone inside `staffProfile`.

- [ ] Write failing tests for an authenticated Google user with no password, an existing student adding a staff profile, no student profile, existing staff profile, duplicate staff ID, invalid fields, and unauthenticated access. Assert email, provider ID, password, avatar, roles, and student profile remain unchanged.
- [ ] Validate trimmed required names (100 each), staff ID (20), organization (255), title (100), optional phone (20), optional ISO DOB not in the future, and optional exact gender enum `MALE`/`FEMALE`/`OTHER`. Use dedicated creation DTOs; do not reuse partial PATCH DTOs. Bound unknown client role/user-target fields through the project's request-handling conventions and prove they cannot affect identity or privileges.
- [ ] Implement one transaction covering allowed user-name/gender changes and profile insert. Determine the user only from authentication. Use database constraints from task 4 as the final concurrency protection, with prechecks only for helpful errors.
- [ ] Translate constraint failures outside the rolled-back transaction into HTTP 409 with distinct stable codes, proposed `STAFF_PROFILE_ALREADY_EXISTS` and `STAFF_ID_ALREADY_EXISTS`. Do not catch a failed insert and continue using a rollback-only transaction. Return field validation errors as 400 and ineligibility as 403 if an approved policy restricts profile creation.
- [ ] Allow every authenticated user to create their first profile without verification or role grants. Leave PATCH update-only and staff ID/organization read-only after creation.
- [ ] Add PostgreSQL races for the same user and the same staff ID across two users. Assert one success, one conflict, no partial name changes on the loser, and GET immediately returns the winner's real profile. Run registration and PATCH regression tests; commit.

## Task 6: Add admin staff-profile reads without widening public responses

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/UserController.java`
- Create: `ddd-api/src/main/java/com/ddd/api/dto/user/res/AdminUserResponseDto.java`
- Create: `ddd-application/src/main/java/com/ddd/application/dto/user/AdminUserSummaryDto.java`
- Create: `ddd-application/src/main/java/com/ddd/application/service/user/AdminUserQueryService.java`
- Create: `ddd-api/src/main/java/com/ddd/api/mapper/AdminUserApiMapper.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/UserJpaRepository.java`
- Test: existing staff mapper tests
- Create: `ddd-api/src/test/java/com/ddd/api/controller/AdminUserControllerTest.java`
- Create: `ddd-application/src/test/java/com/ddd/application/service/user/AdminUserQueryServiceTest.java`

**Interfaces:** `Page<AdminUserSummaryDto> getAllUsers(boolean isActive, int page, int size, String sortBy, String sortDir)` and `StaffProfileSummaryDto getStaffProfileByUserId(Long userId)` in the new service. `GET /api/users/admin` retains existing fields, including `providerId`, adding nullable `staffProfile`; `GET /api/users/admin/{userId}/staff-profile` returns the existing staff response shape.

- [ ] Write failing tests for ADMIN access, 401 without authentication, 403 for STUDENT/FANPAGE_ADMIN, a user without a staff profile (200/null), missing user (404), and mismatched user/profile numeric IDs.
- [ ] Read the admin user page with an entity graph for the existing one-to-one `fanpageAdminProfile` association, preserving users without profiles. Fetch roles for the page separately to avoid collection-fetch pagination and N+1 queries; map inside the read transaction. Retain totals, order, and active filtering.
- [ ] Map through admin-specific DTOs. Leave `UserResponseDto` and search response mapping unchanged to prevent disclosure of staff DOB/phone/organization outside the admin API.
- [ ] Fetch admin detail through a dedicated user-ID query with `@EntityGraph(attributePaths = {"roles", "fanpageAdminProfile"})`, then reuse the existing staff mapper with the user's fetched association. Leave shared lookup graphs unchanged; do not copy the student endpoint's user-ID/profile-ID fallback.
- [ ] Test mixed-profile pages, inactive users, empty pages, stable totals, bounded query counts across page sizes, search responses omitting staff fields, and updated self-profile values appearing on the next admin read. Run admin security tests with the actual security filter configuration; commit.

## Task 7: Open registration submissions and restrict absence alerts

**Detailed plan:** [Task 7 implementation](2026-11-23-fanpage-registration-eligibility-implementation.md). The user's confirmed policy supersedes earlier staff-verification assumptions.

- [ ] Permit every authenticated user to create their first fanpage-admin profile through Task 5, without staff verification, approval, password, or existing FANPAGE_ADMIN role. Preserve existing roles.
- [ ] Add the authenticated eligibility read with `PROFILE_REQUIRED`/`ELIGIBLE`, stable reasons, and `requiresReauthentication: false` for profile completion. Share the current-user profile completeness check with registration; add no staff-review lifecycle.
- [ ] Add an exact authenticated POST exception for `/api/fanpages/fanpage-admin/register` before the broader role/public matchers. Keep required profile/request validation, PENDING fanpages, and existing membership creation. Retain all neighboring management-route restrictions; profile creation grants no global roles.
- [ ] Require `isAdminOfFanpage(fanpageId, currentUserId)` for absence alerts. It checks active target-fanpage ADMIN membership. Remove ordinary-member access and the unconditional system ADMIN bypass; global roles alone are insufficient.
- [ ] Test Google/local accounts, every role, profile conflicts, unchanged token usability for registration, preserved profiles/roles, exact matcher scope, and the complete alert authorization matrix. Document the confirmed policy and test results.

## Task 8: Validate the release and hand off FE contracts

**Files:** rollout document; source report remains unchanged unless separately requested.

- [ ] Run focused tests while developing each task, first confirming new tests fail for the intended behavior and then pass. Use `mvn -pl ddd-application -am -Dtest=UserCommandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` as the existing-service example; substitute each owning module/test class. Run new PostgreSQL tests through their configured test datasource and real commit boundaries, not transaction-rollback-only test fixtures.
- [ ] Run `mvn test` after completed deliveries. Record environment/dependency failures distinctly from regressions. Verify integration tests actually executed; skipped DB tests are not proof of atomicity or migration safety.
- [ ] In an isolated environment, execute the documented migrations and smoke-test password status/setup, Google staff-profile creation, admin list/detail, and absence-alert persistence/delivery. Exercise policy-dependent registration only after task 7 is decided.
- [ ] Publish real request/response examples with `/api` paths, `BaseResponse`, exact status/error codes, field nullability, POST 201 behavior, pagination, and token-refresh instructions. Mark examples as examples; avoid assuming test IDs from the report exist.
- [ ] Give FE its integration checklist: replace `/auth/staff-profile`, map confirmed form fields without truncation/defaults, distinguish loading/error from `hasPassword: false`, invalidate per-account caches, use admin staff fields, and map `/fanpages/{id}` notification links to `/fanpage/{id}`.
- [ ] Record deployment order: compatible schema repairs first, backend contracts next, FE consumers after verification. Gate only policy-dependent work; do not delay the independent notification repair or password-status endpoint.
- [ ] Record rollback limits: application rollback retains profile/notification data and additive constraints; do not restore a CHECK excluding a notification type once rows of that type exist. Treat failed SSE delivery after commit as a delivery issue, not a reason to remove saved notifications.

## Completion evidence

Each delivery includes its commit, test commands/results, contract examples, and any migration execution evidence. The release is complete when sections A–D pass their acceptance checks and section E either passes the approved policy matrix or is explicitly reported as a remaining policy-dependent delivery. Do not describe the whole issue as resolved while Google onboarding still ends at a predictable registration 403.
