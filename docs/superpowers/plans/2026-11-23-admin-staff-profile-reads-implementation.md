# Admin Staff Profile Reads Implementation Plan

> **For agentic workers:** Use the repository's `executing-plans` skill when implementing this plan. Track the deliverables below without splitting them into individual coding actions.

**Goal:** Let system administrators see real staff-profile data in the user list and read an individual user's staff profile without changing public/search responses.

**Architecture:** Introduce an admin-specific query service, summary DTO, and API mapper. Page users before fetching associated data in batches, and reuse the existing staff mapper for consistent self/admin profile responses.

**Tech Stack:** Existing Spring Security, read-only transactions, JPA, PostgreSQL, MapStruct, Maven, JUnit, and Mockito. No new runtime dependencies or schema migration.

**Spec:** [Issue report, D](../../bug/backend-account-security-and-staff-profile-request.md) and [parent plan, Task 6](2026-11-23-account-security-staff-profile-refactor.md).

## Contract and boundaries

Extend `GET /api/users/admin` with nullable `staffProfile` on each user, retaining the current `BaseResponse`/page envelope and every existing account field: `userId`, `email`, `roles`, `role`, `firstName`, `lastName`, `gender`, `isActive`, `providerId`, and `avatarUrl`. Preserve the existing singular-role derivation rather than choosing a different role when creating the new mapper.

Add `GET /api/users/admin/{userId}/staff-profile`, returning HTTP 200 with `BaseResponse<StaffProfileResponseDto>`. Resolve the path ID exclusively against `users.id`. An existing user without a staff profile returns real account information with `staffProfile: null`; a missing user returns 404. Never fall back to looking up a profile by its own ID.

Staff details use the existing names and types: `staffId`, `orgName`, `title`, `dateOfBirth`, and `phoneNumber`. Preserve null values and never synthesize a profile from roles. A user may have both student and staff profiles.

Both admin routes require system `ADMIN`: unauthenticated access returns 401 and authenticated users without ADMIN return 403. This grants read access only. Keep student admin profile routes, self-service GET/PATCH, search responses, and write permissions unchanged. Task 6 can ship independently of Task 5 because existing staff records already provide the data.

## Files

- Modify `ddd-api/src/main/java/com/ddd/api/controller/UserController.java`.
- Create `ddd-api/src/main/java/com/ddd/api/dto/user/res/AdminUserResponseDto.java` and `ddd-api/src/main/java/com/ddd/api/mapper/AdminUserApiMapper.java`.
- Create `ddd-application/src/main/java/com/ddd/application/dto/user/AdminUserSummaryDto.java` and `ddd-application/src/main/java/com/ddd/application/service/user/AdminUserQueryService.java`.
- Modify `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/UserJpaRepository.java` and `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/FanpageAdminProfileJpaRepository.java` for dedicated paginated and batch reads.
- Reuse `StaffProfileDtoMapper`, `StaffProfileApiMapper`, and existing user-field mapping. Leave shared `UserResponseDto` unchanged.
- Create `AdminUserControllerTest.java` under API controller tests, plus `AdminUserQueryServiceTest.java` and `AdminUserQueryIntegrationTest.java` under application user-service tests. Extend existing staff mapper tests where needed.
- Update `docs/guidelines/account-security-staff-profile-rollout.md` with response examples and FE integration notes.

## Implementation

- [ ] **Add the admin read model and service.** Expose `Page<AdminUserSummaryDto> getAllUsers(boolean isActive, int page, int size, String sortBy, String sortDir)` and `StaffProfileSummaryDto getStaffProfileByUserId(Long userId)`. Build admin list data from the current account fields plus nullable `StaffProfileDetailsDto`. Map inside a read-only transaction so serialization does not trigger lazy persistence queries. Do not return entities or authentication data.

- [ ] **Fetch pages without N+1 queries or collection-pagination errors.** Use a dedicated query to page users without fetching the roles collection. Fetch roles for that page's user IDs in a separate query and fetch staff profiles in one batch; assemble results in the original page order. Keep the existing active filter, page/size defaults, sorting behavior, total elements, and page metadata. Add a stable user-ID tie-breaker where needed for equal sort values. Empty pages must not issue invalid empty-IN queries. Do not use the existing roles-fetch entity graph for the paginated read if it results in collection-fetch pagination or in-memory limits.

- [ ] **Implement user-ID-based detail reads.** Load the target user, return 404 when missing, and retrieve the optional staff profile by `user_id`. Use the existing staff summary/response mappers, including `dob` to `dateOfBirth`, to keep self/admin detail consistent. Include inactive users/profiles according to the existing self-profile behavior; do not add a hidden profile-active filter that turns real data into null. Do not reinterpret the existing student endpoint or copy its profile-ID fallback.

- [ ] **Wire protected routes and isolate response mapping.** Switch only the admin user list to the new mapper and add the detail route under the existing ADMIN namespace. Verify protection using the actual security filter configuration and API prefix. Keep `/me` scoped to the authenticated user and leave shared user/search mapping untouched. The additional DOB/phone/organization fields must appear only in the intended authorized staff responses, never incidentally in search results.

## Verification

- [ ] **Test contracts and authorization.** Cover ADMIN success, anonymous 401, and STUDENT/FANPAGE_ADMIN 403 on both routes. Test full and missing profiles, nonexistent users, inactive accounts, multiple roles, dual student/staff profiles, and deliberately different user/profile IDs. Assert every existing list field remains and null optional fields retain their existing representation. Verify the student and self-service routes still behave as before and search data contains no new staff-profile fields.

- [ ] **Verify pagination and query behavior with PostgreSQL.** Seed mixed users with zero, one, and multiple roles, with/without staff profiles. Check ordering, full/partial/empty pages, active/inactive filtering, total counts, no duplicate/missing users, and unchanged page membership when profiles are added. Compare SQL counts for different page sizes using a fresh persistence context and disabled query-cache effects: queries should remain bounded by the page/count/roles/profile reads, rather than grow per user. Fail on collection-fetch pagination warnings or configure the integration test to reject that behavior.

- [ ] **Verify freshness and hand off.** Update a profile through existing self-service PATCH, then read it through self GET, admin detail, and admin list in fresh requests; values must agree after commit. Run controller, service, mapper, PostgreSQL, and existing user-profile regressions, recording any incomplete DB checks explicitly. Document the final list/detail examples, 200/null versus 404 distinction, authorization, and FE usage: render staff details directly from the list and fetch detail when opening a user, without one detail request per table row. Check the diff for unrelated changes.

## Done when

ADMIN can list and inspect staff data for the correct users, including accounts without staff profiles. Pagination remains correct with bounded queries, updated profile values appear on subsequent reads, and shared search responses and existing write privileges remain unchanged. No profile creation or role changes are part of this task.
