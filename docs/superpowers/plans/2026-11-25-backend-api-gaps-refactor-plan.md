# Backend API Gaps & Refactoring Implementation Plan

> **For agentic workers:** Follow `architecture_conventions.md` strictly (DDD layer boundaries, `SecurityContextPort`, `ServiceUtil`, `BaseResponse<T>`, domain exceptions, MapStruct mappers). Do not implement until the user approves this plan.

**Goal:** Resolve all Group 1 (blocking FE integration) and Group 2 (API gaps & standardization) issues documented in `docs/bug/BACKEND_API_GAPS_AND_FIX_REQUEST.md`.

**Architecture:** Multi-module Maven DDD (`ddd-domain` → `ddd-infrastructure` → `ddd-application` → `ddd-api` → `ddd-start`). Domain models and repository ports remain pure Java in `ddd-domain`; JPA entities, Flyway/SQL migrations (if needed), Specifications, and native SQL queries live in `ddd-infrastructure`; CQRS Command/Query services and DTOs live in `ddd-application`; REST controllers, request/response records, and API mappers live in `ddd-api`.

---

## Group 1 — Mandatory Fixes & Build/Restart

### Task 1.1: Student Profile (`GET / PUT /users/me/profile`)
**Problem:**
- `StudentProfileSummaryDto` and `StudentProfileResponseDto` lack `schoolId` and `majorId`, causing FE `profileService.ts` validation to fail.
- `StudentProfileRequestDto` has `@NotNull` on `associationId`, causing `400 Bad Request` when students have no association.
- `UserCommandServiceImpl` does not validate that `majorId` belongs to `schoolId` or that both `major` and `school` are active.

**Files to Modify:**
- `ddd-application/src/main/java/com/ddd/application/dto/user/StudentProfileSummaryDto.java`
  - Add `Long schoolId`, `Long majorId` (keep backward-compatible secondary constructor delegating with `null, null` if any tests/queries use it).
- `ddd-api/src/main/java/com/ddd/api/dto/user/res/StudentProfileResponseDto.java`
  - Add `Long schoolId`, `Long majorId` (and update secondary constructor).
- `ddd-application/src/main/java/com/ddd/application/mapper/StudentProfileDtoMapper.java`
  - Map `major.id` → `majorId` and `major.school.id` → `schoolId` in `toStudentProfileSummaryDto`.
- `ddd-api/src/main/java/com/ddd/api/dto/user/req/StudentProfileRequestDto.java`
  - Remove `@NotNull` from `associationId` (keep `@Positive`).
  - Add `@Positive Integer schoolId`.
- `ddd-application/src/main/java/com/ddd/application/dto/user/StudentProfileUpdateDto.java`
  - Add `Integer schoolId`.
- `ddd-application/src/main/java/com/ddd/application/service/user/impl/UserCommandServiceImpl.java`
  - In `updateProfileFields(StudentProfileJpaEntity profile, StudentProfileUpdateDto updateDto)`:
    - When `updateDto.majorId() != null`, load `MajorJpaEntity` (throw `ResourceNotFoundException` if missing).
    - Validate `major.getIsActive()` is `true` and `major.getSchool() != null && Boolean.TRUE.equals(major.getSchool().getIsActive())` (else throw `BusinessException`).
    - If `updateDto.schoolId() != null`, validate `major.getSchool().getId().equals(Long.valueOf(updateDto.schoolId()))` (else throw `BusinessException`).
- **Tests:** Update/add unit tests in `UserCommandServiceImplTest` for active/inactive major/school and school-major mismatch.

---

### Task 1.2: SuperAdmin Statistics by Academic Year (`GET /admin/statistics/dashboard`)
**Problem:**
- `AdminStatisticController` only accepts `year` and `semesterId`, ignoring `academicYear` (e.g., `2025-2026`).
- `admin_monthly_trends.sql` counts `CANCELLED` registrations in `registration_count`.
- `admin_top_fanpages.sql` counts `r.user_id` instead of `COUNT(DISTINCT (r.event_id, r.user_id))` and hardcodes `LIMIT 10` in `getDashboard`.

**Files to Modify:**
- `ddd-api/src/main/java/com/ddd/api/controller/AdminStatisticController.java`
  - Add `@RequestParam(required = false) String academicYear` to `getDashboard`, `getOverview`, `getMonthlyTrends`, `getEventDistribution`, `getTopFanpages`, `getDemographics`, `getMonthlyAbsentRate`.
