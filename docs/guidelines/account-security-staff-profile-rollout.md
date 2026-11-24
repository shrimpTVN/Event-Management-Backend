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

---

## 7. Staff Profile Uniqueness Constraints Migration (Task 4)

**Target:** `fanpage_admin_profiles` table  
**Constraints Installed:**
1. `uk_staff_profile_user_id`: UNIQUE (user_id)
2. `uk_staff_profile_staff_id`: UNIQUE (staff_id)

### 7.1 Migration Command
```sh
psql -X --set=ON_ERROR_STOP=1 --set=target_schema=public <<'SQL'
SELECT set_config('ddd.staff_profile_migration_schema', :'target_schema', false);
\i ddd-infrastructure/src/main/resources/database/migration/staff-profile-uniqueness-migration.sql
SQL
```

### 7.2 Safety & Normalization Rules
1. **Preflight Checks:**
   - Aborts if duplicate `user_id` values exist.
   - Aborts if duplicate or trimmed collision `staff_id` values exist.
   - Aborts if `staff_id` is null or blank (`trim(staff_id) = ''`).
2. **Whitespace Normalization:**
   - Any leading/trailing whitespace in `staff_id` is safely normalized with `UPDATE fanpage_admin_profiles SET staff_id = trim(staff_id) WHERE staff_id <> trim(staff_id);`.
3. **Legacy Constraint Alignment:**
   - If an existing constraint on `user_id` exists under a legacy auto-generated name (e.g. `fanpage_admin_profiles_user_id_key`), it is safely renamed to `uk_staff_profile_user_id`.
   - `uk_staff_profile_staff_id` is added if not already present.
4. **Idempotency:**
   - The script is repeatable and safe to execute multiple times.

### 7.3 Integration Test Evidence
Verified with `StaffProfileUniquenessMigrationIT`:
- `migrationAppliesSuccessfullyOnValidData`: **PASSED**
- `migrationAbortsOnDuplicateUserId`: **PASSED**
- `migrationAbortsOnDuplicateStaffId`: **PASSED**
- `migrationAbortsOnTrimCollision`: **PASSED**
- `migrationAbortsOnBlankStaffId`: **PASSED**
- `migrationNormalizesWhitespaceSafely`: **PASSED**
- `migrationIsIdempotent`: **PASSED**
- `uniqueConstraintsEnforceRejection`: **PASSED** (SQLState `23505` asserted)
- `concurrentInsertRaceCondition`: **PASSED** (concurrent race condition verified)

---

## 8. Staff Profile Creation Contract & Self-Service Policy (Task 5)

**Endpoints:**
- `POST /api/users/me/staff-profile`
- `POST /users/me/staff-profile`

### 8.1 Confirmed Policy
- Every authenticated user (including Google OAuth users without password, or existing students) is eligible to create their fanpage-admin / staff profile if they do not already have one.
- No staff verification, approval, institutional email check, password, student profile, or existing `FANPAGE_ADMIN` role is required for profile creation.
- Creation grants no system roles; authorization to manage specific fanpages remains strictly separate.

### 8.2 Request & Response Contract
- **Request Body (`StaffProfileCreateRequestDto`):**
  - `firstName` (String, required, max 100)
  - `lastName` (String, required, max 100)
  - `gender` (String, optional, enum `MALE | FEMALE | OTHER`)
  - `staffProfile` (`StaffProfileDetailsCreateRequestDto`, required):
    - `staffId` (String, required, max 20, trimmed)
    - `orgName` (String, required, max 255)
    - `title` (String, required, max 100)
    - `dateOfBirth` (LocalDate, optional, ISO `YYYY-MM-DD`, cannot be in the future)
    - `phoneNumber` (String, optional, max 20)
- **Response:**
  - Status `201 CREATED` with `BaseResponse<StaffProfileResponseDto>`
  - Reuses existing staff profile representation:
    ```json
    {
      "code": 200,
      "message": "Staff profile created successfully",
      "data": {
        "id": 10,
        "firstName": "John",
        "lastName": "Doe",
        "email": "john.doe@example.com",
        "gender": "MALE",
        "avatar": null,
        "roles": ["STUDENT"],
        "staffProfile": {
          "staffId": "CB12345",
          "orgName": "Faculty of CS",
          "title": "Lecturer",
          "dateOfBirth": "1990-01-01",
          "phoneNumber": "0901234567"
        }
      }
    }
    ```

