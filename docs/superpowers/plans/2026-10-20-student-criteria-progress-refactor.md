# Student Criteria Progress Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the student criteria progress tracking implementation to resolve state overwrites, eliminate batch N+1 query bottlenecks, enforce strict DDD API and Application DTO/Mapper encapsulation, implement missing single-criterion progress queries, and add comprehensive unit tests.

**Architecture:** Domain-Driven Design (DDD) layered architecture (API -> Application -> Domain <- Infrastructure). Entities maintain business invariants; repository interfaces reside in Domain and implementations in Infrastructure; Application orchestrates workflows and in-memory progress calculations; API handles HTTP presentation, DTO mapping, and role-based access control.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Data JPA, MapStruct, PostgreSQL, JUnit 5, Mockito.

**Spec:** `docs/artifact/student-criteria-progress-implementation-plan.md`

---

## Global Constraints

- Domain layer (`ddd-domain`) must not depend on Application (`ddd-application`) or Infrastructure (`ddd-infrastructure`).
- API layer (`ddd-api`) must not leak Application DTOs directly; all controller responses must wrap API Response DTOs using MapStruct API Mappers.
- Application layer services must use MapStruct DTO Mappers rather than hand-coded private mapping boilerplate.
- Existing database constraints (`uq_student_criteria_progress`, `ck_progress_source`) and soft-delete/audit patterns must be respected.
- An `EVENT_CHECKOUT` progress record must never be downgraded or overwritten by a manual tick or untick.

## Review Focus

1. **EVENT_CHECKOUT protection on manual update:** Calling `PATCH /student/criteria/{criteriaId}/progress` with `completed = true` when progress is already completed via `EVENT_CHECKOUT` must not overwrite the completion source to `MANUAL` or wipe `sourceEventId`.
2. **Batch checkout efficiency:** Marking criteria completion during batch checkout for $N$ attendees must resolve event, semester, academic year, and criterion metadata in constant $O(1)$ queries rather than $O(N)$.
3. **Inactive/Non-leaf criteria guards:** Manual progress updates on non-leaf criteria or inactive criteria must fail with clear business exceptions.
4. **DTO and Mapper encapsulation:** Every layer boundary must have its corresponding DTO and MapStruct mapper (`AcademicYearResponseDto`, `AcademicYearApiMapper`, `AcademicYearDtoMapper`).
5. **Parent completion thresholds:** Parents with `required_children = 1`, `2`, or `null` (all children) must evaluate status strictly based on their active children's completed states.
6. **Role boundary enforcement:** Non-student roles (e.g., `ADMIN`, `FANPAGE_ADMIN`) must be denied access to student criteria self-progress endpoints.

---

## Detailed DTO & Mapper Architecture Matrix

| Layer | Type | Package | File | Role |
| :--- | :--- | :--- | :--- | :--- |
| **API** | Response DTO | `com.ddd.api.dto.academicyear.res` | `AcademicYearResponseDto.java` | **NEW**: Exposes Academic Year fields cleanly to HTTP clients |
| **API** | Mapper | `com.ddd.api.mapper` | `AcademicYearApiMapper.java` | **NEW**: Maps `AcademicYearDto` <-> `AcademicYearResponseDto` |
| **API** | Mapper | `com.ddd.api.mapper` | `StudentCriteriaProgressApiMapper.java` | **UPDATE**: Add single `CriteriaProgressNodeResponseDto` mapper |
| **Application** | Mapper | `com.ddd.application.mapper` | `AcademicYearDtoMapper.java` | **NEW**: Maps `AcademicYear` <-> `AcademicYearDto` (replaces duplicate private methods) |
| **Infrastructure** | Mapper | `com.ddd.infrastructure.mapper` | `AcademicYearMapper.java` | **UPDATE**: Add `toDomainList` collection mapping |
| **Infrastructure** | Mapper | `com.ddd.infrastructure.mapper` | `StudentCriteriaProgressMapper.java` | **UPDATE**: Add `toJpaEntityList` collection mapping |

---

## File Structure

