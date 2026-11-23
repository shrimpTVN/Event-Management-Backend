# Staff Profile Creation Implementation Plan

> **For agentic workers:** Use the repository's `executing-plans` skill when implementing this plan. Track the deliverables below without splitting them into individual coding actions.

**Goal:** Allow an authenticated existing account, including a Google account without a password, to create its staff profile without registering another user.

**Architecture:** Add dedicated creation DTOs and a focused staff-profile creation service. Persist permitted user changes and the staff profile in one transaction, reuse existing staff response mapping, and rely on Task 4's database constraints for concurrent-write safety.

**Tech Stack:** Existing Spring Security, validation, transactions, JPA, PostgreSQL, MapStruct, JUnit, and Mockito. No new runtime dependencies.

**Spec:** [Issue report, B](../../bug/backend-account-security-and-staff-profile-request.md), [parent plan, Task 5](2026-11-23-account-security-staff-profile-refactor.md), and [Task 4 uniqueness contract](2026-11-23-staff-profile-uniqueness-implementation.md).

## Contract and boundaries

Add `POST /api/users/me/staff-profile`. On success return HTTP 201 with `BaseResponse<StaffProfileResponseDto>`, using the same data structure as the existing GET/PATCH endpoints. A subsequent GET must return the persisted profile.

The request contains required `firstName`, `lastName`, and nested `staffProfile` with required `staffId`, `orgName`, and `title`. Optional fields are user-level `gender` and nested `dateOfBirth`/`phoneNumber`. Do not reuse the PATCH DTO, whose presence tracking and optional fields serve different semantics.

Determine the target account exclusively from authentication. The request has no accepted target user ID, email, password, avatar, or roles. Preserve Google linkage, password, email, avatar, all existing roles, and the student profile. Omitted or null optional gender preserves the existing user gender; omitted/null profile DOB and phone remain null. Never populate sample values.

Profile creation does not grant fanpage registration permission or any system role. Do not require `FANPAGE_ADMIN`, a password, or a student profile merely to enter onboarding. Before exposing the endpoint, confirm whether authenticated users may self-declare an unverified staff profile or need an existing server-side eligibility check. Record any restriction explicitly and return 403 when it applies; do not invent verification from email patterns. Fanpage permission policy remains Task 7.

Task 4's verified unique constraints and agreed staff-ID normalization are deployment prerequisites. Existing registration and PATCH routes retain their contracts; PATCH remains update-only, with staff ID and organization read-only after creation.

## Files

- Modify `ddd-api/src/main/java/com/ddd/api/controller/UserController.java` and `ddd-api/src/main/java/com/ddd/api/mapper/StaffProfileApiMapper.java`.
- Create `StaffProfileCreateRequestDto.java` and `StaffProfileDetailsCreateRequestDto.java` under `ddd-api/src/main/java/com/ddd/api/dto/user/req/`.
- Create `StaffProfileCreateDto.java` and `StaffProfileDetailsCreateDto.java` under `ddd-application/src/main/java/com/ddd/application/dto/user/`.
- Create `ddd-application/src/main/java/com/ddd/application/service/userprofile/StaffProfileCreationService.java`; reuse the existing JPA repositories and `StaffProfileDtoMapper`.
- Add dedicated profile-conflict exceptions under `ddd-domain/src/main/java/com/ddd/domain/exception/`; update the API error enum/handler for this flow without remapping unrelated integrity errors.
- Add `StaffProfileCreationServiceTest.java` under application userprofile tests and `StaffProfileControllerTest.java` under API controller tests.
- Add `StaffProfileCreationIntegrationTest.java` under application userprofile tests for real transaction/race coverage. This refines the parent plan's infrastructure-only concurrency test location because proving user-name rollback requires the application service as well as persistence.
- Update `docs/guidelines/account-security-staff-profile-rollout.md` with the creation contract and eligibility assumptions.

## Implementation

- [ ] **Add creation DTOs and validation.** Trim required text and reject missing/blank values. Enforce first/last names at 100 characters each, staff ID at 20, organization at 255, title at 100, and optional phone at 20. Validate lengths on normalized values, without truncation. Keep staff IDs case-sensitive and normalize consistently with Task 4. Require ISO `YYYY-MM-DD` for supplied DOB and reject future dates. Accept only `MALE`, `FEMALE`, or `OTHER` for supplied gender. Normalize an empty optional phone to null. Preserve the project's field-error response structure and prove unexpected client identity/role fields cannot alter protected data.

- [ ] **Implement atomic creation.** Expose `StaffProfileSummaryDto createForCurrentUser(StaffProfileCreateDto dto)`. Resolve the current user, apply any approved eligibility guard, and check for an existing profile before changing names. Update only the permitted user fields and insert the profile in the same transaction. Map the saved entities through the existing summary mapper, including `dob` to `dateOfBirth`. Use a real transaction boundary; do not introduce a separate commit for user changes or profile insertion. Return success only after commit completes.

- [ ] **Handle duplicates and failures consistently.** Return 409/`STAFF_PROFILE_ALREADY_EXISTS` for an existing user profile and 409/`STAFF_ID_ALREADY_EXISTS` for a staff ID owned by another profile, including inactive profiles. Prechecks improve messages but do not replace unique constraints. Translate actual constraint violations after the creation transaction has rolled back, using the precise constraint names recorded in Task 4 and structured database exception details. Let exceptions escape the transactional service to the API boundary for translation; do not catch a failed insert and continue inside a rollback-only transaction. Preserve unrelated database failures as server errors rather than labelling them duplicates. Invalid requests return 400, unauthenticated requests 401, and missing accounts follow the existing not-found convention.

- [ ] **Wire the endpoint and preserve neighboring flows.** Map POST to the creation service and the existing staff response DTO, returning 201. Keep GET/PATCH behavior intact, keep the new-account `/auth/register/fanpage-admin` path separate, and do not implement the FE's nonexistent `/auth/staff-profile` route as an alternate identity flow. No token replacement is needed because creation grants no roles; document that registration eligibility may still require Task 7.

## Verification

- [ ] **Cover successful creation and request boundaries.** Test Google users without passwords, local accounts, users with no student profile, and users retaining both profile types. Verify GET after POST, persistence after a new session, allowed name/gender updates, unchanged protected fields, optional nulls, exact validation limits, whitespace-only fields, invalid enums/dates, and attempts to target another account or submit roles.

- [ ] **Prove rollback and races with PostgreSQL.** Race requests for the same user and requests from different users sharing one staff ID. Assert exactly one creation succeeds, the loser receives the correct conflict, and the losing request changes no names/gender or existing profiles. Induce a persistence/commit failure after user fields change and verify all changes roll back. Exercise actual Spring-proxied services and inspect state through a fresh transaction; mocks cannot prove these guarantees.

- [ ] **Run regressions and document FE integration.** Run controller, creation-service, mapper, PostgreSQL integration, existing registration, and PATCH tests. Record any unexecuted DB checks as incomplete. Document the exact 201 envelope, null semantics, validation errors, 409 codes, and approved eligibility rule. FE should replace `/auth/staff-profile` with this POST, retain its separate email-registration branch, refresh profile caches after success, and never treat a saved staff profile as proof of fanpage privileges.

## Done when

An eligible authenticated existing account creates exactly one persistent staff profile and immediately reads it through GET. Duplicate, invalid, or failed requests leave account/profile data unchanged; protected identity and roles are preserved. The contract supports Google onboarding without requiring a password or creating another user, while fanpage authorization remains explicitly separate.
