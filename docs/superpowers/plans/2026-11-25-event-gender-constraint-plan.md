# Event Gender Constraint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `genderConstraint` flag to events and enforce per-gender (`MALE` / `FEMALE`) registration quotas under the existing event-level pessimistic lock, while preserving total capacity invariants and backward compatibility for unconstrained events.

**Architecture:** Extend `Event` / `EventJpaEntity` and the Event API/Application DTOs with `genderConstraint` (default `false`). Expose a non-cancelled per-gender registration count query through `RegistrationRepository`. In `RegistrationCommandServiceImpl.registerEvent()`, after acquiring the pessimistic event lock and validating duplicates, enforce `User.gender` (`MALE` or `FEMALE`) and check the gender-specific quota before checking total `capacity`. In `EventCommandServiceImpl.updateEvent()`, validate under the same event lock that `maleQuantity` and `femaleQuantity` are not lower than currently occupied male/female registrations when `genderConstraint` is `true`.

**Tech Stack:** Spring Boot 4.1.1, Spring Data JPA/Hibernate, PostgreSQL, MapStruct, JUnit 5, Mockito.

**Spec:** User requirements and clarified design decisions (2026-11-25):
- `genderConstraint` defaults to `false`; existing events remain unconstrained.
- When `genderConstraint = true`, `User.gender` must be `MALE` or `FEMALE` (case-insensitive); `null` or `OTHER` is rejected with `EventRegistrationException`.
- Keep `maleQuantity + femaleQuantity <= capacity` when `genderConstraint = true` (allows reserved gender quotas plus optional unreserved slots).
- On `updateEvent`, when `genderConstraint = true` (including toggling `false -> true`), reject `maleQuantity < occupiedMale` or `femaleQuantity < occupiedFemale` with `BusinessException`.
- Gender quota exhaustion throws `EventGenderFullException` mapped to HTTP `409 Conflict` with `ErrorCodeEnum.EVENT_GENDER_FULL` (`EVENT.005`).

## Global Constraints

- Follow the event-first pessimistic locking protocol (`eventRepository.findByIdForUpdate(eventId)`) and `READ_COMMITTED` isolation established in `2026-11-24-registration-race-condition-refactoring.md`.
- Keep `ddd-domain` pure Java (zero Spring or JPA annotations).
- Preserve `PRIMARY KEY (user_id, event_id)` on `registrations` and reuse `CANCELLED` rows on re-registration.
- Occupied registrations for a gender are `COUNT(r WHERE r.event.id = :eventId AND r.status <> 'CANCELLED' AND UPPER(r.user.gender) = UPPER(:gender))`.
- Check duplicates before checking gender quota or total capacity so repeated requests retain `DuplicateResourceException`.
- Do not modify `events.capacity`, `events.male_quantity`, or `events.female_quantity` during registration or cancellation.

## Review Focus

- `User.gender` is `null` or `"OTHER"` when `genderConstraint = true`: must fail with `EventRegistrationException` prompting profile gender update, without saving a registration.
- `maleQuantity = 0` or `femaleQuantity = 0` when `genderConstraint = true`: zero means zero allowed slots for that gender (must reject with `EventGenderFullException`), not unlimited.
- Toggling `genderConstraint` from `false` to `true` on `updateEvent` when registrations already exist: must validate current occupied male and female counts against the new `maleQuantity` and `femaleQuantity` under the event lock.
- Concurrent final-slot competition for the last male slot (while female slots and total capacity still have room): only one male student succeeds; others receive `EventGenderFullException` (`EVENT.005`), while an eligible female student can still register.
- Re-registration of a `CANCELLED` student when `genderConstraint = true`: consumes a slot in that student's gender quota and is rejected with `EventGenderFullException` if that gender's quota is full.

---

### Task 1: Schema, Domain Model, JPA Entity, and Per-Gender Count Port