```
ddd-api/
├── controller/
│   ├── AcademicYearController.java                   (Modify: return AcademicYearResponseDto via mapper)
│   └── StudentCriteriaProgressController.java         (Modify: add PreAuthorize, add GET /criteria/{criteriaId}/progress)
├── dto/
│   └── academicyear/res/
│       └── AcademicYearResponseDto.java              (Create: API Response DTO)
└── mapper/
    ├── AcademicYearApiMapper.java                    (Create: MapStruct mapper for AcademicYearDto -> AcademicYearResponseDto)
    └── StudentCriteriaProgressApiMapper.java         (Modify: support single node mapping)

ddd-application/
├── mapper/
│   └── AcademicYearDtoMapper.java                    (Create: MapStruct mapper for AcademicYear <-> AcademicYearDto)
└── service/
    ├── AcademicYearService.java                      (Modify: add resolveOrCreateForSemester)
    ├── impl/
    │   ├── AcademicYearServiceImpl.java              (Modify: inject AcademicYearDtoMapper, centralize resolveOrCreateForSemester)
    │   └── SemesterServiceImpl.java                  (Modify: delegate academic year resolution to AcademicYearService)
    ├── criteria/
    │   └── StudentCriteriaProgressService.java        (Modify: add batch markCompletedFromEvent, add getCriteriaProgress)
    └── criteria/impl/
        └── StudentCriteriaProgressServiceImpl.java   (Modify: use AcademicYearDtoMapper, batch event checkout, event checkout overwrite guard)
    └── registration/impl/
        └── RegistrationCommandServiceImpl.java       (Modify: use batch markCompletedFromEvent in fanpageMemberCheckOut)

ddd-domain/
└── repository/
    └── StudentCriteriaProgressRepository.java        (Modify: add findByUserIdsAndCriteriaIdAndAcademicYearId, saveAll)

ddd-infrastructure/
├── mapper/
│   ├── AcademicYearMapper.java                       (Modify: add toDomainList)
│   └── StudentCriteriaProgressMapper.java            (Modify: add toJpaEntityList)
└── repository/
    ├── StudentCriteriaProgressRepositoryImpl.java    (Modify: implement batch repository methods)
    └── jpaRepository/
        └── StudentCriteriaProgressJpaRepository.java (Modify: add batch find query)

tests/
└── ddd-application/
    └── src/test/java/com/ddd/application/service/criteria/impl/
        └── StudentCriteriaProgressServiceImplTest.java (Create: comprehensive unit tests)
```

---

## Tasks

### Task 1: Create Application & Infrastructure Mappers and Consolidate Academic Year Resolution

**Files:**
- Create: `ddd-application/src/main/java/com/ddd/application/mapper/AcademicYearDtoMapper.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/AcademicYearMapper.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/AcademicYearRepositoryImpl.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/AcademicYearService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/impl/AcademicYearServiceImpl.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/impl/SemesterServiceImpl.java`

- [x] **Step 1: Create `AcademicYearDtoMapper` in `ddd-application`**
  ```java
  package com.ddd.application.mapper;

  import com.ddd.application.dto.academicyear.AcademicYearDto;
  import com.ddd.domain.model.AcademicYear;
  import org.mapstruct.Mapper;

  import java.util.List;

  @Mapper(componentModel = "spring")
  public interface AcademicYearDtoMapper {
      AcademicYearDto toDto(AcademicYear academicYear);
      AcademicYear toEntity(AcademicYearDto dto);
      List<AcademicYearDto> toDtoList(List<AcademicYear> academicYears);
  }
  ```
- [x] **Step 2: Add collection mapping in `AcademicYearMapper` & update repository**
  - Add `List<AcademicYear> toDomainList(List<AcademicYearJpaEntity> entities)` to `AcademicYearMapper`.
  - In `AcademicYearRepositoryImpl.findAllActive()`: return `mapper.toDomainList(...)`.
- [x] **Step 3: Define and implement canonical `resolveOrCreateForSemester` in `AcademicYearService`**
  - Add to `AcademicYearService`:
    ```java
    AcademicYear resolveOrCreateForSemester(Semester semester);
    ```
  - In `AcademicYearServiceImpl`:
    - Inject `AcademicYearDtoMapper`.
    - Replace private `toDto` with mapper calls.
    - Implement `resolveOrCreateForSemester`: check `semester.getAcademicYearId()`, lookup by `academicYear` code, or create and link to semester.
- [x] **Step 4: Update `SemesterServiceImpl`**
  - Replace private duplicate `ensureAcademicYear` in `SemesterServiceImpl` with `academicYearService.resolveOrCreateForSemester(semester)`.
