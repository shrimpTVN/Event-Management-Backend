# Password Security Implementation Plan

> **For agentic workers:** Use the repository's `executing-plans` skill when implementing this plan. Track the deliverables below without splitting them into individual coding actions.

**Goal:** Expose the authenticated account's password status and ensure concurrent initial password creation cannot overwrite an existing password.

**Architecture:** Add a focused account-security query service and response DTO. Keep password writes within the existing auth transaction, enforce initial-setup exclusivity in the repository, and map password-state failures through the existing API error envelope.

**Tech Stack:** Existing Spring Security, Spring transactions, JPA, PostgreSQL, password encoder, JUnit, and Mockito. No new runtime dependencies or database columns.

**Spec:** [Issue report, A](../../bug/backend-account-security-and-staff-profile-request.md) and [parent plan, Task 3](2026-11-23-account-security-staff-profile-refactor.md).

## Contract and constraints

`GET /api/users/me/security` returns HTTP 200 with `BaseResponse<AccountSecurityResponseDto>`, whose data is `{ "hasPassword": boolean }`. Determine the user from authentication, with no target email or user ID parameter. Permit every authenticated role without requiring a student or staff profile. Return 401 without valid authentication and the existing user-not-found response if the authenticated account no longer exists.

Define password presence consistently as `password != null && !password.isBlank()`. A Google account can have a password; provider and role are not evidence of password state. Return current database state and prevent HTTP caching of this security response with `Cache-Control: no-store`. Never expose the stored password/hash or add status fields to public search/list DTOs.

Keep existing password validation, hashing, and session behavior: initial setup revokes existing refresh sessions and creates the replacement refresh cookie; password change verifies the current password and revokes refresh sessions. Do not claim that revocation immediately invalidates existing access JWTs. Preserve account identity, profiles, Google linkage, roles, and unrelated fields.

## Files

- Modify `ddd-api/src/main/java/com/ddd/api/controller/UserController.java`.
- Create `ddd-api/src/main/java/com/ddd/api/dto/user/res/AccountSecurityResponseDto.java` and `ddd-application/src/main/java/com/ddd/application/service/user/AccountSecurityQueryService.java`.
- Modify `ddd-application/src/main/java/com/ddd/application/service/user/impl/UserCommandServiceImpl.java`.
- Modify `ddd-domain/src/main/java/com/ddd/domain/repository/UserRepository.java`, `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/UserRepositoryImpl.java`, and `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/UserJpaRepository.java`.
- Add dedicated password-state exceptions under `ddd-domain/src/main/java/com/ddd/domain/exception/`; extend `ddd-api/src/main/java/com/ddd/api/common/enums/ErrorCodeEnum.java` and `ddd-api/src/main/java/com/ddd/api/exception/GlobalExceptionHandler.java`.
- Update existing user-command and auth-service tests. Add `UserSecurityControllerTest.java` under the API controller tests, `AccountSecurityQueryServiceTest.java` under application user-service tests, and `PasswordCreationConcurrencyTest.java` under infrastructure repository tests.
- Add an auth transaction integration test under application service tests for password/refresh-session rollback. Update the rollout document with the final contract.

## Implementation

- [ ] **Add the password-status endpoint.** Implement `boolean AccountSecurityQueryService.hasPassword()` using the authenticated user ID and existing user repository. Map it to `AccountSecurityResponseDto(boolean hasPassword)` through `BaseResponse`; enforce authenticated access and the no-store header. Read the latest persisted value without a cross-account application cache.

- [ ] **Make initial setup atomic.** Add `boolean UserRepository.setPasswordIfAbsent(Long userId, String encodedPassword)`. Implement it by locking the user row, checking its current password with Java `isBlank()`, and setting only the password when absent. Use a dedicated JPA `PESSIMISTIC_WRITE` lookup without collection-fetch joins; require the existing outer write transaction so the lock remains held through password and refresh-session updates. This avoids differences between Java whitespace handling and SQL `TRIM`. Recheck under the lock even if the service already checked before encoding. Return false for an already-set password and retain user-not-found handling for a missing row. Do not use the existing whole-user save path for this write.

- [ ] **Wire stable state errors.** Map failed initial setup to HTTP 409 with `exceptionCode: PASSWORD_ALREADY_SET`; map change-password on an account without a password to HTTP 409 with `exceptionCode: PASSWORD_NOT_SET`. Use dedicated exceptions/handlers rather than changing all `BusinessException` responses or parsing message text. Keep the existing wrong-current-password behavior. Inspect legacy nonblank OAuth placeholders separately; do not erase or reinterpret them automatically.

- [ ] **Preserve transaction and session consistency.** Keep password setup and refresh-session revocation/replacement in the existing auth transaction, with no independent repository commit. A losing creation request must not revoke or replace sessions. If token persistence fails, roll back the password write and session changes together. Keep change-password verification active regardless of the status previously shown by FE.

## Verification

- [ ] **Test reads, access, and errors.** Cover local accounts, Google accounts without passwords, Google accounts with passwords, and reads after creation/change. Include null, empty, spaces, tabs/newlines, and representative Unicode whitespace matching Java semantics. Verify every authenticated role works without a profile, unauthenticated access returns 401, missing accounts follow the existing contract, response data contains only the boolean, and error codes/statuses match the documented contract.

- [ ] **Prove exclusivity and rollback with PostgreSQL.** Race two transactions setting different passwords for one account: exactly one succeeds, the loser receives the stable conflict, and only the winner matches the stored hash. Verify repeat setup cannot overwrite, unrelated user fields remain unchanged, and an auth transaction failing during refresh-token persistence leaves password and sessions unchanged. Exercise real Spring proxies/transactions; mocks cannot prove locking or rollback.

- [ ] **Run regressions and hand off the contract.** Run relevant controller, query-service, user-command, auth, and PostgreSQL tests; record incomplete database verification explicitly. Document the final envelope, 409 codes, cookie/session behavior, and FE flow: read status on opening security settings, show loading/errors separately from false, reread after setup or a state conflict, clear account-specific caches on logout, and use the existing login-again flow after password change. Check the diff for unrelated changes.

## Done when

The endpoint reports current password state for any authenticated account without exposing credentials. Concurrent setup yields one winner without overwriting its password or disrupting its sessions, rollback preserves password/session consistency, and existing password-change verification continues to work. No FE implementation or database schema migration is part of this task.