**Files:**
- Modify: `ddd-infrastructure/src/main/resources/database/schema.sql`
- Create: `ddd-infrastructure/src/main/resources/database/migration/event-gender-constraint-migration.sql`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/model/Event.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/repository/RegistrationRepository.java`
- Create: `ddd-domain/src/main/java/com/ddd/domain/exception/EventGenderFullException.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/EventJpaEntity.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/RegistrationJpaRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/RegistrationRepositoryImpl.java`
- Modify: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/mapper/EventMapperTest.java`
- Modify: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/repository/RegistrationRepositoryImplTest.java`

**Interfaces:**
- Produces: `boolean Event.isGenderConstraint()` / `void Event.setGenderConstraint(boolean)` (default `false`).
- Produces: `long RegistrationRepository.countActiveRegistrationsByEventIdAndGender(Long eventId, String gender)`.
- Produces: `EventGenderFullException extends EventRegistrationException` with constructors `()` and `(String message)`.

- [ ] **Step 1: Write failing tests for `EventMapper` (`genderConstraint` round-trip) and `RegistrationRepositoryImpl.countActiveRegistrationsByEventIdAndGender`**
  - In `EventMapperTest.java`, add `genderConstraintSurvivesMappingToJpaAndBack()`.
  - In `RegistrationRepositoryImplTest.java`, add `countActiveRegistrationsByEventIdAndGender_delegatesToJpa()`.
- [ ] **Step 2: Run infrastructure tests to verify failure**
  - Run: `./mvnw -pl ddd-infrastructure -am -Dtest=EventMapperTest,RegistrationRepositoryImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Expected: Compilation failure because `genderConstraint` and `countActiveRegistrationsByEventIdAndGender` do not exist yet.
- [ ] **Step 3: Add `gender_constraint` to `schema.sql` and `event-gender-constraint-migration.sql`**
  - Add `gender_constraint BOOLEAN NOT NULL DEFAULT FALSE` to `events` in `schema.sql` and `ALTER TABLE events ADD COLUMN IF NOT EXISTS gender_constraint BOOLEAN NOT NULL DEFAULT FALSE;` in `event-gender-constraint-migration.sql`.
- [ ] **Step 4: Update `Event`, `EventJpaEntity`, and create `EventGenderFullException`**
  - Add `private boolean genderConstraint = false;` to `Event.java` and `@Column(name = "gender_constraint", nullable = false) private boolean genderConstraint = false;` to `EventJpaEntity.java`.
  - Create `EventGenderFullException extends EventRegistrationException` in `ddd-domain`.
- [ ] **Step 5: Add `countActiveRegistrationsByEventIdAndGender` to `RegistrationJpaRepository`, `RegistrationRepository`, and `RegistrationRepositoryImpl`**
  - JPQL in `RegistrationJpaRepository`:
    `SELECT COUNT(r) FROM RegistrationJpaEntity r JOIN r.user u WHERE r.event.id = :eventId AND r.status <> com.ddd.domain.enums.RegistrationStatusEnum.CANCELLED AND UPPER(u.gender) = UPPER(:gender)`
- [ ] **Step 6: Run infrastructure tests to verify pass**
  - Run: `./mvnw -pl ddd-infrastructure -am -Dtest=EventMapperTest,RegistrationRepositoryImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Expected: All tests pass.
- [ ] **Step 7: Commit Task 1**

---

### Task 2: Event DTOs, Mappers, and `EventCommandServiceImpl` Quota Validation

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/event/req/EventCreateRequestDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/event/req/EventUpdateRequestDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/event/EventCreateDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/event/EventUpdateDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/event/impl/EventCommandServiceImpl.java`
- Modify: `ddd-application/src/test/java/com/ddd/application/service/event/impl/EventCapacityConcurrencyPolicyTest.java`
- Modify: `ddd-application/src/test/java/com/ddd/application/service/event/impl/EventCommandServiceImplTest.java`
- Modify: `ddd-api/src/test/java/com/ddd/api/mapper/EventApiMapperTest.java` (verify existing tests compile/pass with updated records)

**Interfaces:**
- Consumes: `RegistrationRepository.countActiveRegistrationsByEventIdAndGender(Long eventId, String gender)` and `Event.isGenderConstraint()`.
- Produces: `genderConstraint` field propagated across `EventCreateRequestDto` -> `EventCreateDto` -> `Event` -> `EventJpaEntity` -> `EventInfoDto` -> `EventResponseDto`, and `EventUpdateRequestDto` -> `EventUpdateDto` -> `Event`.

- [ ] **Step 1: Write failing tests in `EventCapacityConcurrencyPolicyTest` and `EventCommandServiceImplTest`**
  - Test `updateEvent` when `genderConstraint = true`:
    - Rejects `maleQuantity < countActiveRegistrationsByEventIdAndGender(id, "MALE")` with `BusinessException("Male quantity cannot be lower than current male registrations.")`.
    - Rejects `femaleQuantity < countActiveRegistrationsByEventIdAndGender(id, "FEMALE")` with `BusinessException("Female quantity cannot be lower than current female registrations.")`.
    - Rejects toggling `genderConstraint` from `false` to `true` when existing male or female registrations exceed the proposed quotas.
    - Allows `maleQuantity >= occupiedMale` and `femaleQuantity >= occupiedFemale` (and skips gender count checks when `genderConstraint = false`).
