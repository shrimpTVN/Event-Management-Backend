# SV5T Required Occurrences & Leaf Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Support multi-occurrence SV5T leaf criteria (`required_occurrences > 1`) across DB entities, admin CRUD/tree APIs, leaf-to-parent progress state-transition calculation, and student progress query responses.

**Architecture:** Map `criterias.required_occurrences` (already present in PostgreSQL) and add `student_criteria_progress.completed_occurrences` (`INTEGER NOT NULL DEFAULT 0`) across `ddd-domain` and `ddd-infrastructure`. In `StudentCriteriaProgressCommandServiceImpl`, leaf completion uses a state-transition check (`wasCompleted` vs `isNowCompleted = completedOccurrences >= requiredOccurrences`) so parent `completedChildrenCount` is incremented/decremented only when a leaf crosses the completion threshold, while retaining partial progress (`1/3`, `2/3`).

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, MapStruct, JUnit 5, Mockito, Maven multi-module (`ddd-domain`, `ddd-infrastructure`, `ddd-application`, `ddd-api`).

**Spec:** [BACKEND_SV5T_REQUIRED_OCCURRENCES_FIX(1).md](file:///home/shrimp/code/ddd/docs/bug/BACKEND_SV5T_REQUIRED_OCCURRENCES_FIX(1).md)

## Global Constraints

- Follow DDD module dependency rules in `architecture_conventions.md`: `ddd-domain` has zero Spring/JPA annotations; `ddd-api` never imports `ddd-infrastructure`.
- `Criteria.requiredOccurrences` defaults to `1` and must be positive (`> 0`); `Criteria.setRequiredOccurrences` throws `IllegalArgumentException` if `<= 0`.
- `StudentCriteriaProgress.completedOccurrences` defaults to `0` and must never be negative (`>= 0`).
- `CriteriaRequestDto.requiredOccurrences` uses `@Positive(message = "Required occurrences must be positive")`.
- Do not duplicate `wasCompleted`/`isNowCompleted` in entities; they are transient local booleans inside `StudentCriteriaProgressCommandServiceImpl`.

## Review Focus

- Leaf with `requiredOccurrences = 3` receiving a 4th completion (`3/3 -> 4/3`) must increment `completedOccurrences` to `4` without incrementing parent `completedChildrenCount` a second time.
- Leaf with `requiredOccurrences = 3` losing an occurrence when above threshold (`4/3 -> 3/3`) must stay `isCompleted = true` without decrementing parent `completedChildrenCount`.
- Leaf with `requiredOccurrences = 3` losing an occurrence at threshold (`3/3 -> 2/3`) must transition to `isCompleted = false`, keep the progress row with `completedOccurrences = 2`, and decrement parent `completedChildrenCount` by `1`.
- Leaf with `requiredOccurrences = 3` losing an occurrence while uncompleted (`2/3 -> 1/3`) must decrement `completedOccurrences` to `1`, keep the row, and NOT decrement parent `completedChildrenCount`.
- Existing criteria or `Criteria` with `requiredOccurrences == null` must safely default to `1` in both `createCriteria` and `bulkMarkCompleted` / `markUncompleted`.

---

### Task 1: Domain, Infrastructure Entities & Migration SQL

**Files:**
- Modify: `ddd-domain/src/main/java/com/ddd/domain/model/Criteria.java:14-37`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/CriteriaJpaEntity.java:33-39`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/model/StudentCriteriaProgress.java:17-31`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/StudentCriteriaProgressJpaEntity.java:43-53`
- Create: `ddd-infrastructure/src/main/resources/database/migration/student-criteria-progress-completed-occurrences-migration.sql`

**Interfaces:**
- Produces:
  - `Criteria.getRequiredOccurrences() : Integer`, `Criteria.setRequiredOccurrences(Integer) : void`
  - `CriteriaJpaEntity.getRequiredOccurrences() : Integer`, `CriteriaJpaEntity.setRequiredOccurrences(Integer) : void`
  - `StudentCriteriaProgress.getCompletedOccurrences() : Integer`, `StudentCriteriaProgress.setCompletedOccurrences(Integer) : void`
  - `StudentCriteriaProgressJpaEntity.getCompletedOccurrences() : Integer`, `StudentCriteriaProgressJpaEntity.setCompletedOccurrences(Integer) : void`

- [ ] **Step 1: Add `requiredOccurrences` to `Criteria.java` and `CriteriaJpaEntity.java`**
  - In `Criteria.java`: add `private Integer requiredOccurrences = 1;` and `setRequiredOccurrences(Integer requiredOccurrences)` throwing `IllegalArgumentException("Required occurrences must be positive")` when `requiredOccurrences != null && requiredOccurrences <= 0`.
  - In `CriteriaJpaEntity.java`: add `@Column(name = "required_occurrences", nullable = false) private Integer requiredOccurrences = 1;`.

- [ ] **Step 2: Add `completedOccurrences` to `StudentCriteriaProgress.java` and `StudentCriteriaProgressJpaEntity.java`**
  - In `StudentCriteriaProgress.java`: add `private Integer completedOccurrences = 0;`, initialize `this.completedOccurrences = 0;` in the 3-arg constructor, and add `setCompletedOccurrences(Integer)` guarding `Math.max(0, ...)` when non-null.
  - In `StudentCriteriaProgressJpaEntity.java`: add `@ColumnDefault("0") @Column(name = "completed_occurrences", nullable = false) private Integer completedOccurrences = 0;`.

- [ ] **Step 3: Create migration SQL `student-criteria-progress-completed-occurrences-migration.sql`**
  - Add `completed_occurrences INTEGER NOT NULL DEFAULT 0` to `student_criteria_progress`, and backfill existing completed leaf rows (`is_completed = TRUE` and not a parent) to `c.required_occurrences`.

- [ ] **Step 4: Verify `ddd-domain` and `ddd-infrastructure` compile**
  - Run: `mvn test-compile -pl ddd-infrastructure -am`
  - Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**
  - `git add ddd-domain/src/main/java/com/ddd/domain/model/Criteria.java ddd-domain/src/main/java/com/ddd/domain/model/StudentCriteriaProgress.java ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/CriteriaJpaEntity.java ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/StudentCriteriaProgressJpaEntity.java ddd-infrastructure/src/main/resources/database/migration/student-criteria-progress-completed-occurrences-migration.sql`
  - `git commit -m "feat(criteria): add requiredOccurrences and completedOccurrences to domain and entities"`

---

### Task 2: Criteria CRUD & Tree DTOs (`requiredOccurrences` End-to-End)

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/criteria/CriteriaDto.java:3-14`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/criteria/CriteriaTreeNodeDto.java:5-15`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteria/impl/CriteriaServiceImpl.java:31-45,134-144`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/criteria/req/CriteriaRequestDto.java:5-20`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/criteria/res/CriteriaResponseDto.java:3-14`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/criteria/res/CriteriaTreeNodeResponseDto.java:7-17`
- Test: `ddd-application/src/test/java/com/ddd/application/service/criteria/impl/CriteriaServiceImplTest.java`

**Interfaces:**
- Consumes: `Criteria.getRequiredOccurrences()`, `Criteria.setRequiredOccurrences(Integer)` from Task 1.
- Produces: Updated `CriteriaDto`, `CriteriaTreeNodeDto`, `CriteriaRequestDto`, `CriteriaResponseDto`, `CriteriaTreeNodeResponseDto` records including `Integer requiredOccurrences`.

- [ ] **Step 1: Write failing unit tests in `CriteriaServiceImplTest.java`**
  - `createCriteria_whenRequiredOccurrencesNull_shouldDefaultToOne()`
  - `getCriteriaTree_shouldIncludeRequiredOccurrencesOnNodes()` (asserting `node.requiredOccurrences() == 3`)

- [ ] **Step 2: Run test to verify it fails**
  - Run: `mvn test -pl ddd-application -am -Dtest=CriteriaServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: FAIL (missing `requiredOccurrences` field/component on `CriteriaDto` / `CriteriaTreeNodeDto`).

- [ ] **Step 3: Update Criteria DTOs and `CriteriaServiceImpl.java`**
  - Add `Integer requiredOccurrences` after `requiredChildren` in `CriteriaDto`, `CriteriaResponseDto`, `CriteriaTreeNodeDto`, `CriteriaTreeNodeResponseDto`, and `@Positive(message = "Required occurrences must be positive") Integer requiredOccurrences` in `CriteriaRequestDto`.
  - In `CriteriaServiceImpl.createCriteria`: if `criteria.getRequiredOccurrences() == null`, set `1`.
  - In `CriteriaServiceImpl.buildTreeNodeTopDown`: pass `current.getRequiredOccurrences() != null ? current.getRequiredOccurrences() : 1` into `new CriteriaTreeNodeDto(...)`.

- [ ] **Step 4: Run test and compile `ddd-api`**
  - Run: `mvn test-compile -pl ddd-api -am && mvn test -pl ddd-application -am -Dtest=CriteriaServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: `BUILD SUCCESS`, 2/2 tests PASS.

- [ ] **Step 5: Commit**
  - `git add ddd-application/src/main/java/com/ddd/application/dto/criteria/ ddd-application/src/main/java/com/ddd/application/service/criteria/impl/CriteriaServiceImpl.java ddd-api/src/main/java/com/ddd/api/dto/criteria/ ddd-application/src/test/java/com/ddd/application/service/criteria/impl/CriteriaServiceImplTest.java`
  - `git commit -m "feat(criteria): expose requiredOccurrences in criteria DTOs and tree"`

---

### Task 3: Leaf Occurrence State-Transition Calculation in `StudentCriteriaProgressCommandServiceImpl`

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImpl.java:34-232`
- Test: `ddd-application/src/test/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImplTest.java`

**Interfaces:**
- Consumes: `Criteria.getRequiredOccurrences()`, `StudentCriteriaProgress.getCompletedOccurrences()`, `StudentCriteriaProgress.setCompletedOccurrences(Integer)` from Task 1.
- Produces: Multi-occurrence-aware `bulkMarkCompleted(List<Long>, Long, Long)` and `markUncompleted(Long, Long, Long)`.

- [ ] **Step 1: Write failing unit tests in `StudentCriteriaProgressCommandServiceImplTest.java`**
  - `bulkMarkCompleted_whenRequiredOccurrencesIsThree_shouldTrackPartialAndCompleteOnlyOnThird()`:
    - Call 1: `completedOccurrences == 1`, `isCompleted == false`, parent `completedChildrenCount` not incremented.
    - Call 3: `completedOccurrences == 3`, `isCompleted == true`, parent `completedChildrenCount == 1`.
    - Call 4: `completedOccurrences == 4`, `isCompleted == true`, parent `completedChildrenCount` remains `1` (not incremented again).
  - `markUncompleted_whenOccurrencesDropFromFourToThree_shouldKeepCompletedAndNotDecrementParent()`
  - `markUncompleted_whenOccurrencesDropFromThreeToTwo_shouldMarkUncompletedKeepRowAndDecrementParent()`
  - `markUncompleted_whenOccurrencesDropFromTwoToOne_shouldDecrementOccurrencesAndNotDecrementParent()`

- [ ] **Step 2: Run tests to verify they fail**
  - Run: `mvn test -pl ddd-application -am -Dtest=StudentCriteriaProgressCommandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: FAIL on new multi-occurrence assertions.

- [ ] **Step 3: Refactor `bulkMarkCompleted` and `markUncompleted` in `StudentCriteriaProgressCommandServiceImpl.java`**
  - In `bulkMarkCompleted`:
    - Load `leafCriteria` and `requiredOccurrences = leafCriteria.getRequiredOccurrences() != null ? leafCriteria.getRequiredOccurrences() : 1`.
    - Load existing `StudentCriteriaProgress` records for `(distinctUserIds, criteriaId, academicYearId)` instead of skipping all existing rows.
    - For each user: increment `completedOccurrences`, compute `wasCompleted = Boolean.TRUE.equals(leafProgress.getIsCompleted())` and `isNowCompleted = newOccurrences >= requiredOccurrences`, set `isCompleted = isNowCompleted` (and `completedAt = now` on transition).
    - Collect only users where `!wasCompleted && isNowCompleted` into `newlyCompletedUserIds` for parent propagation (`calculateUpdateProgressList`).
  - In `markUncompleted`:
    - Load `leafCriteria` (`requiredOccurrences` defaulting to `1`).
    - Decrement `newOccurrences = Math.max(0, currentOccurrences - 1)`.
    - Compute `wasCompleted = Boolean.TRUE.equals(leafProgress.getIsCompleted())` and `isNowCompleted = newOccurrences >= requiredOccurrences`.
    - If `newOccurrences == 0`, delete `leafProgress`; otherwise set `completedOccurrences = newOccurrences`, `isCompleted = isNowCompleted`, clear `completedAt` if `!isNowCompleted`, and save `leafProgress`.
    - Only propagate `-1` up to parents when `wasCompleted && !isNowCompleted`.

- [ ] **Step 4: Run tests to verify all pass**
  - Run: `mvn test -pl ddd-application -am -Dtest=StudentCriteriaProgressCommandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: PASS (all existing + new tests green).

- [ ] **Step 5: Commit**
  - `git add ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImpl.java ddd-application/src/test/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImplTest.java`
  - `git commit -m "feat(criteria-progress): track completedOccurrences and state transitions for leaf criteria"`

---

### Task 4: Student Progress Query Output (`requiredOccurrences` & `completedOccurrences`)

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/criteria/CriteriaProgressItemDto.java:5-17`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressQueryServiceImpl.java:72-106`
- Test: `ddd-application/src/test/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressQueryServiceImplTest.java`

**Interfaces:**
- Consumes: `CriteriaTreeNodeDto.requiredOccurrences()` (Task 2), `StudentCriteriaProgressJpaEntity.getCompletedOccurrences()` (Task 1).
- Produces: `CriteriaProgressItemDto` with `Integer requiredOccurrences` and `int completedOccurrences`.

- [ ] **Step 1: Write failing unit test in `StudentCriteriaProgressQueryServiceImplTest.java`**
  - `getProgress_shouldIncludeRequiredAndCompletedOccurrencesForLeafNode()` asserting `requiredOccurrences == 3`, `completedOccurrences == 2`, `isCompleted == false`.

- [ ] **Step 2: Run test to verify it fails**
  - Run: `mvn test -pl ddd-application -am -Dtest=StudentCriteriaProgressQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: FAIL (missing fields on `CriteriaProgressItemDto`).

- [ ] **Step 3: Update `CriteriaProgressItemDto.java` and `StudentCriteriaProgressQueryServiceImpl.java`**
  - Add `Integer requiredOccurrences` and `int completedOccurrences` to `CriteriaProgressItemDto`.
  - In `StudentCriteriaProgressQueryServiceImpl.attachProgress`: read `completedOccurrences` from `progressEntity` (default `0`), read `requiredOccurrences` from `node.requiredOccurrences()` (default `1`), and pass both into `new CriteriaProgressItemDto(...)`.

- [ ] **Step 4: Run all Criteria tests and compile `ddd-api`**
  - Run: `mvn test-compile -pl ddd-api -am && mvn test -pl ddd-application -am -Dtest=CriteriaServiceImplTest,StudentCriteriaProgressCommandServiceImplTest,StudentCriteriaProgressQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: `BUILD SUCCESS`, all tests PASS.

- [ ] **Step 5: Commit**
  - `git add ddd-application/src/main/java/com/ddd/application/dto/criteria/CriteriaProgressItemDto.java ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressQueryServiceImpl.java ddd-application/src/test/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressQueryServiceImplTest.java`
  - `git commit -m "feat(criteria-progress): expose requiredOccurrences and completedOccurrences in progress query"`
