# Comprehensive Refactoring Plan: Event DTOs & Superadmin API Issues

- **Document Version:** 1.0.0
- **Associated Bug Reports:**
  - [BUG_REPORT_EVENT_TYPE_ID.md](../bug/BUG_REPORT_EVENT_TYPE_ID.md)
  - [superadmin-api-todo.md](../bug/superadmin-api-todo.md)
- **Target Architecture:** Spring Boot DDD (Multi-module) as documented in [architecture_conventions.md](../../architecture_conventions.md)

---

## 1. Executive Summary

This refactoring plan consolidates the fixes and missing administrative capabilities identified across both bug reports into three structured, non-breaking execution phases:
1. **Phase 1: Critical Bug Fixes (Immediate)**: Resolves the missing `userId` in `UserSummaryDto` and missing `eventTypeId`/`criteriaId` in Event DTOs.
2. **Phase 2: Fanpage Member & Staff Profile Data for Superadmin**: Resolves member lookup visibility on inactive fanpages and exposes staff profiles (`staffId`, `phoneNumber`).
3. **Phase 3: Event Registration Metrics & Superadmin Operations**: Implements non-breaking `registeredCount` calculation, rejection reasons for fanpages, and administrative semester controls.

---

## 2. Phase 1: Critical Bug Fixes (Immediate Execution)

### 1.1 Fix Missing `userId` in `UserDtoMapper`
- **Location**: `ddd-application/src/main/java/com/ddd/application/mapper/UserDtoMapper.java`
- **Current Defect**: `UserJpaEntity` has primary key `id`, but `UserSummaryDto` names this field `userId`. MapStruct does not auto-match differing names without explicit mapping, leaving `userId` as `null` in `/users/admin` and `/users/search-users`.
- **Target Solution**:
  ```java
  @Mapping(target = "userId", source = "id")
  @Mapping(target = "roles", expression = "java(mapRoleNames(userJpaEntity.getRoles()))")
  @Mapping(target = "role", expression = "java(mapPrimaryRole(userJpaEntity.getRoles()))")
  UserSummaryDto toUserSummaryDto(UserJpaEntity userJpaEntity);
  ```
- **Verification**: Rebuild application module and verify generated code sets `userId = userJpaEntity.getId()`.

---

### 1.2 Add `eventTypeId` and `criteriaId` to Event DTOs and Mappings
- **Location**:
  - `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`
  - `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`
  - `ddd-application/src/main/java/com/ddd/application/mapper/EventDtoMapper.java`
  - `ddd-application/src/test/java/com/ddd/application/service/event/impl/StoredEventQueryServiceImplTest.java`
- **Current Defect**: `EventInfoDto` and `EventResponseDto` only expose `eventTypeName` and `criteriaName` as strings. The underlying entities already have `eventType.id` and `criteria.id`, but they are omitted in DTOs, breaking frontend drawer filtering by ID and causing ambiguity on sub-criteria with identical names.
- **Target Solution**:
  1. Add `Long eventTypeId` and `Long criteriaId` to `EventInfoDto` record fields and backward-compatible overloaded constructors.
  2. Add `Long eventTypeId` and `Long criteriaId` to `EventResponseDto` record fields and backward-compatible overloaded constructors.
  3. Add mapping rules to `EventDtoMapper.toInfoDto(EventJpaEntity entity)`:
     ```java
     @Mapping(target = "eventTypeId", source = "eventType.id")
     @Mapping(target = "eventTypeName", source = "eventType.name")
     @Mapping(target = "criteriaId", source = "criteria.id")
     @Mapping(target = "criteriaName", source = "criteria.name")
     ```
  4. Update unit test mock constructors in `StoredEventQueryServiceImplTest.java`.

---

## 3. Phase 2: Fanpage Member & Staff Profile Data for Superadmin

### 2.1 Allow Superadmin to View Members of Inactive Fanpages
- **Location**: `ddd-application/src/main/java/com/ddd/application/service/fanpage/impl/FanpageQueryServiceImpl.java`
- **Current Defect**: `getMembersOfFanpage(Long fanpageId)` enforces `fanpageRepository.findByIdAndIsActive(fanpageId, true)`, which throws 404 if the fanpage is inactive or banned, preventing Superadmin from seeing who was the admin.
- **Target Solution**:
  - Inject `SecurityContextPort`.
  - If `securityContextPort.hasRole("ADMIN")`, look up fanpage by `findById(fanpageId)` without the `isActive=true` constraint.

---