- [ ] **Step 2: Run `EventCapacityConcurrencyPolicyTest` and `EventCommandServiceImplTest` to verify failure**
  - Run: `./mvnw -pl ddd-application -am -Dtest=EventCapacityConcurrencyPolicyTest,EventCommandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Expected: Compilation/test failure.
- [ ] **Step 3: Propagate `genderConstraint` across Request/App/Info/Response DTOs**
  - Add `Boolean genderConstraint` (defaulting `null -> false` in compact constructors) to `EventCreateRequestDto` and `EventUpdateRequestDto`.
  - Add `Boolean genderConstraint` to `EventCreateDto` and `EventUpdateDto`.
  - Add `boolean genderConstraint` to `EventInfoDto` (updating `withRegistrationInfo` and secondary constructor) and `EventResponseDto` (updating secondary constructor).
  - Fix any existing test fixture calls constructing `EventCreateDto`, `EventUpdateDto`, `EventInfoDto`, or `EventResponseDto`.
- [ ] **Step 4: Update `EventCommandServiceImpl.updateEvent`**
  - After `findByIdForUpdate(id)`, mapping, and `validateCapacityAndQuantities()` + total `capacity >= occupied` check:
  - If `event.isGenderConstraint()`:
    - Check `occupiedMale > event.getMaleQuantity()` -> throw `BusinessException("Male quantity cannot be lower than current male registrations.")`.
    - Check `occupiedFemale > event.getFemaleQuantity()` -> throw `BusinessException("Female quantity cannot be lower than current female registrations.")`.
- [ ] **Step 5: Run application & API mapper tests to verify pass**
  - Run: `./mvnw -pl ddd-api -am -Dtest=EventCapacityConcurrencyPolicyTest,EventCommandServiceImplTest,EventApiMapperTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Expected: All tests pass.
- [ ] **Step 6: Commit Task 2**

---

### Task 3: Enforce Gender Constraint in `RegistrationCommandServiceImpl`

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImpl.java`
- Modify: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImplTest.java`

**Interfaces:**
- Consumes: `Event.isGenderConstraint()`, `Event.getMaleQuantity()`, `Event.getFemaleQuantity()`, `RegistrationRepository.countActiveRegistrationsByEventIdAndGender(Long, String)`, `EventGenderFullException`.
- Preserves: `void registerEvent(Long eventId)`.

- [ ] **Step 1: Write failing unit tests in `RegistrationCommandServiceImplTest`**
  - When `genderConstraint = false`: `OTHER`/`null` gender and male/female students can register up to `capacity` without checking gender counts (`countActiveRegistrationsByEventIdAndGender` never called).
  - When `genderConstraint = true`:
    - Student with `gender = null` or `"OTHER"` throws `EventRegistrationException` ("Please update your gender to Male or Female before registering for this event!") before saving.
    - Duplicate non-cancelled registration still throws `DuplicateResourceException` even if that gender's quota is full.
    - Male student succeeds when `occupiedMale < maleQuantity` (and `occupiedTotal < capacity`), including re-registration from `CANCELLED`.
    - Male student throws `EventGenderFullException` when `occupiedMale >= maleQuantity` (including `maleQuantity = 0`), even if `occupiedTotal < capacity` and female slots remain.
    - Female student succeeds when `occupiedFemale < femaleQuantity`, and throws `EventGenderFullException` when `occupiedFemale >= femaleQuantity`.
- [ ] **Step 2: Run `RegistrationCommandServiceImplTest` to verify failure**
  - Run: `./mvnw -pl ddd-application -am -Dtest=RegistrationCommandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Expected: New gender constraint tests fail.
- [ ] **Step 3: Implement gender constraint enforcement in `RegistrationCommandServiceImpl.registerEvent`**
  - After profile and school eligibility checks (and before or after duplicate check — check `User.gender` before duplicate check or right after profile validation, and check gender quota **after** duplicate check `isRegistered(user, eventId)` and **before** total capacity check):
  - If `event.isGenderConstraint()`:
    - Normalize `user.getGender()`: must be `"MALE"` or `"FEMALE"` (case-insensitive); otherwise throw `EventRegistrationException("Please update your gender to Male or Female before registering for this event!")`.
    - After `Registration reg = isRegistered(user, eventId);`:
      - Determine `quota = "MALE".equals(normalizedGender) ? event.getMaleQuantity() : event.getFemaleQuantity()`.
      - If `registrationRepository.countActiveRegistrationsByEventIdAndGender(eventId, normalizedGender) >= quota`, throw `new EventGenderFullException("Slots for " + normalizedGender.toLowerCase() + " participants are full.")`.
  - Keep existing total capacity check (`countActiveRegistrationsByEventId(eventId) >= event.getCapacity()`) immediately after.
- [ ] **Step 4: Run `RegistrationCommandServiceImplTest` to verify pass**
  - Run: `./mvnw -pl ddd-application -am -Dtest=RegistrationCommandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Expected: All tests pass.