### 8.3 Error Codes & Conflict Handling
- `409 Conflict` with `STAFF_PROFILE_ALREADY_EXISTS`: Current authenticated user already has a staff profile.
- `409 Conflict` with `STAFF_ID_ALREADY_EXISTS`: The requested `staffId` is already claimed by another user's profile (checked both via precheck and backed by PostgreSQL constraint `uk_staff_profile_staff_id`).
- `400 Bad Request`: Validation failure on request body fields.
- `401 Unauthorized`: Request missing valid authentication token.

---

## 9. Admin User List & Staff Profile Reads (Task 6)

### 9.1 Endpoints
- `GET /api/users/admin` (and `/users/admin`): Paginated user list with attached nullable profiles (`studentProfile` and `staffProfile` / `fanpageAdminProfile`).
- `GET /api/users/admin/{userId}/staff-profile` (and `/users/admin/{userId}/staff-profile`): Single user staff profile lookup by user ID.

### 9.2 GET /users/admin Response Structure
Returns `BaseResponse<Page<AdminUserResponseDto>>`:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "userId": 10,
        "email": "user@example.com",
        "roles": ["STUDENT", "FANPAGE_ADMIN"],
        "role": "STUDENT",
        "firstName": "John",
        "lastName": "Doe",
        "gender": "MALE",
        "isActive": true,
        "providerId": null,
        "avatarUrl": null,
        "studentProfile": {
          "studentId": "B2100001",
          "kNumber": 47,
          "schoolName": "School of Computer Science",
          "majorName": "Software Engineering",
          "majorCode": "SE01",
          "associationName": null
        },
        "staffProfile": {
          "staffId": "CB01234",
          "orgName": "School of Computer Science",
          "title": "Lecturer",
          "dateOfBirth": "1990-01-01",
          "phoneNumber": "0901234567"
        },
        "fanpageAdminProfile": {
          "staffId": "CB01234",
          "orgName": "School of Computer Science",
          "title": "Lecturer",
          "dateOfBirth": "1990-01-01",
          "phoneNumber": "0901234567"
        }
      }
    ],
    "page": {
      "size": 10,
      "number": 0,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

### 9.3 GET /users/admin/{userId}/staff-profile Response Structure
Returns `BaseResponse<StaffProfileResponseDto>`:
- If user exists and has a staff profile: returns HTTP 200 with profile populated.
- If user exists but has no staff profile: returns HTTP 200 with user account data and `staffProfile: null`.
- If user does not exist: returns HTTP 404 `ResourceNotFoundException`.

### 9.4 Optimization Guarantees
- Pagination queries execute with outer joins on `studentProfile` (with `major` and `school`) and `fanpageAdminProfile`.
- Roles are batch-fetched for all user IDs on the current page in a single query (`findUsersWithRolesByIdIn`), avoiding N+1 queries and eliminating Hibernate in-memory pagination warnings (`HHH000104`).

---

## 10. Fanpage Registration Eligibility & Absence Alert Authorization (Task 7)

### 10.1 Fanpage Registration Eligibility Endpoint
- **Path**: `GET /api/users/me/fanpage-registration-eligibility` (and `/users/me/fanpage-registration-eligibility`)
- **Access**: Any authenticated user (`.anyRequest().authenticated()`).
- **Response Structure**: `BaseResponse<FanpageRegistrationEligibilityResponseDto>`
  - Complete profile:
    ```json
    {
      "code": 200,
      "message": "Success",
      "data": {
        "status": "ELIGIBLE",
        "reasonCode": null,
        "requiresReauthentication": false
      }
    }
    ```
  - Incomplete/missing profile:
    ```json
    {
      "code": 200,
      "message": "Success",
      "data": {
        "status": "PROFILE_REQUIRED",
        "reasonCode": "FANPAGE_ADMIN_PROFILE_REQUIRED",
        "requiresReauthentication": false
      }
    }
    ```

### 10.2 Fanpage Registration Command Eligibility Check
- In `FanpageCommandServiceImpl.createFanpage(registerDto)`:
  - Calls `fanpageRegistrationEligibilityService.assertCurrentUserCanRegister()`.
  - Ensures required staff ID and organization are complete without requiring pre-assigned system roles or verification queues.
  - Returns `400 Bad Request` with `USER.002` / `FanpageAdminProfileRequiredException` if incomplete.

### 10.3 Absence Alert Authorization
- In `FanpageStatisticCommandServiceImpl.alertAbsentStudents(fanpageId, dto)`:
  - Calls `assertCallerIsAdminOfFanpage(fanpageId)`.
  - Validates `fanpageMemberRepository.isAdminOfFanpage(fanpageId, currentUserId)`.
  - System `ADMIN` unconditional bypass has been removed: only active administrators of the target fanpage can dispatch absence alert notifications.




