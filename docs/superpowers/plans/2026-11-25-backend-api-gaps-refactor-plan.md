# Backend API Gaps & Refactoring Implementation Plan

> **For agentic workers:** Follow `architecture_conventions.md` strictly (DDD layer boundaries, `SecurityContextPort`, `ServiceUtil`, `BaseResponse<T>`, domain exceptions, MapStruct mappers). Execute **one task at a time**, run its verification tests, and **commit immediately after each task** before moving to the next. Do not implement until the user approves this plan.

**Goal:** Resolve Group 1 (blocking FE integration) and Group 2 (API gaps & standardization, excluding 2.3 Event End Time & `ONGOING` filter) from `docs/bug/BACKEND_API_GAPS_AND_FIX_REQUEST.md`, with an atomic commit after each task.

**Architecture:** Multi-module Maven DDD (`ddd-domain` → `ddd-infrastructure` → `ddd-application` → `ddd-api` → `ddd-start`).
- `ddd-domain`: Pure Java models, enums, exceptions, repository & port interfaces.
- `ddd-infrastructure`: JPA entities, Spring Data JPA repositories, Specifications, repository impls, MapStruct `*Mapper`, native SQL files (`src/main/resources/sql/statistic/admin/*.sql`).
- `ddd-application`: CQRS `*CommandService` / `*QueryService` (or Tier 2 `*Service`), `*InfoDto` / `*Dto`, MapStruct `*DtoMapper`.
- `ddd-api`: `@RestController` returning `BaseResponse<T>`, `*RequestDto` / `*ResponseDto` records, MapStruct `*ApiMapper`.

---

## Task 1: Student Profile (`GET / PUT /users/me/profile`)

**Scope:** Fix `schoolId`/`majorId` missing in responses, optional `associationId`, and server-side validation of `majorId` ↔ `schoolId` and active status.

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/user/StudentProfileSummaryDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/user/res/StudentProfileResponseDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/mapper/StudentProfileDtoMapper.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/user/req/StudentProfileRequestDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/user/StudentProfileUpdateDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/user/impl/UserCommandServiceImpl.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/user/impl/UserCommandServiceImplTest.java`

**Steps:**
1. Add `Long schoolId` and `Long majorId` to `StudentProfileSummaryDto` and `StudentProfileResponseDto` (keep existing secondary constructors delegating with `null, null` for compatibility).
2. In `StudentProfileDtoMapper.toStudentProfileSummaryDto`, add `@Mapping(source = "major.id", target = "majorId")` and `@Mapping(source = "major.school.id", target = "schoolId")`.
3. In `StudentProfileRequestDto`, remove `@NotNull` from `associationId` (keep `@Positive`) and add `@Positive Integer schoolId`. Add `Integer schoolId` to `StudentProfileUpdateDto`.
4. In `UserCommandServiceImpl.updateProfileFields`:
   - When `updateDto.majorId() != null`, fetch `MajorJpaEntity` (throw `ResourceNotFoundException` if missing).
   - Validate `Boolean.TRUE.equals(major.getIsActive())` and `major.getSchool() != null && Boolean.TRUE.equals(major.getSchool().getIsActive())` (throw `BusinessException` if inactive).
   - If `updateDto.schoolId() != null`, validate `major.getSchool().getId().equals(Long.valueOf(updateDto.schoolId()))` (throw `BusinessException` on mismatch).
5. Add unit tests in `UserCommandServiceImplTest` covering: `schoolId`/`majorId` returned, null `associationId` allowed, inactive major/school rejected, and `schoolId` mismatch rejected.

**Verify & Commit:**
```bash
mvn test -pl ddd-application,ddd-api -Dtest="UserCommandServiceImplTest,UserQueryServiceImplTest"
git add ddd-application/src/main/java/com/ddd/application/dto/user/StudentProfileSummaryDto.java \
  ddd-api/src/main/java/com/ddd/api/dto/user/res/StudentProfileResponseDto.java \
  ddd-application/src/main/java/com/ddd/application/mapper/StudentProfileDtoMapper.java \
  ddd-api/src/main/java/com/ddd/api/dto/user/req/StudentProfileRequestDto.java \
  ddd-application/src/main/java/com/ddd/application/dto/user/StudentProfileUpdateDto.java \
  ddd-application/src/main/java/com/ddd/application/service/user/impl/UserCommandServiceImpl.java \
  ddd-application/src/test/java/com/ddd/application/service/user/impl/UserCommandServiceImplTest.java