- `ddd-application/src/main/java/com/ddd/application/service/admin/AdminStatisticQueryService.java` & `AdminStatisticQueryServiceImpl.java`
  - Add `String academicYear` parameter to all dashboard/stat methods.
  - When `academicYear != null && !academicYear.isBlank()`, pass `academicYear.trim()` down and do NOT default `year` to `LocalDate.now().getYear()` (so calendar year doesn't conflict with cross-year CTU academic year like `2025-2026`).
  - In `getDashboard`, allow `topFanpages` without hardcoding `10` (or pass `limit` parameter / `null` to return all when requested).
- `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/AdminStatisticRepositoryImpl.java`
  - Add `String academicYear` parameter to all query methods and bind `.param("academicYear", academicYear)`.
- `ddd-infrastructure/src/main/resources/sql/statistic/admin/*.sql`:
  - `admin_overview_events.sql`, `admin_overview_attendance.sql`, `admin_monthly_trends.sql`, `admin_event_distribution.sql`, `admin_top_fanpages.sql`, `admin_demographics_cohort.sql`, `admin_demographics_school.sql`, `admin_demographics_major.sql`, `admin_monthly_absent_rate.sql`:
    - `LEFT JOIN semesters sem ON e.semester_id = sem.id` and `LEFT JOIN academic_years ay ON sem.academic_year_id = ay.id`.
    - Filter: `AND (:academicYear::varchar IS NULL OR ay.code = :academicYear::varchar)`.
  - `admin_monthly_trends.sql`:
    - Change `COUNT(r.user_id) AS registration_count` to `COUNT(r.user_id) FILTER (WHERE r.status != 'CANCELLED') AS registration_count`.
  - `admin_top_fanpages.sql`:
    - Change `attendee_count` to `COUNT(DISTINCT (r.event_id, r.user_id)) FILTER (WHERE r.status IN ('CHECKED_IN', 'CHECKED_OUT', 'COMPLETED')) AS attendee_count`.
    - Support optional limit: `LIMIT COALESCE(:limit::int, 2147483647)` (or pass `null` when unbounded).
- **Tests:** Update `AdminStatisticQueryServiceImplTest` and `AdminStatisticControllerTest`.

---

### Task 1.3: Locked Majors Visibility (`MajorRepositoryImpl` / `MajorJpaRepository`)
**Problem:**
- `MajorRepositoryImpl.findBySchoolId` calls `findBySchoolIdAndIsActiveTrue(schoolId)`, hiding deactivated (`isActive = false`) majors in SuperAdmin management.

**Files to Modify:**
- `ddd-domain/src/main/java/com/ddd/domain/repository/MajorRepository.java`
  - Keep `findBySchoolId(Long schoolId)` returning all majors (active + inactive) for admin, and add `findActiveBySchoolId(Long schoolId)` for student-facing queries (or vice versa, ensuring admin endpoints use the unfiltered query).
- `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/MajorJpaRepository.java`
  - Add `List<MajorJpaEntity> findBySchoolId(Long schoolId);` alongside `findBySchoolIdAndIsActiveTrue(Long schoolId)`.
- `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/MajorRepositoryImpl.java`
  - Update `findBySchoolId` to use `majorJpaRepository.findBySchoolId(schoolId)`, and implement `findActiveBySchoolId` using `findBySchoolIdAndIsActiveTrue(schoolId)`.
- `ddd-application/src/main/java/com/ddd/application/service/major/MajorService.java` & `MajorServiceImpl.java`
  - Expose `findBySchoolId` (admin/all) and `findActiveBySchoolId` (student onboarding) as needed.

---

### Task 1.4: Semester & Academic Year Validation (`SemesterServiceImpl` & `SemesterRequestDto`)
**Problem:**
- `SemesterRequestDto` does not accept `academicYearId`.
- Some `SemesterDto` responses lack `academicYear` code.
- `SemesterServiceImpl` lacks validation against active `AcademicYear` bounds, duplicate `number` per academic year, and overlapping active semesters (using `IllegalArgumentException` instead of `BusinessException`).

**Files to Modify:**
- `ddd-api/src/main/java/com/ddd/api/dto/semester/SemesterRequestDto.java`
  - Add `@Positive Long academicYearId` (optional/nullable).
  - Make `academicYear` optional when `academicYearId` is provided (or keep `@NotBlank` if FE always sends one of them — validate in service).
- `ddd-domain/src/main/java/com/ddd/domain/repository/SemesterRepository.java` & `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/SemesterRepositoryImpl.java` & `SemesterJpaRepository.java`
  - Add `existsByAcademicYearIdAndNumberAndIsActiveTrue(Long academicYearId, Integer number)` (and `...AndIdNot` for updates).
  - Update `existsOverlappingDateRange` to check `isActive = true` and exclude `id` on update: `(startDate <= :endDate AND endDate >= :startDate AND isActive = true AND (:excludeId IS NULL OR id != :excludeId))`.
- `ddd-application/src/main/java/com/ddd/application/service/academicyear/impl/SemesterServiceImpl.java`
  - In `validateDateRange`: throw `BusinessException` instead of `IllegalArgumentException` when `!startDate.isBefore(endDate)`.
  - Resolve `AcademicYear`: if `academicYearId != null`, fetch active `AcademicYear` (else `ResourceNotFoundException` / `BusinessException`); else resolve by `academicYear` code or `ensureAcademicYear`.
  - Validate `[startDate, endDate]` falls within `[year.getStartDate(), year.getEndDate()]` (throw `BusinessException`).
  - Validate no duplicate `number` in the same `academicYearId` among active semesters (throw `DuplicateResourceException`).
  - Validate no overlapping date range with other active semesters (excluding `id` on update).
  - Ensure `academicYear` code is always populated on returned `SemesterDto` (map via `AcademicYearRepository` or `SemesterJpaEntity.academicYear.code` in `SemesterMapper` / `SemesterDtoMapper`).

---

## Group 2 — API Gaps & Standardization

### Task 2.1: SuperAdmin Event Search (`GET /events/admin`)
**Problem:**
- `EventFilterRequestDto`'s compact constructor forces `isActive = true` and `status = PUBLISHED` when null/blank, forcing FE to fire 10 parallel requests.
- `EventFilterRequestDto` only supports a single `String status`.

**Files to Modify:**
- `ddd-api/src/main/java/com/ddd/api/dto/event/req/EventFilterRequestDto.java`
  - Add `List<EventStatusEnum> statuses` field.
  - Remove the forced `isActive = true` and `status = PUBLISHED` defaults from the compact constructor (keep pagination/sort defaults only).
  - In `EventController.searchEvents(...)` (public endpoint), apply the public defaults (`isActive = true`, `status = PUBLISHED` if both `status` and `statuses` are empty).
  - In `EventController.searchEventsForAdmin(...)`, pass `request` as-is so `status == null`, `statuses == null`, and `isActive == null` return all matching events in 1 request.
- `ddd-application/src/main/java/com/ddd/application/dto/event/EventSearchCriteriaDto.java`
  - Add `List<EventStatusEnum> statuses`.
- `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/specification/EventSpecification.java`
  - Support `List<EventStatusEnum> statuses` (`root.get("status").in(statuses)`) when non-empty, falling back to single `status` if provided.
- `ddd-application/src/main/java/com/ddd/application/service/event/impl/EventQueryServiceImpl.java`
  - Pass `criteria.statuses()` to `EventSpecification.withFilter(...)`.

---

### Task 2.2: SuperAdmin Hide / Restore Violating Events
**Problem:**
- No endpoint for SuperAdmin to hide/flag an event with `violationReason` or restore it.

**Design Option (to confirm):**
- **Recommended:** Reuse `isActive` (`false` = hidden/soft-deleted, `true` = active) + add nullable `violationReason` (`TEXT`) on `events` table, `Event`, `EventJpaEntity`, `EventInfoDto`, `EventResponseDto`.
  - *Pros:* All existing public queries (`findWithDetailsByIdAndIsActiveTrue`, `searchEvents` with `isActive = true`) automatically exclude hidden events with zero risk of leaking.
  - *Cons:* Cannot distinguish admin-hidden from organizer-soft-deleted unless `violationReason != null` is used as the indicator.
- **Alternative:** Add a separate `isHidden` boolean column alongside `violationReason`.
  - *Pros:* Separates admin moderation from organizer soft-delete.
  - *Cons:* Requires updating every public query/specification/recommendation query to also check `isHidden = false`.

**Files to Modify (assuming Recommended option):**
- `ddd-domain/src/main/java/com/ddd/domain/model/Event.java` — add `String violationReason`.
- `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/EventJpaEntity.java` — add `@Column(name = "violation_reason", columnDefinition = "TEXT") private String violationReason;`.
- `ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/EventMapper.java` & `ddd-application/src/main/java/com/ddd/application/mapper/EventDtoMapper.java` — map `violationReason`.
- `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java` & `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java` — add `String violationReason`.
- `ddd-api/src/main/java/com/ddd/api/dto/event/req/EventVisibilityRequestDto.java` (new record: `@NotNull Boolean hidden`, `String reason`).
- `ddd-application/src/main/java/com/ddd/application/service/event/EventCommandService.java` & `EventCommandServiceImpl.java`
  - Add `void updateEventVisibilityByAdmin(Long id, boolean hidden, String reason)` checking `securityContextPort.hasRole("ADMIN")`, setting `event.setActive(!hidden)` and `event.setViolationReason(hidden ? reason : null)`.
- `ddd-api/src/main/java/com/ddd/api/controller/EventController.java`
  - Add `@PatchMapping("/admin/{id}/visibility") @PreAuthorize("hasRole('ADMIN')")` delegating to `eventCommandService.updateEventVisibilityByAdmin(id, request.hidden(), request.reason())`.

---

### Task 2.3: Event End Time (`dateEndHappen`) & `ONGOING` Status/Filter
**Problem:**
- `Event` / `EventResponseDto` lacks `dateEndHappen`, and `EventStatusEnum` / filters lack `ONGOING`.

**Design Option (to confirm):**
- **Recommended:** Add `Instant dateEndHappen` (nullable for backward compatibility with existing rows) to `Event`, `EventJpaEntity`, `EventCreateRequestDto`, `EventUpdateRequestDto`, `EventInfoDto`, `EventResponseDto` (with validation `dateEndHappen.isAfter(dateHappen)` when provided). Treat `ONGOING` as a **query filter / computed state** (`status == PUBLISHED && dateHappen <= now && (dateEndHappen == null || dateEndHappen > now)`) rather than a persisted DB enum value, so no cron job is needed to transition `PUBLISHED` → `ONGOING`.
  - *Pros:* Zero state-sync bugs; works immediately for all events based on `Clock`.
  - *Cons:* `status` field in DB remains `PUBLISHED` while happening.

**Files to Modify:**
- `ddd-domain/src/main/java/com/ddd/domain/model/Event.java`, `EventJpaEntity.java`, `EventCreateRequestDto.java`, `EventCreateDto.java`, `EventUpdateRequestDto.java`, `EventUpdateDto.java`, `EventInfoDto.java`, `EventResponseDto.java` — add `Instant dateEndHappen`.
- `ddd-domain/src/main/java/com/ddd/domain/model/Event.java` — in `validateEventDates()`, if `dateEndHappen != null && !dateEndHappen.isAfter(dateHappen)`, throw `BusinessException`.
- `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/specification/EventSpecification.java` — if filter `status` (or `statuses`) includes `"ONGOING"`, translate to `(status = PUBLISHED AND dateHappen <= now AND (dateEndHappen IS NULL OR dateEndHappen > now))`.

---

### Task 2.4: Fanpage Avatar, `orgName`, and `isFollowFanpage` in `EventResponseDto`
**Problem:**
- `EventResponseDto` lacks `fanpageAvatarUrl` and `orgName`, forcing FE to make extra calls to `/fanpages/{id}` and `/fanpages/following`.

**Files to Modify:**
- `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`
  - Add `String fanpageAvatarUrl`, `String orgName` (update `withRegistrationInfo` and secondary constructors).
- `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`
  - Add `String fanpageAvatarUrl`, `String orgName`.
- `ddd-application/src/main/java/com/ddd/application/mapper/EventDtoMapper.java`
  - Map `fanpage.avatarUrl` → `fanpageAvatarUrl` and `fanpage.orgName` → `orgName` in `toInfoDto(EventJpaEntity)`.
- `ddd-application/src/main/java/com/ddd/application/service/event/impl/EventResponseEnricher.java` & `EventQueryServiceImpl.java`
  - Verify `getEventById` (`GET /events/{id}`) passes `security.getCurrentUserId()` to `responseEnricher.enrich(...)` and populates `isFollowFanpage` accurately when authenticated.

---

### Task 2.5: Event Views & Recommendations Contract (`POST /events/views`)
- Verify `EventViewCommandServiceImpl` idempotency on `batchId` + `(studentId, eventId)` in `EventViewRepositoryImpl`.
- Document the agreed FE/BE contract (client-generated UUID `batchId` per feed/scroll session, dwell/viewport threshold before batching `POST /events/views`).

---

## Verification Plan
1. Run `mvn test` across all modules (`ddd-domain`, `ddd-infrastructure`, `ddd-application`, `ddd-api`).
2. Build and start `ddd-start` on port `8080`.
3. Smoke test:
   - `PUT /users/me/profile` returns `schoolId` and `majorId`, accepts null `associationId`, rejects inactive/mismatched `majorId`.
   - `GET /admin/statistics/dashboard?academicYear=2025-2026` filters all widgets accurately.
   - `GET /events/admin` without `status`/`isActive` returns all events across statuses and active states.
   - `PATCH /events/admin/{id}/visibility` hides and restores events with `violationReason`.
