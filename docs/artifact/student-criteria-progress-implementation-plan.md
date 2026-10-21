# Student Criteria Progress Implementation Plan

## 1. Goal

Implement student-side tracking for the Student 5 Good program.

Students should be able to:

- View the criteria tree for an academic year.
- See which criteria are completed and which are incomplete.
- Manually tick or untick leaf criteria.
- Automatically complete a criterion after successfully checking out from a linked event.
- See parent and overall progress calculated from child criteria.

This phase does **not** include student submissions, evidence upload, reviewer approval, or moderation.

## 2. Existing functionality to reuse

- `criterias` stores the hierarchical criteria tree.
- `events.criteria_id` links an event to a criterion.
- `registrations` stores check-in/check-out state.
- `semesters` stores the academic-year association for events.
- `CriteriaController` already exposes public criteria browsing and admin CRUD.

Relevant existing code:

- `ddd-domain/src/main/java/com/ddd/domain/model/Criteria.java`
- `ddd-api/src/main/java/com/ddd/api/controller/CriteriaController.java`
- `ddd-infrastructure/src/main/resources/database/schema.sql`
- `ddd-infrastructure/src/main/resources/database/seed_criterias_sv5t.sql`

## 3. Design decisions

### 3.1 Store progress only for leaf criteria

Student progress records are created only for criteria without children.

Parent criteria are calculated from their children when the progress API is called. This avoids inconsistent data between parent and child records.

### 3.2 Use academic years, not criteria periods

The current use case has one Student 5 Good tracking cycle per academic year. Introduce a reusable `academic_years` table instead of a separate criteria-period table.

### 3.3 Use `required_children` on `criterias`

For parent criteria, `required_children` defines how many completed children are needed:

- `3`: all three children must be completed.
- `1`: at least one child must be completed.
- `2`: at least two children must be completed.
- `NULL`: leaf criterion; completion comes from the student progress record.

## 4. Database changes

### 4.1 Add `academic_years`

```sql
CREATE TABLE academic_years
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date   DATE NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Example values:

```text
2025-2026
2026-2027
```

### 4.2 Update `semesters`

Add `academic_year_id` and migrate existing values from `semesters.academic_year`.

```sql
ALTER TABLE semesters
    ADD COLUMN academic_year_id BIGINT;

ALTER TABLE semesters
    ADD CONSTRAINT fk_semesters_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years(id);

ALTER TABLE semesters
    ADD CONSTRAINT uq_semester_number_per_year
        UNIQUE (academic_year_id, number);
```

After the migration is verified, remove the duplicated `academic_year` string column.

### 4.3 Add `required_children` to `criterias`

```sql
ALTER TABLE criterias
    ADD COLUMN required_children INTEGER;

ALTER TABLE criterias
    ADD CONSTRAINT ck_criteria_required_children
        CHECK (required_children IS NULL OR required_children > 0);
```

Seed values should follow the existing Student 5 Good rules:

```text
Đạo đức tốt       -> 3
Thể lực tốt       -> 1
Tình nguyện tốt   -> 1
Hội nhập tốt      -> 2
Leaf criteria     -> NULL
```

### 4.4 Add `student_criteria_progress`

```sql
CREATE TABLE student_criteria_progress
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    criteria_id       BIGINT NOT NULL,
    academic_year_id  BIGINT NOT NULL,
    is_completed      BOOLEAN NOT NULL DEFAULT FALSE,
    completion_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    source_event_id   BIGINT,
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_student_criteria_progress
        UNIQUE (user_id, criteria_id, academic_year_id),

    CONSTRAINT fk_progress_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_progress_criteria
        FOREIGN KEY (criteria_id) REFERENCES criterias(id) ON DELETE CASCADE,

    CONSTRAINT fk_progress_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT,

    CONSTRAINT fk_progress_event
        FOREIGN KEY (source_event_id) REFERENCES events(id) ON DELETE SET NULL,

    CONSTRAINT ck_progress_source
        CHECK (completion_source IN ('MANUAL', 'EVENT_CHECKOUT', 'SYSTEM'))
);

CREATE INDEX idx_progress_user_year
    ON student_criteria_progress (user_id, academic_year_id);

CREATE INDEX idx_progress_criteria
    ON student_criteria_progress (criteria_id);