git commit -m "fix(profile): return schoolId/majorId, make associationId optional, validate major/school"
```

---

## Task 2: SuperAdmin Statistics by Academic Year (`GET /admin/statistics/dashboard`)

**Scope:** Support `academicYear` filter across all admin statistics endpoints and SQL queries, exclude `CANCELLED` from monthly trend registrations, and count distinct `(event_id, user_id)` in top fanpages without hardcoding a top-10 cap when unbounded.

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/AdminStatisticController.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/admin/AdminStatisticQueryService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/admin/impl/AdminStatisticQueryServiceImpl.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/AdminStatisticRepositoryImpl.java`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_overview_events.sql`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_overview_attendance.sql`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_monthly_trends.sql`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_event_distribution.sql`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_top_fanpages.sql`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_demographics_cohort.sql`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_demographics_school.sql`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_demographics_major.sql`
- Modify: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_monthly_absent_rate.sql`
- Test: `ddd-application/src/test/java/com/ddd/application/service/admin/impl/AdminStatisticQueryServiceImplTest.java`
- Test: `ddd-api/src/test/java/com/ddd/api/controller/AdminStatisticControllerTest.java`

**Steps:**
1. Add `@RequestParam(required = false) String academicYear` to `getDashboard`, `getOverview`, `getMonthlyTrends`, `getEventDistribution`, `getTopFanpages`, `getDemographics`, `getMonthlyAbsentRate` in `AdminStatisticController`.
2. Update `AdminStatisticQueryService` and `AdminStatisticQueryServiceImpl` to accept `String academicYear`. When `academicYear != null && !academicYear.isBlank()`, normalize (`trim()`) and do NOT default `year` to `LocalDate.now().getYear()`. In `getDashboard`, pass `null` (or configurable limit) for `topFanpages` so full list is available.
3. Update `AdminStatisticRepositoryImpl` methods to accept `String academicYear` and bind `.param("academicYear", academicYear)`.
4. Update all 9 `admin_*.sql` files to `LEFT JOIN semesters sem ON e.semester_id = sem.id LEFT JOIN academic_years ay ON sem.academic_year_id = ay.id` and filter `AND (:academicYear::varchar IS NULL OR ay.code = :academicYear::varchar)`.
5. In `admin_monthly_trends.sql`, update `registration_count` to `COUNT(r.user_id) FILTER (WHERE r.status != 'CANCELLED') AS registration_count`.
6. In `admin_top_fanpages.sql`, update `attendee_count` to `COUNT(DISTINCT (r.event_id, r.user_id)) FILTER (WHERE r.status IN ('CHECKED_IN', 'CHECKED_OUT', 'COMPLETED')) AS attendee_count`, and use `LIMIT COALESCE(:limit::int, 2147483647)`.
7. Update unit/controller tests for `academicYear` propagation.

**Verify & Commit:**
```bash
mvn test -pl ddd-application,ddd-api -Dtest="AdminStatisticQueryServiceImplTest,AdminStatisticControllerTest"
git add ddd-api/src/main/java/com/ddd/api/controller/AdminStatisticController.java \
  ddd-application/src/main/java/com/ddd/application/service/admin/AdminStatisticQueryService.java \
  ddd-application/src/main/java/com/ddd/application/service/admin/impl/AdminStatisticQueryServiceImpl.java \
  ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/AdminStatisticRepositoryImpl.java \
  ddd-infrastructure/src/main/resources/sql/statistic/admin/ \
  ddd-application/src/test/java/com/ddd/application/service/admin/impl/AdminStatisticQueryServiceImplTest.java \
  ddd-api/src/test/java/com/ddd/api/controller/AdminStatisticControllerTest.java
