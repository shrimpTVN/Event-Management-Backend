# Point History & Checkout Ledger Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Approach B (Append-Only Point Transaction Ledger) for recording point history and updating semester point summaries when a student checks out of an event.

**Architecture:** Adhere strictly to Domain-Driven Design (DDD):
- `ddd-domain`: Defines pure business models (`PointTransaction`, `StudentSemesterPoint`), enums (`PointTransactionTypeEnum`, `PointTransactionStatusEnum`), and repository interfaces with no Spring/framework leakage.
- `ddd-infrastructure`: Implements JPA entities, Flyway/PostgreSQL schema updates, entity mappers, and repository implementations for persistence.
- `ddd-application`: Orchestrates the checkout point awarding workflow (`PointLedgerService`) within `RegistrationCommandServiceImpl`, ensuring idempotent ledger insertion, category cap enforcement (`point_categories.maximum`), and semester point aggregation.
- `ddd-api`: Exposes REST endpoints for students to retrieve point transaction history and semester summaries conforming to `BaseResponse<T>`.

**Tech Stack:** Java 21, Spring Boot 4.x / Spring 6.x, PostgreSQL, Spring Data JPA, MapStruct / Lombok, JUnit 5, Mockito.

**Spec:** Defined during brainstorming session in chat (Approach B: Append-Only Transaction Ledger).

---

## Global Constraints

- **DDD Layer Independence:** `ddd-domain` must contain zero Spring/web/framework dependencies.
- **Uniform API Envelope:** All API responses must be wrapped in `BaseResponse<T>`.
- **Idempotency on Checkout:** Re-invoking check-out or duplicate event processing must never award duplicate points for the same `(user_id, event_id, point_category_id)`.
- **Ceiling Enforcement:** Aggregated `total_point` in `student_semester_points` must never exceed `point_categories.maximum`.
- **Audit & Immutability:** Ledger records in `point_transactions` are immutable once inserted; corrections require offsetting records (`REVOCATION` or `MANUAL_ADJUSTMENT`).

---

## Review Focus

1. **Concurrent Checkout & Idempotency:** If checkout is triggered concurrently or retried, verify only one set of `PointTransaction` records is inserted per event registration.
2. **Category Maximum Cap:** When adding event points that push total points beyond `point_categories.maximum`, verify the semester total caps precisely at `maximum` while the transaction ledger records the actual event point value.
3. **Multi-category Events:** For events configured with multiple `point_categories` in `event_points`, verify separate ledger entries and summary updates are executed atomically.
4. **Semester Scoping:** Verify `semester_id` is derived from `events.semester_id` and assigned accurately to both transactions and semester totals.
5. **No Negative Totals on Rollback:** If a point transaction is revoked, ensure `student_semester_points.total_point` never drops below 0.

---

### Task 1: Database Schema Migration & DDL Updates

**Files:**
- Create: `ddd-infrastructure/src/main/resources/database/migrations/V2__create_point_transactions_table.sql`
- Modify: `ddd-infrastructure/src/main/resources/database/schema.sql`

**Responsibilities:**
- Add `point_transactions` table with full FK constraints and indexes.
- Add unique constraint `uq_ssp_user_semester_category` on `student_semester_points(user_id, semester_id, point_category_id)`.

- [ ] **Step 1: Define DDL for `point_transactions`**
  ```sql
  CREATE TABLE point_transactions
  (
      id                BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
      user_id           BIGINT       NOT NULL,
      semester_id       BIGINT       NOT NULL,
      point_category_id BIGINT       NOT NULL,
      event_id          BIGINT,
      points            INTEGER      NOT NULL,
      type              VARCHAR(50)  NOT NULL,
      status            VARCHAR(50)  NOT NULL DEFAULT 'CONFIRMED',
      description       TEXT,
      created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      created_by        BIGINT,
      updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      updated_by        BIGINT,
      CONSTRAINT pk_point_transactions PRIMARY KEY (id),
      CONSTRAINT fk_pt_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
      CONSTRAINT fk_pt_semesters FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
      CONSTRAINT fk_pt_categories FOREIGN KEY (point_category_id) REFERENCES point_categories (id) ON DELETE RESTRICT,
      CONSTRAINT fk_pt_events FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE SET NULL
  );

  CREATE UNIQUE INDEX uq_pt_checkout_reward 
  ON point_transactions (user_id, event_id, point_category_id) 
  WHERE type = 'CHECKOUT_REWARD' AND status = 'CONFIRMED';

  CREATE INDEX idx_pt_user_semester ON point_transactions (user_id, semester_id);
  ```