- [x] **Step 5: Verify build with test-compile**
  - Run `./mvnw test-compile`.

---

### Task 2: Fix Manual Progress Overwrite & Inactive Criteria Guard

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteria/impl/StudentCriteriaProgressServiceImpl.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/criteria/impl/StudentCriteriaProgressServiceImplTest.java`

- [x] **Step 1: Write failing unit test in `StudentCriteriaProgressServiceImplTest`**
  - Test `setManualProgress_whenAlreadyEventCheckout_doesNotOverwriteSourceOrTimestamp`.
  - Test `setManualProgress_whenCriteriaInactive_throwsBusinessException`.
  - Test `setManualProgress_whenNonLeafCriteria_throwsBusinessException`.
- [x] **Step 2: Update `StudentCriteriaProgressServiceImpl.java`**
  - Inject `AcademicYearDtoMapper` and use it instead of private `toAcademicYearDto`.
  - Reuse `academicYearService.resolveOrCreateForSemester(semester)` instead of private `resolveAcademicYear`.
  - Add check: `if (!criteria.isActive()) throw new BusinessException("Cannot set progress for inactive criteria");`
  - In `setManualProgress()`:
    ```java
    if (progress.isCompleted() && progress.getCompletionSource() == CriteriaCompletionSourceEnum.EVENT_CHECKOUT) {
        if (!completed) {
            throw new BusinessException("A criterion completed by event checkout cannot be manually unchecked");
        }
        return; // Already completed via verified event; preserve event metadata and source
    }
    ```
- [x] **Step 3: Run unit tests to verify they pass**
  - Run `./mvnw test -pl ddd-application -Dtest=StudentCriteriaProgressServiceImplTest`.

---

### Task 3: Optimize Event Checkout with Batch Completion

**Files:**
- Modify: `ddd-domain/src/main/java/com/ddd/domain/repository/StudentCriteriaProgressRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/StudentCriteriaProgressMapper.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/StudentCriteriaProgressJpaRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/StudentCriteriaProgressRepositoryImpl.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteria/StudentCriteriaProgressService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteria/impl/StudentCriteriaProgressServiceImpl.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImpl.java`

- [x] **Step 1: Add batch methods in domain repository, infrastructure mapper, and JPA repository**
  - In `StudentCriteriaProgressRepository`:
    ```java
    List<StudentCriteriaProgress> findByUserIdsAndCriteriaIdAndAcademicYearId(
            Collection<Long> userIds, Long criteriaId, Long academicYearId);
    List<StudentCriteriaProgress> saveAll(List<StudentCriteriaProgress> progresses);
    ```
  - In `StudentCriteriaProgressMapper`:
    ```java
    List<StudentCriteriaProgressJpaEntity> toJpaEntityList(List<StudentCriteriaProgress> domains);
    ```
  - In `StudentCriteriaProgressJpaRepository`:
    ```java
    List<StudentCriteriaProgressJpaEntity> findAllByUserIdInAndCriteriaIdAndAcademicYearId(
            Collection<Long> userIds, Long criteriaId, Long academicYearId);
    ```
  - Implement batch methods in `StudentCriteriaProgressRepositoryImpl`.
- [x] **Step 2: Add batch method in `StudentCriteriaProgressService` & implementation**
  - In `StudentCriteriaProgressService`:
    ```java
    void markCompletedFromEvent(Long userId, Long eventId);
    void markCompletedFromEvent(List<Long> userIds, Long eventId);
    ```
  - In `StudentCriteriaProgressServiceImpl`:
    - In `markCompletedFromEvent(Long userId, Long eventId)`: delegate to `markCompletedFromEvent(List.of(userId), eventId)`.
    - In `markCompletedFromEvent(List<Long> userIds, Long eventId)`:
      - Resolve `Event`, `Semester`, `AcademicYear`, and leaf `Criteria` once.
      - Bulk-fetch existing progress records for all `userIds`.
      - Index existing progresses by `userId`.
      - For each user in `userIds`, retrieve existing or instantiate new `StudentCriteriaProgress`.
      - Call `.complete(EVENT_CHECKOUT, eventId, now)`.
      - Call `progressRepository.saveAll(toSave)`.
- [x] **Step 3: Update `RegistrationCommandServiceImpl` batch checkout**
  - In `fanpageMemberCheckOut`: replace `studentUserIds.forEach(...)` with `studentCriteriaProgressService.markCompletedFromEvent(studentUserIds, eventId);`.
- [x] **Step 4: Add unit tests for batch event completion in `StudentCriteriaProgressServiceImplTest`**
  - Verify bulk resolution and saving with constant query count.
  - Run `./mvnw test -pl ddd-application -Dtest=StudentCriteriaProgressServiceImplTest`.

---

### Task 4: API Layer Encapsulation (AcademicYear DTO/Mapper) & Single Criterion Progress Query

**Files:**
- Create: `ddd-api/src/main/java/com/ddd/api/dto/academicyear/res/AcademicYearResponseDto.java`
- Create: `ddd-api/src/main/java/com/ddd/api/mapper/AcademicYearApiMapper.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/AcademicYearController.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteria/StudentCriteriaProgressService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteria/impl/StudentCriteriaProgressServiceImpl.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/StudentCriteriaProgressController.java`

- [x] **Step 1: Create `AcademicYearResponseDto` and `AcademicYearApiMapper`**
  - In `com.ddd.api.dto.academicyear.res`:
    ```java
    public record AcademicYearResponseDto(
            Long id,
            String code,
            LocalDate startDate,
            LocalDate endDate,
            boolean isActive
    ) implements Serializable {}
    ```
  - In `com.ddd.api.mapper`:
    ```java
    @Mapper(componentModel = "spring")
    public interface AcademicYearApiMapper {
        AcademicYearResponseDto toResponseDto(AcademicYearDto dto);
        List<AcademicYearResponseDto> toResponseDtoList(List<AcademicYearDto> dtos);
    }
    ```
- [x] **Step 2: Update `AcademicYearController`**
  - Inject `AcademicYearApiMapper`.
  - Update endpoints to return `BaseResponse<AcademicYearResponseDto>` and `BaseResponse<List<AcademicYearResponseDto>>`.
- [x] **Step 3: Implement `GET /student/criteria/{criteriaId}/progress`**
  - In `StudentCriteriaProgressService`:
    ```java
    CriteriaProgressNodeDto getCriteriaProgress(String email, Long criteriaId, Long academicYearId);
    ```
  - In `StudentCriteriaProgressServiceImpl`:
    - Leverage existing tree building logic to find and return the requested criterion node.
  - In `StudentCriteriaProgressController`:
    ```java
    @GetMapping("/criteria/{criteriaId}/progress")
    public BaseResponse<CriteriaProgressNodeResponseDto> getCriteriaProgress(
            Authentication authentication,
            @PathVariable Long criteriaId,
            @RequestParam(required = false) Long academicYearId) {
        CriteriaProgressNodeDto progress = progressService.getCriteriaProgress(
                authentication.getName(), criteriaId, academicYearId);
        return BaseResponse.of(progressApiMapper.toResponseDto(progress));
    }
    ```
- [x] **Step 4: Add `@PreAuthorize("hasRole('STUDENT')")` on `StudentCriteriaProgressController`**
  - Restrict `/student/**` endpoints to students only.

---

### Task 5: Comprehensive Unit Tests & Documentation Sync

**Files:**
- Create/Modify: `ddd-application/src/test/java/com/ddd/application/service/criteria/impl/StudentCriteriaProgressServiceImplTest.java`
- Modify: `docs/api_documentation.md`

- [x] **Step 1: Add full test matrix in `StudentCriteriaProgressServiceImplTest`**
  - Full tree calculation with SV5T root and 5 main criteria.
  - Parent criterion evaluation with `requiredChildren = 1`.
  - Parent criterion evaluation with `requiredChildren = 2`.
  - Parent criterion evaluation with `requiredChildren = null` (all children required).
  - Overall completed leaf count and main criteria count verification.
  - Manual toggle (complete / uncomplete).
  - Prevention of manual unchecking when `EVENT_CHECKOUT`.
  - Single criterion progress retrieval.
  - Batch event checkout completion.
- [x] **Step 2: Run all tests across modules**
  - Run `./mvnw test -pl ddd-domain,ddd-application,ddd-api,ddd-infrastructure`.
- [x] **Step 3: Update API documentation**
  - Sync `docs/api_documentation.md` with `GET /student/criteria/{criteriaId}/progress`, `AcademicYearResponseDto`, and security role specifications.