```

Column meaning:

- `is_completed`: the student's current completion state.
- `completed_at`: when the criterion became completed; clear it when a manual completion is undone.
- `completion_source`: whether completion came from a manual tick, event checkout, or system calculation.
- `source_event_id`: the event that caused automatic completion, when applicable.

## 5. Domain layer

Add:

```text
AcademicYear
StudentCriteriaProgress
```

Update `Criteria` with:

```java
private Integer requiredChildren;
```

Add validation:

- `requiredChildren` must be positive when present.
- Leaf criteria should have `requiredChildren = null`.
- Parent criteria should have `requiredChildren > 0`.
- Only leaf criteria can be manually changed.

Add domain repositories:

```text
AcademicYearRepository
StudentCriteriaProgressRepository
```

Required repository operations:

```text
findActiveAcademicYear()
findByUserIdAndAcademicYearId(...)
findByUserIdAndCriteriaIdAndAcademicYearId(...)
save(...)
deleteManualProgress(...)
```

## 6. Application layer

Create:

```text
StudentCriteriaProgressQueryService
StudentCriteriaProgressCommandService
StudentCriteriaProgressServiceImpl
```

Core operations:

```text
getProgressTree(userId, academicYearId)
setManualProgress(userId, criteriaId, academicYearId, completed)
markCompletedFromEvent(userId, eventId)
calculateCriteriaStatus(criteriaId, userId, academicYearId)
```

### Completion rules

For a leaf criterion:

```text
completed = student's is_completed value
```

For a parent criterion:

```text
completed_children >= required_children
```

For the root criterion, calculate its result from its direct children using the same rule.

Parent progress should not be inserted into `student_criteria_progress`.

## 7. API layer

### 7.1 Get current academic year

```http
GET /academic-years/current
```

### 7.2 Get student criteria progress

```http
GET /student/criteria-progress?academicYearId={id}
```

Response should include:

- Criteria tree.
- `isCompleted` for every node.
- `completionSource` for leaf criteria.
- `completedAt` for leaf criteria.
- `completedChildren` for parent criteria.
- `requiredChildren` for parent criteria.
- Overall completed and total counts.

Example:

```json
{
  "data": {
    "academicYearId": 1,
    "completedLeafCriteria": 12,
    "totalLeafCriteria": 25,
    "completedMainCriteria": 3,
    "totalMainCriteria": 5,
    "criteria": [
      {
        "criteriaId": 21,
        "name": "Tiêu chuẩn Hội nhập tốt",
        "isCompleted": true,
        "completedChildren": 2,
        "requiredChildren": 2,
        "children": []
      }
    ]
  }
}
```

### 7.3 Manually complete a leaf criterion

```http
PATCH /student/criteria/{criteriaId}/progress
```

Request:

```json
{
  "academicYearId": 1,
  "isCompleted": true
}
```

Rules:

- The authenticated student is taken from the security context.
- The requested criterion must be a leaf.
- `completion_source` is set to `MANUAL`.
- `completed_at` is set when changing from false to true.
- `completed_at` is cleared when manually changing back to false.
- An `EVENT_CHECKOUT` record should not be silently downgraded by a manual request.

### 7.4 Get one criterion's progress

```http
GET /student/criteria/{criteriaId}/progress?academicYearId={id}
```

## 8. Checkout integration

Update the successful checkout transaction in `RegistrationCommandService`.

Flow:

```text
1. Validate checkout.
2. Update registration.check_out_at.
3. Mark registration as completed.
4. Load the event's criterion.
5. Load the event's semester and academic year.
6. Upsert student_criteria_progress.
7. Set completion_source = EVENT_CHECKOUT.
8. Set source_event_id and completed_at.
```

This operation must be idempotent. Repeating checkout must not create duplicate progress rows.

If an event can satisfy multiple criteria, replace the single `events.criteria_id` relationship with an `event_criterias` join table. Keep the existing column for the first version if one event maps to only one criterion.

## 9. Database migration and seed work

1. Create `academic_years` migration.
2. Insert distinct years from existing `semesters.academic_year` values.
3. Backfill `semesters.academic_year_id`.
4. Add the foreign key and unique constraint.
5. Add `criterias.required_children`.
6. Update Student 5 Good seed data with required-child values.
7. Create `student_criteria_progress`.
8. Add indexes and update triggers.
9. Verify all migration scripts on a clean PostgreSQL database.
10. Standardize the criteria seed files. `seed_criterias_sv5t.sql` should be the canonical source; remove or fix the older conflicting `criteria-data.sql`.

## 10. Tests

### Unit tests

- Leaf criterion completion.
- Parent criterion with `required_children = 1`.
- Parent criterion with `required_children = 2`.
- Parent criterion requiring all children.
- Manual tick and untick behavior.
- Completion source handling.

### Integration tests

- Student can read their own progress.
- Student cannot modify another student's progress.
- Duplicate progress records are prevented.
- Checkout automatically completes the linked criterion.
- Repeated checkout is idempotent.
- An event without a criterion does not create progress.
- Parent status is returned correctly.
- Academic-year filtering works correctly.

## 11. Implementation order

1. Clean and standardize criteria seed data.
2. Add Flyway/database migration for academic years.
3. Backfill semester academic-year references.
4. Add `required_children` to criteria.
5. Add `student_criteria_progress`.
6. Implement domain models and repositories.
7. Implement progress calculation in the application layer.
8. Implement the student progress-tree API.
9. Implement manual tick/untick.
10. Integrate successful event checkout.
11. Add unit and PostgreSQL integration tests.
12. Update the API documentation.

## 12. Out of scope for this phase

- Evidence upload.
- Student submission workflow.
- Administrator approval.
- Reviewer comments.
- Notification workflow.
- Criteria version snapshots.
- Multiple evaluation campaigns within one academic year.

These can be added later if the project evolves from a personal tracker into an official evaluation and approval system.