git commit -m "fix(admin-stats): support academicYear filter and fix trend/fanpage attendee counts"
```

---

## Task 3: Locked Majors Visibility (`MajorRepositoryImpl` / `MajorJpaRepository`)

**Scope:** Ensure SuperAdmin major management can see deactivated (`isActive = false`) majors by school while keeping an active-only query for student onboarding.

**Files:**
- Modify: `ddd-domain/src/main/java/com/ddd/domain/repository/MajorRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/MajorJpaRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/MajorRepositoryImpl.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/major/MajorService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/major/impl/MajorServiceImpl.java`

**Steps:**
1. Add `List<MajorJpaEntity> findBySchoolId(Long schoolId);` to `MajorJpaRepository` alongside `findBySchoolIdAndIsActiveTrue(Long schoolId)`.
2. In `MajorRepository` and `MajorRepositoryImpl`, update `findBySchoolId(Long schoolId)` to return all majors (`findBySchoolId`), and add `findActiveBySchoolId(Long schoolId)` using `findBySchoolIdAndIsActiveTrue`.
3. Expose `findActiveBySchoolId(Long schoolId)` in `MajorService` / `MajorServiceImpl` for student-facing callers if needed.

**Verify & Commit:**
```bash
mvn test -pl ddd-domain,ddd-infrastructure,ddd-application
git add ddd-domain/src/main/java/com/ddd/domain/repository/MajorRepository.java \
  ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/MajorJpaRepository.java \
  ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/MajorRepositoryImpl.java \
  ddd-application/src/main/java/com/ddd/application/service/major/MajorService.java \
  ddd-application/src/main/java/com/ddd/application/service/major/impl/MajorServiceImpl.java
git commit -m "fix(major): include inactive majors in findBySchoolId for admin management"
```

---

## Task 4: Semester & Academic Year Validation (`SemesterServiceImpl` & `SemesterRequestDto`)

**Scope:** Support optional `academicYearId` in `SemesterRequestDto`, always return `academicYear` code in `SemesterDto`, and enforce CTU academic-year/date/number/overlap validation using `BusinessException`.

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/semester/SemesterRequestDto.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/repository/SemesterRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/SemesterJpaRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/SemesterRepositoryImpl.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/academicyear/impl/SemesterServiceImpl.java`

**Steps:**
1. Add `@Positive Long academicYearId` (nullable) to `SemesterRequestDto` and relax `@NotBlank` on `academicYear` if `academicYearId` is supplied.
2. Add repository queries in `SemesterRepository`, `SemesterJpaRepository`, and `SemesterRepositoryImpl`:
   - `existsActiveByAcademicYearIdAndNumber(Long academicYearId, Integer number, Long excludeId)`
   - `existsActiveOverlappingDateRange(LocalDate startDate, LocalDate endDate, Long excludeId)`
3. In `SemesterServiceImpl`:
   - Replace `IllegalArgumentException` with `BusinessException` in `validateDateRange` (`!startDate.isBefore(endDate)`).
   - Resolve active `AcademicYear` (by `academicYearId` if provided, else by `academicYear` code via `AcademicYearRepository`, throwing `ResourceNotFoundException` / `BusinessException` if missing or inactive).
   - Validate `[startDate, endDate]` is within `[year.getStartDate(), year.getEndDate()]` (`!startDate.isBefore(year.getStartDate()) && !endDate.isAfter(year.getEndDate())`).
   - Validate no duplicate `number` in the same `academicYearId` among active semesters (`DuplicateResourceException`).
   - Validate no overlapping date range with other active semesters, excluding `id` on update (`DuplicateResourceException`).
   - Ensure `academicYear` code is always populated on returned `SemesterDto` across `findAll`, `findAllActive`, `findById`, `getCurrent`, `createSemester`, `updateSemester`.