- [ ] **Step 2: Add Unique Constraint to `student_semester_points` in `schema.sql`**
  ```sql
  ALTER TABLE student_semester_points
      ADD CONSTRAINT uq_ssp_user_semester_category UNIQUE (user_id, semester_id, point_category_id);
  ```

- [ ] **Step 3: Verification**
  - Verify syntax and foreign key integrity with existing schema.

---

### Task 2: Domain Layer Entities, Enums & Repository Interfaces

**Files:**
- Create: `ddd-domain/src/main/java/com/ddd/domain/enums/PointTransactionTypeEnum.java`
- Create: `ddd-domain/src/main/java/com/ddd/domain/enums/PointTransactionStatusEnum.java`
- Create: `ddd-domain/src/main/java/com/ddd/domain/model/PointTransaction.java`
- Create: `ddd-domain/src/main/java/com/ddd/domain/model/StudentSemesterPoint.java`
- Create: `ddd-domain/src/main/java/com/ddd/domain/repository/PointTransactionRepository.java`
- Create: `ddd-domain/src/main/java/com/ddd/domain/repository/StudentSemesterPointRepository.java`

**Responsibilities:**
- Create pure Java domain models with encapsulation and zero framework dependencies.

- [ ] **Step 1: Create Enums**
  - `PointTransactionTypeEnum`: `CHECKOUT_REWARD`, `MANUAL_BONUS`, `PENALTY_DEDUCTION`, `REVOCATION`.
  - `PointTransactionStatusEnum`: `CONFIRMED`, `REVOKED`.

- [ ] **Step 2: Create Domain Models**
  - `PointTransaction`: `id`, `userId`, `semesterId`, `pointCategoryId`, `eventId`, `points`, `type`, `status`, `description`, timestamps.
  - `StudentSemesterPoint`: `id`, `userId`, `semesterId`, `pointCategoryId`, `totalPoint`, `finalizedAt`, timestamps.

- [ ] **Step 3: Define Domain Repository Interfaces**
  - `PointTransactionRepository`:
    - `PointTransaction save(PointTransaction transaction);`
    - `boolean existsByUserIdAndEventIdAndPointCategoryIdAndType(Long userId, Long eventId, Long pointCategoryId, PointTransactionTypeEnum type);`
    - `List<PointTransaction> findByUserIdAndSemesterId(Long userId, Long semesterId);`
  - `StudentSemesterPointRepository`:
    - `Optional<StudentSemesterPoint> findByUserIdAndSemesterIdAndPointCategoryId(Long userId, Long semesterId, Long pointCategoryId);`
    - `StudentSemesterPoint save(StudentSemesterPoint studentSemesterPoint);`
    - `List<StudentSemesterPoint> findByUserIdAndSemesterId(Long userId, Long semesterId);`

---

### Task 3: Infrastructure Layer Persistence & Mapping

**Files:**
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/PointTransactionJpaEntity.java`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/StudentSemesterPointJpaEntity.java`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpa/SpringDataPointTransactionRepository.java`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpa/SpringDataStudentSemesterPointRepository.java`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/PointTransactionEntityMapper.java`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/StudentSemesterPointEntityMapper.java`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/PointTransactionRepositoryImpl.java`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/StudentSemesterPointRepositoryImpl.java`

**Responsibilities:**
- Implement JPA entities inheriting `BaseEntityJpa`.
- Provide bidirectional mappings between Domain Models and JPA Entities.
- Implement domain repositories cleanly without leaking JPA details.

- [ ] **Step 1: Create JPA Entities & Spring Data Repositories**
  - Map entity relationships with `FetchType.LAZY`.
  - Implement query methods: `findByUserIdAndSemesterId`, `findByUserIdAndSemesterIdAndPointCategoryId`.

- [ ] **Step 2: Implement Entity Mappers**
  - Map Struct / manual mapping converting domain models to/from JPA entities.

- [ ] **Step 3: Implement Repository Implementations**
  - Implement `PointTransactionRepositoryImpl` and `StudentSemesterPointRepositoryImpl`.

---