- [ ] **Step 5: Commit Task 3**

---

### Task 4: API Error Code and Exception Handler for `EventGenderFullException`

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/common/enums/ErrorCodeEnum.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/exception/GlobalExceptionHandler.java`
- Modify: `ddd-api/src/test/java/com/ddd/api/exception/RegistrationConcurrencyExceptionHandlerTest.java`
- Modify: `ddd-api/src/test/java/com/ddd/api/controller/RegistrationControllerTest.java`

**Interfaces:**
- Consumes: `EventGenderFullException` from Task 1/3.
- Produces: HTTP `409 Conflict`, `exceptionCode = "EVENT.005"` (`ErrorCodeEnum.EVENT_GENDER_FULL`).

- [ ] **Step 1: Write failing handler and controller tests**
  - In `RegistrationConcurrencyExceptionHandlerTest`, add `handleEventGenderFullReturns409WithEvent005Code()`.
  - In `RegistrationControllerTest`, assert `EventGenderFullException` returns HTTP 409 with `$.exceptionCode == "EVENT.005"`.
- [ ] **Step 2: Run API tests to verify failure**
  - Run: `./mvnw -pl ddd-api -am -Dtest=RegistrationConcurrencyExceptionHandlerTest,RegistrationControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Expected: Fails because `EVENT_GENDER_FULL` and handler method are missing (currently falls back to `EventRegistrationException` `EVENT.002` 400).
- [ ] **Step 3: Add `EVENT_GENDER_FULL` to `ErrorCodeEnum` and `@ExceptionHandler(EventGenderFullException.class)` to `GlobalExceptionHandler`**
  - `EVENT_GENDER_FULL("EVENT.005", HttpStatus.CONFLICT, "Event slots for your gender are full.")`
  - Handler returns `wrapWithResponse(error.getCode(), message, error.getStatus())`.
- [ ] **Step 4: Run API tests to verify pass**
  - Run: `./mvnw -pl ddd-api -am -Dtest=RegistrationConcurrencyExceptionHandlerTest,RegistrationControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Expected: All tests pass.
- [ ] **Step 5: Commit Task 4**

---

### Task 5: PostgreSQL Concurrency Regression Coverage

**Files:**
- Modify: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationPostgresFixtures.java`
- Modify: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationConcurrencyIntegrationTest.java`

**Interfaces:**
- Consumes: Real PostgreSQL schema, `EventCommandService`, `RegistrationCommandService`, and `genderConstraint` changes from Tasks 1–4.

- [ ] **Step 1: Extend `RegistrationPostgresFixtures` with helpers for gendered users and gender-constrained events**
  - Add helper to set user gender (`MALE`, `FEMALE`, `OTHER`) and create an event with `(capacity, maleQuantity, femaleQuantity, genderConstraint)`.
- [ ] **Step 2: Add PostgreSQL concurrency tests in `RegistrationConcurrencyIntegrationTest`**
  - `concurrentMaleStudentsCompeteForFinalMaleSlot`: `capacity=4, maleQuantity=2, femaleQuantity=2, genderConstraint=true`, 1 male already registered; 5 male students race concurrently -> exactly 1 succeeds, 4 fail with `EventGenderFullException`, occupied male count is 2; a female student can still register afterward.
  - `genderConstraintRejectsOtherAndNullGender`: student with `OTHER` or `null` gender is rejected with `EventRegistrationException`.
  - `concurrentGenderQuotaReductionVsRegistration`: ordered lock test between `updateEvent` reducing `maleQuantity` and a male student registering -> never allows `occupiedMale > maleQuantity`.
- [ ] **Step 3: Run PostgreSQL integration tests and full suite**
  - Run: `REGISTRATION_TEST_JDBC_URL=... REGISTRATION_TEST_DB_USER=... REGISTRATION_TEST_DB_PASSWORD=... ./mvnw -pl ddd-application -am -Dtest=RegistrationConcurrencyIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Run full `./mvnw test` with database test environment variables configured.
  - Expected: All tests pass with zero leftover test schemas.
- [ ] **Step 4: Commit Task 5**