**Verify & Commit:**
```bash
mvn test -pl ddd-domain,ddd-infrastructure,ddd-application,ddd-api
git add ddd-api/src/main/java/com/ddd/api/dto/semester/SemesterRequestDto.java \
  ddd-domain/src/main/java/com/ddd/domain/repository/SemesterRepository.java \
  ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/SemesterJpaRepository.java \
  ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/SemesterRepositoryImpl.java \
  ddd-application/src/main/java/com/ddd/application/service/academicyear/impl/SemesterServiceImpl.java
git commit -m "fix(semester): support academicYearId, populate academicYear code, validate bounds & overlaps"
```

---

## Task 5: SuperAdmin Event Search (`GET /events/admin`)

**Scope:** Allow `GET /events/admin` to return all events across statuses and `isActive` states in a single request, and support multi-status filtering via `statuses`.

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/event/req/EventFilterRequestDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/EventController.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/event/EventSearchCriteriaDto.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/specification/EventSpecification.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/event/impl/EventQueryServiceImpl.java`

**Steps:**
1. Add `List<EventStatusEnum> statuses` to `EventFilterRequestDto` and `EventSearchCriteriaDto`.
2. Remove the forced `isActive = true` and `status = PUBLISHED` defaults from `EventFilterRequestDto`'s compact constructor (keep only pagination/sort defaults).
3. In `EventController.searchEvents(...)` (public endpoint), apply public defaults (`isActive = true`, and `status = PUBLISHED` when both `status` and `statuses` are empty). Keep `searchEventsForAdmin(...)` passing `request` unfiltered.
4. In `EventSpecification.withFilter(...)` and `EventQueryServiceImpl.searchAndFilterEvents(...)`, support `List<EventStatusEnum> statuses` (`root.get("status").in(statuses)`) when non-empty, otherwise fall back to `status` if non-blank.

**Verify & Commit:**
```bash
mvn test -pl ddd-infrastructure,ddd-application,ddd-api -Dtest="EventApiMapperTest"
git add ddd-api/src/main/java/com/ddd/api/dto/event/req/EventFilterRequestDto.java \
  ddd-api/src/main/java/com/ddd/api/controller/EventController.java \
  ddd-application/src/main/java/com/ddd/application/dto/event/EventSearchCriteriaDto.java \
  ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/specification/EventSpecification.java \
  ddd-application/src/main/java/com/ddd/application/service/event/impl/EventQueryServiceImpl.java
git commit -m "feat(event-admin): support unfiltered admin search and multi-status filtering"
```

---

## Task 6: SuperAdmin Hide / Restore Violating Events (`PATCH /events/admin/{id}/visibility`)

**Scope:** Allow SuperAdmin to hide an event with `violationReason` (`isActive = false`) and restore it (`isActive = true`, clearing `violationReason`), and expose `violationReason` in `EventResponseDto`.

**Files:**
- Modify: `ddd-domain/src/main/java/com/ddd/domain/model/Event.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/EventJpaEntity.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/EventMapper.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`
- Create: `ddd-api/src/main/java/com/ddd/api/dto/event/req/EventVisibilityRequestDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/event/EventCommandService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/event/impl/EventCommandServiceImpl.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/EventController.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/event/impl/EventCommandServiceImplTest.java`

**Steps:**
1. Add `String violationReason` to `Event`, `EventJpaEntity` (`@Column(name = "violation_reason", columnDefinition = "TEXT")`), `EventInfoDto`, and `EventResponseDto` (mapped automatically by `EventMapper`, `EventDtoMapper`, `EventApiMapper`).
2. Create `EventVisibilityRequestDto(@NotNull Boolean hidden, String reason)` in `com.ddd.api.dto.event.req`.
3. Add `void updateEventVisibilityByAdmin(Long id, boolean hidden, String reason)` to `EventCommandService` and implement in `EventCommandServiceImpl`:
   - Check `securityContextPort.hasRole("ADMIN")` (throw `UnauthorizedException` if not).
   - Load `Event` via `eventRepository.findByIdForUpdate(id)`.
   - Set `event.setActive(!hidden)` and `event.setViolationReason(hidden ? reason : null)` and save.
4. Add `@PatchMapping("/admin/{id}/visibility") @PreAuthorize("hasRole('ADMIN')")` in `EventController`.
5. Add unit tests in `EventCommandServiceImplTest` for hide (with reason), restore (clears reason), and non-admin rejection.

**Verify & Commit:**
```bash
mvn test -pl ddd-application,ddd-api -Dtest="EventCommandServiceImplTest,EventApiMapperTest"
git add ddd-domain/src/main/java/com/ddd/domain/model/Event.java \
  ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/EventJpaEntity.java \
  ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/EventMapper.java \
  ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java \
  ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java \
  ddd-api/src/main/java/com/ddd/api/dto/event/req/EventVisibilityRequestDto.java \
  ddd-application/src/main/java/com/ddd/application/service/event/EventCommandService.java \
  ddd-application/src/main/java/com/ddd/application/service/event/impl/EventCommandServiceImpl.java \
  ddd-api/src/main/java/com/ddd/api/controller/EventController.java \
  ddd-application/src/test/java/com/ddd/application/service/event/impl/EventCommandServiceImplTest.java