### Task 4: Application Layer Point Ledger Service & Checkout Integration

**Files:**
- Create: `ddd-application/src/main/java/com/ddd/application/service/point/PointLedgerService.java`
- Create: `ddd-application/src/main/java/com/ddd/application/service/point/impl/PointLedgerServiceImpl.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImpl.java`
- Create: `ddd-application/src/main/java/com/ddd/application/dto/point/PointTransactionDto.java`
- Create: `ddd-application/src/main/java/com/ddd/application/dto/point/StudentSemesterPointDto.java`
- Create: `ddd-application/src/main/java/com/ddd/application/mapper/PointDtoMapper.java`

**Responsibilities:**
- Implement `PointLedgerService` encapsulating the business logic for calculating, inserting ledger entries, and updating capped semester summaries.
- Integrate into `RegistrationCommandServiceImpl.checkOut(...)`.

- [ ] **Step 1: Define DTOs & Mappers**
  - `PointTransactionDto`, `StudentSemesterPointDto`.

- [ ] **Step 2: Implement `PointLedgerService`**
  - `void recordCheckoutPoints(Long userId, Long eventId);`
  - Logic:
    1. Fetch `Event` to retrieve `semesterId`. If `semesterId` is null, throw `BusinessException`.
    2. Fetch all configured points from `EventPointRepository.findByEventId(eventId)`.
    3. For each `EventPoint`:
       - Check idempotency: if `pointTransactionRepository.existsByUserIdAndEventIdAndPointCategoryIdAndType(...)` is true, skip (log warning).
       - Insert `PointTransaction` with `points = eventPoint.getPoint()`, `type = CHECKOUT_REWARD`, `status = CONFIRMED`.
       - Retrieve `PointCategory` to determine `maximum` ceiling.
       - Find or initialize `StudentSemesterPoint` for `(userId, semesterId, pointCategoryId)`.
       - Calculate `newTotal = Math.min(existingTotal + points, category.getMaximum())`.
       - Save `StudentSemesterPoint`.

- [ ] **Step 3: Connect to `RegistrationCommandServiceImpl.checkOut`**
  - After setting `registration.setStatus(RegistrationStatusEnum.COMPLETED)` and saving the registration, invoke:
    `pointLedgerService.recordCheckoutPoints(user.getId(), eventId);`
  - Ensure the whole operation runs within `@Transactional`.

---

### Task 5: API Layer Endpoints & Controller

**Files:**
- Create: `ddd-api/src/main/java/com/ddd/api/controller/StudentPointController.java`
- Create: `ddd-api/src/main/java/com/ddd/api/dto/point/res/PointTransactionResponseDto.java`
- Create: `ddd-api/src/main/java/com/ddd/api/dto/point/res/StudentSemesterPointResponseDto.java`
- Create: `ddd-api/src/main/java/com/ddd/api/mapper/PointApiMapper.java`

**Responsibilities:**
- Expose student endpoints to review earned points:
  - `GET /api/v1/students/me/points/history?semesterId={semesterId}`: Detailed transaction log.
  - `GET /api/v1/students/me/points/summary?semesterId={semesterId}`: Summary per category with progress against category maximum.

- [ ] **Step 1: Create Response DTOs and API Mapper**
- [ ] **Step 2: Implement `StudentPointController`**
- [ ] **Step 3: Secure endpoints with `@PreAuthorize("hasRole('STUDENT')")` and authenticated user resolution**

---

### Task 6: Unit & Integration Testing

**Files:**
- Create: `ddd-application/src/test/java/com/ddd/application/service/point/PointLedgerServiceTest.java`
- Create: `ddd-application/src/test/java/com/ddd/application/service/registration/RegistrationCommandServiceTest.java`

**Responsibilities:**
- Verify idempotency, cap limit enforcement, and multi-category event handling.

- [ ] **Step 1: Unit Test `PointLedgerService`**
  - Test 1: Single category event checkout awards exact points.
  - Test 2: Category ceiling limit caps `total_point` at `maximum` when event points exceed cap.
  - Test 3: Idempotent execution: Calling checkout twice does not double-record points.
  - Test 4: Multi-category event records multiple transaction lines atomically.

- [ ] **Step 2: Integration Test with `RegistrationCommandService.checkOut`**
  - Verify complete checkout flow from registration status change through to point persistence.