### 2.2 Provide Staff Profile (`staffId`, `phoneNumber`) for Fanpage Admins and Superadmin
- **Location**:
  - `ddd-application/src/main/java/com/ddd/application/dto/fanpage/FanpageMemberDto.java`
  - `ddd-api/src/main/java/com/ddd/api/dto/fanpage/res/FanpageMemberResponseDto.java`
  - `ddd-application/src/main/java/com/ddd/application/mapper/FanpageMemberDtoMapper.java`
  - `ddd-api/src/main/java/com/ddd/api/controller/UserController.java`
- **Current Defect**:
  - `FanpageMemberResponseDto` lacks `staffId` and `phoneNumber`.
  - `GET /users/admin/{id}/profile` calls `getProfileStudentById` returning `StudentProfileResponseDto` even for staff/admins who only have a `FanpageAdminProfileJpaEntity`.
- **Target Solution**:
  1. Add nullable `String staffId` and `String phoneNumber` to `FanpageMemberDto` and `FanpageMemberResponseDto`.
  2. In `FanpageMemberDtoMapper`, map from `entity.getUser().getFanpageAdminProfile()` safely using null-safe expressions or default helper methods.
  3. In `UserController`, provide an endpoint or unified composite profile `GET /users/admin/{id}/staff-profile` for Superadmin inspection.

---

## 4. Phase 3: Registration Counts & Superadmin Management Operations

### 3.1 Non-breaking `registeredCount` for Events
- **Location**:
  - `ddd-domain/src/main/java/com/ddd/domain/repository/RegistrationRepository.java`
  - `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/RegistrationJpaRepository.java`
  - `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`
  - `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`
  - `ddd-application/src/main/java/com/ddd/application/service/event/impl/EventQueryServiceImpl.java`
- **Current Defect**: `capacity` represents target limit, but total confirmed registrations (`status != 'CANCELLED'`) is not returned.
- **Target Solution**:
  1. Add query in `RegistrationJpaRepository` to count non-cancelled registrations by event IDs in batch (`SELECT r.event.id, COUNT(r.id) FROM RegistrationJpaEntity r WHERE r.event.id IN :eventIds AND r.status <> 'CANCELLED' GROUP BY r.event.id`).
  2. Add `Long registeredCount` to `EventInfoDto` and `EventResponseDto`.
  3. Hydrate `registeredCount` in `EventQueryServiceImpl.searchAndFilterEvents` without N+1 query overhead.

---

### 3.2 Fanpage Rejection with Reason
- **Location**:
  - `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/FanpageJpaEntity.java`
  - `ddd-domain/src/main/java/com/ddd/domain/model/Fanpage.java`
  - `ddd-application/src/main/java/com/ddd/application/service/fanpage/FanpageCommandService.java`
  - `ddd-api/src/main/java/com/ddd/api/controller/FanpageController.java`
- **Target Solution**:
  1. Add `rejectionReason` column to `fanpages` table and entities.
  2. Create endpoint `POST /fanpages/admin/reject-fanpage/{fanpageId}` accepting `reason`.
  3. Mark status as `REJECTED` and store reason.

---

### 3.3 Superadmin Event Visibility & Current Semester Setting
- **Location**:
  - `ddd-api/src/main/java/com/ddd/api/controller/EventController.java`
  - `ddd-api/src/main/java/com/ddd/api/controller/SemesterController.java`
- **Target Solution**:
  1. Add `PATCH /events/admin/{id}/toggle-status` or moderation endpoint for Superadmin.
  2. Add `PATCH /semesters/admin/{id}/set-current` to designate the active current semester.

---

## 5. Architectural Quality Bar & Constraints
- **DDD Boundary Integrity**:
  - `ddd-domain` remains completely free of Spring/JPA annotations.
  - Controllers only use `BaseResponse<T>` and never extract user identity to pass as service parameters; services inject `SecurityContextPort`.
- **Query Performance**:
  - No N+1 queries when populating `registeredCount` across paginated lists.
- **Zero Suppression / High Quality**:
  - No suppressed linter/compiler warnings, no hardcoded dummy IDs.

---

## 6. Verification and Validation Matrix

| Target Fix | Verification Command / Step | Expected Outcome |
|---|---|---|
| User ID Mapping | `mvn test-compile -pl ddd-application` | Generated `UserDtoMapperImpl` maps `userId = userJpaEntity.getId()` |
| Event IDs Mapping | `mvn test-compile -pl ddd-application,ddd-api` | `EventInfoDto` & `EventResponseDto` expose `eventTypeId` & `criteriaId` |
| Unit Tests | `mvn test -pl ddd-application` | All tests pass, including `StoredEventQueryServiceImplTest` |
| Inactive Fanpage Members | Call `GET /api/fanpages/{id}/members` as ADMIN | Returns 200 OK with members even when `isActive = false` |
| Staff Profile | Call `GET /api/fanpages/{id}/members` | Admin member includes non-null `staffId` and `phoneNumber` |