git commit -m "feat(event-admin): add PATCH /events/admin/{id}/visibility with violationReason"
```

---

## Task 7: Fanpage Avatar, `orgName`, and `isFollowFanpage` in `EventResponseDto`

**Scope:** Include `fanpageAvatarUrl` and `orgName` in `EventInfoDto` / `EventResponseDto`, and verify `isFollowFanpage` enrichment on `GET /events/{id}`.

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/mapper/EventDtoMapper.java`
- Test: `ddd-application/src/test/java/com/ddd/application/mapper/EventDtoMapperTest.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/event/impl/EventResponseEnricherTest.java`

**Steps:**
1. Add `String fanpageAvatarUrl` and `String orgName` to `EventInfoDto` (update `withRegistrationInfo` and secondary constructors) and `EventResponseDto`.
2. In `EventDtoMapper.toInfoDto(EventJpaEntity)`, add `@Mapping(target = "fanpageAvatarUrl", source = "fanpage.avatarUrl")` and `@Mapping(target = "orgName", source = "fanpage.orgName")`.
3. Verify `EventDtoMapperTest` and `EventResponseEnricherTest` (including signed-in `isFollowFanpage` on `enrich(EventInfoDto, Long userId)`).

**Verify & Commit:**
```bash
mvn test -pl ddd-application,ddd-api -Dtest="EventDtoMapperTest,EventResponseEnricherTest,EventApiMapperTest"
git add ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java \
  ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java \
  ddd-application/src/main/java/com/ddd/application/mapper/EventDtoMapper.java \
  ddd-application/src/test/java/com/ddd/application/mapper/EventDtoMapperTest.java \
  ddd-application/src/test/java/com/ddd/application/service/event/impl/EventResponseEnricherTest.java
git commit -m "feat(event): include fanpageAvatarUrl and orgName in EventResponseDto"
```

---

## Task 8: Event Views Contract (`POST /events/views`) & Full Verification

**Scope:** Verify `EventViewRepositoryImpl` / `EventViewCommandServiceImpl` idempotency on `batchId` and `(studentId, eventId)`, document the FE/BE integration contract if needed, and run the full test suite.

**Files:**
- Check/Modify (if gap found): `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/EventViewRepositoryImpl.java`
- Modify (if needed): `docs/bug/BACKEND_API_GAPS_AND_FIX_REQUEST.md` (append agreed `batchId` / viewport contract note for FE)

**Steps:**
1. Inspect `EventViewRepositoryImpl.record(...)` to confirm duplicate `batchId` / `(studentId, eventId)` handling is idempotent.
2. Document the agreed client `batchId` UUID + viewport/dwell threshold convention in `docs/bug/BACKEND_API_GAPS_AND_FIX_REQUEST.md` (Section 2.5).
3. Run full `mvn test` across the entire repository.

**Verify & Commit:**
```bash
mvn test
git add docs/bug/BACKEND_API_GAPS_AND_FIX_REQUEST.md
git commit -m "docs(event-views): document batchId and viewport contract after full test pass"
```
