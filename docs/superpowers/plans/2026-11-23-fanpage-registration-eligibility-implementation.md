# Fanpage Registration Eligibility Implementation Plan

> **For agentic workers:** Use the repository's `executing-plans` skill when implementing this plan. Track the deliverables below without splitting them into individual coding actions. The user-confirmed policy below supersedes verification assumptions in the original report and earlier plan.

**Goal:** Let authenticated accounts complete a fanpage-admin profile without verification, submit a fanpage registration with the required profile, and restrict absence alerts to administrators of the target fanpage.

**Architecture:** Use a shared profile-completeness check for registration status and submission. Allow authenticated access to the exact registration POST while retaining the existing restrictions on other management endpoints. Authorize absence alerts using current active fanpage-admin membership, independently of profile existence or global role claims.

**Tech Stack:** Existing Spring Security, transactions, JPA, PostgreSQL, JWT, Maven, JUnit, and Mockito. No new runtime dependencies or verification subsystem.

**Spec:** User clarification: every authenticated user may create a fanpage-admin profile once, with no staff verification; only fanpage admins may send absence alerts. Also see [issue report, E](../../bug/backend-account-security-and-staff-profile-request.md), [parent plan](2026-11-23-account-security-staff-profile-refactor.md), and [Task 5](2026-11-23-staff-profile-creation-implementation.md).

## Confirmed policy and boundaries

Every authenticated account, including a Google account without a password and an account with STUDENT or ADMIN roles, may create its own first fanpage-admin profile. Keep Task 5's validation, unique staff ID, and one-profile-per-user constraints. No staff verification, approval queue, institutional email inference, or FANPAGE_ADMIN role prerequisite applies to creating that profile.

For registration, use the completed profile and existing fanpage request validation as the submission conditions. Preserve the current required staff ID/organization; a student profile alone does not replace those requirements in this delivery. Every user can access the same profile-completion path, and ADMIN-only users follow it too. Do not invent profile values to bypass validation.

Keep `POST /api/fanpages/fanpage-admin/register`, its success envelope, and creation of PENDING fanpages for superadmin review. Profile creation and permission to submit do not grant a global system role or management access to every fanpage. The newly registered fanpage retains the existing membership creation behavior; management-route role and per-fanpage checks remain unchanged in this task.

Only an active member with fanpage role ADMIN on the target fanpage may send absence alerts. A profile, system FANPAGE_ADMIN role, ordinary membership, or admin membership in another fanpage is insufficient. Remove the current unconditional system ADMIN bypass: a superadmin may send only if also an active administrator of that target fanpage. Do not change statistics-reading permissions as part of this correction.

## Contract

Add authenticated `GET /api/users/me/fanpage-registration-eligibility` returning `BaseResponse<FanpageRegistrationEligibilityResponseDto>` with `status`, `reasonCode`, and `requiresReauthentication`.

Use only `PROFILE_REQUIRED` and `ELIGIBLE` for this delivery: incomplete/missing required profile yields `PROFILE_REQUIRED` with a stable reason; a complete profile yields `ELIGIBLE`. Return 200 for these authenticated status reads, 401 without authentication, and the existing not-found response for a missing account. There is no staff-review state, so do not add `PENDING_REVIEW` or a verification table. Fanpage PENDING approval is a separate existing lifecycle.

Return `requiresReauthentication: false` for profile completion because no role changes are required for the exact registration route. Expired/invalid sessions continue to use existing authentication errors and refresh/login behavior. Reevaluate profile completeness during POST; frontend status is not the write authorization gate.

## Files

- Create `FanpageRegistrationEligibilityService.java` under `ddd-application/src/main/java/com/ddd/application/service/fanpage/` and `FanpageRegistrationEligibilityDto.java` under application fanpage DTOs.
- Create `FanpageRegistrationEligibilityResponseDto.java` under API user response DTOs; modify `ddd-api/src/main/java/com/ddd/api/controller/UserController.java`.
- Modify `ddd-application/src/main/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImpl.java` and `FanpageStatisticCommandServiceImpl.java` in the same directory.
- Modify `ddd-infrastructure/src/main/java/com/ddd/infrastructure/config/security/SecurityConfig.java` and `ddd-infrastructure/src/main/java/com/ddd/infrastructure/constant/SecurityConstant.java` for the exact registration exception.
- Reuse `FanpageMemberRepository.isAdminOfFanpage(fanpageId, userId)`, which already checks active membership and the target fanpage's ADMIN role.
- Add eligibility-service and API full-filter registration tests; extend existing auth, fanpage command, and absence-alert tests. Update `docs/guidelines/account-security-staff-profile-rollout.md`.

## Implementation

- [x] **Unblock onboarding without verification.** Keep Google newcomers at their existing base role and preserve roles/profiles for existing Google accounts. Apply Task 5's all-authenticated-users creation rule and do not introduce verification or role grants. Trace any reported 403 through the actual method/path, matcher, and token authority summaries without logging tokens or assuming historical user ID 2 is a universal fixture.

- [x] **Share profile eligibility checks.** Expose `FanpageRegistrationEligibilityDto evaluateCurrentUser()` and `void assertCurrentUserCanRegister()`. Resolve current account/profile state from authentication and the database; require the existing staff ID/organization completeness conditions without checking a verifier or applicant role. Use the same checks in the status read and registration command. Retain missing-profile `USER.002` behavior and separate it from duplicate-name, evidence, and authentication errors.

- [x] **Open only the exact submission route.** Add an authenticated POST matcher for `/api/fanpages/fanpage-admin/register` before the broader FANPAGE_ADMIN and public matchers. Preserve the broader namespace rule. Keep request validation, transactionality, PENDING state, and membership creation in the command. Do not extend the exception to update, invite, remove, appoint, or approval actions. Do not require a token with a newly granted role after saving a profile.

- [x] **Enforce target-fanpage administration for alerts.** Require an authenticated user and `isAdminOfFanpage(fanpageId, currentUserId)` before creating notifications. Replace the ordinary-member check and remove the unconditional system ADMIN return. Return the existing structured 403 for authenticated callers who fail this check and 401 for anonymous callers. Preserve the notification type, payload, recipient handling, and Task 2's after-commit delivery behavior. Do not loosen any neighboring management route.

## Verification

- [x] **Test the complete onboarding path.** Cover Google/local accounts with STUDENT, FANPAGE_ADMIN, and ADMIN roles creating their first profile and submitting a valid PENDING registration using their still-valid existing token. Test missing/incomplete profiles, repeat profile creation, duplicate staff IDs, dual profiles, invalid evidence, and duplicate fanpage names. Verify Google login preserves existing roles and both profiles, and forged roles/user IDs cannot target another account or alter privileges.

- [x] **Exercise real filter and membership boundaries.** Verify anonymous 401, profile-required status, successful eligible submission, and unchanged denial on neighboring management routes for submission-only users. For alerts, allow an active target-fanpage admin; deny an ordinary active member, inactive admin, nonmember, admin of another fanpage, profile-only account, and system ADMIN/FANPAGE_ADMIN without target membership. Permit a system ADMIN account when it also holds active target-fanpage ADMIN membership. Denied requests must create/push no notifications.

- [x] **Run regressions and hand off.** Run eligibility, full-filter registration, auth, fanpage command, and alert tests, including persisted membership checks against PostgreSQL. Document no-verification onboarding, the two eligibility states, existing missing-profile errors, exact route exception, absence-alert rule, and FE cache invalidation after profile creation/account changes. Do not claim submission permission grants access to other management endpoints or requires a role refresh.

## Done when

An authenticated user can create one valid fanpage-admin profile and submit a PENDING fanpage registration without staff verification or a role-changing login. Absence alerts are sent only by active administrators of the specific fanpage, with no global-role bypass. Existing account identity, profile uniqueness, management-route restrictions, and fanpage approval remain intact.
