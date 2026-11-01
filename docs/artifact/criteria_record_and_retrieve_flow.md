# Criteria Record and Retrieve Architectural Flow

This document details the architectural design, domain models, workflows, and execution paths for **recording student criteria progress** (e.g., during event checkout) and **retrieving criteria progress hierarchies** in the DDD system.

---

## 1. Domain Model Architecture & Separation of Concerns

The criteria system separates structural criteria metadata, student evaluation states, and immutable completion audit trails.

```mermaid
classDiagram
    direction LR

    class Criteria {
        +Long id
        +String name
        +String note
        +String scope
        +Integer level
        +Integer childrenOrder
        +Integer requiredChildren
        +Long parentId
    }

    class StudentCriteriaProgress {
        +Long id
        +Long userId
        +Long criteriaId
        +Long academicYearId
        +Boolean isCompleted
        +Instant completedAt
        +Integer completedChildrenCount
    }

    class StudentCriteriaTransaction {
        +Long id
        +Long userId
        +Long criteriaId
        +Long academicYearId
        +Long eventId
        +String completionSource
        +String note
    }

    Criteria "1" <-- "0..*" Criteria : parent
    StudentCriteriaProgress "0..*" --> "1" Criteria : evaluates
    StudentCriteriaTransaction "0..*" --> "1" Criteria : records
```

### Responsibility Breakdown

| Component | Responsibility | Database Table |
| :--- | :--- | :--- |
| **`Criteria`** | Defines criteria hierarchy, ordering, required child counts, and descriptions. Managed by [`CriteriaService`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/service/criteria/CriteriaService.java). | `criterias` |
| **`StudentCriteriaProgress`** | Maintains the student's evaluated state for both leaf and ancestor criteria per academic year (`isCompleted`, `completedChildrenCount`). | `student_criteria_progress` |
| **`StudentCriteriaTransaction`** | Append-only event/manual transaction log preserving checkout evidence, event references, notes, and sources (`EVENT_CHECKOUT`, `MANUAL`). | `student_criteria_transactions` |

---

## 2. Criteria Recording Flow (Event Checkout)

When one or more students complete checkout from an event (self check-out or fanpage admin batch check-out), two write operations execute in coordinated sequence:

1. **Transaction Logging**: Records an immutable `StudentCriteriaTransaction` for each student.
2. **Progress Propagation**: Evaluates and persists `StudentCriteriaProgress` bottom-up through the criteria hierarchy.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Fanpage Admin / Student
    participant RC as RegistrationCommandService
    participant TC as StudentCriteriaTransactionCommandService
    participant PC as StudentCriteriaProgressCommandService
    participant PR as StudentCriteriaProgressRepository
    participant CR as CriteriaRepository

    Admin->>RC: fanpageMemberCheckOut(eventId, studentUserIds)
    RC->>RC: validateAndCompleteRegistration()
    RC->>RC: awardPointsForStudents()
    
    RC->>TC: createTransactionFromEvent(studentUserIds, eventId)
    TC->>TC: bulkCreateTransactions()
    TC-->>RC: Transactions Saved

    RC->>PC: bulkMarkCompleted(studentUserIds, criteriaId, academicYearId)
    PC->>PR: findByUserIdsAndCriteriaIdAndAcademicYearId()
    PC->>PR: saveAll(newLeafProgresses)
    
    PC->>CR: findAllByIsActive(true)
    PC->>PR: findAllByUserIdsAndAcademicYearId(eligibleUserIds, academicYearId)
    
    loop Upward Parent Propagation Per Student
        PC->>PC: parentProgress.completedChildrenCount++
        opt completedChildrenCount >= parent.requiredChildren
            PC->>PC: parentProgress.isCompleted = true
            PC->>PC: ascend to grandparent
        end
    end

    PC->>PR: saveAll(updatedParentProgressList)
    PC-->>RC: Completion Persisted
```

### Hierarchical Propagation Logic

The propagation algorithm implemented in [`StudentCriteriaProgressCommandServiceImpl.java`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImpl.java) operates bottom-up:

```mermaid
flowchart TD
    Start([Eligible Student Checkout]) --> SaveLeaf[Save Leaf Progress: isCompleted = true]
    SaveLeaf --> GetParent[Get leaf.parentId]
    
    CheckParent{Parent Exists?}
    GetParent --> CheckParent
    CheckParent -- No / Reached Root --> End([Finish Batch Save])
    
    CheckParent -- Yes --> IncCount[Increment parent.completedChildrenCount]
    IncCount --> CheckThreshold{completedChildrenCount >= requiredChildren?}
    
    CheckThreshold -- Yes & Not Yet Completed --> MarkComplete[Set parent.isCompleted = true & completedAt = now]
    MarkComplete --> Ascend[currentParentId = parent.parentId]
    Ascend --> CheckParent
    
    CheckThreshold -- No or Already Completed --> StopAscent[Halt Upward Ascent for this Student]
    StopAscent --> End
```

> [!NOTE]
> `bulkMarkCompleted` runs in batches:
> - Evaluates existing completions in one query: `findByUserIdsAndCriteriaIdAndAcademicYearId`.
> - Fetches all existing student progress records across the batch in one query: `findAllByUserIdsAndAcademicYearId`.
> - Saves leaf and ancestor entities using `saveAll(...)`, avoiding N+1 roundtrips.

---

## 3. Criteria Retrieval Flow

To ensure high performance and maintain clean DDD boundaries, retrieval decouples structural tree definitions from student completion evaluations.

```mermaid
sequenceDiagram
    autonumber
    actor Student as Authenticated Student
    participant Ctrl as StudentCriteriaProgressController
    participant PQ as StudentCriteriaProgressQueryService
    participant CS as CriteriaService
    participant CR as CriteriaRepository
    participant PR as StudentCriteriaProgressJpaRepository

    Student->>Ctrl: GET /criteria-progress?academicYearId=...
    Ctrl->>PQ: getProgress(email, academicYearId)
    
    PQ->>CS: getCriteriaTree()
    CS->>CR: findAllByIsActive(true)
    CS->>CS: groupChildrenByParent()
    CS->>CS: buildTreeNodeTopDown()
    CS-->>PQ: List<CriteriaTreeNodeDto> (Pure Structure)

    PQ->>PR: findAllByUserIdAndAcademicYearId(user.getId(), academicYearId)
    PR-->>PQ: List<StudentCriteriaProgressJpaEntity>
    
    PQ->>PQ: Top-Down Decoration (attachProgress)
    Note over PQ: Leaves: check progressMap.get(id).isCompleted<br/>Parents: completedChildrenCount from child DTOs
    
    PQ-->>Ctrl: StudentCriteriaProgressDto
    Ctrl-->>Student: 200 OK (BaseResponse<StudentCriteriaProgressResponseDto>)
```

### Two-Phase Retrieval Mechanics

1. **Phase 1: Structural Tree Generation ([`CriteriaService.getCriteriaTree()`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/service/criteria/impl/CriteriaServiceImpl.java#L99-L127))**:
   - Queries all active criteria using the domain repository [`CriteriaRepository`](file:///home/shrimp/code/ddd/ddd-domain/src/main/java/com/ddd/domain/repository/CriteriaRepository.java).
   - Groups criteria by `parentId`.
   - Identifies top roots (`parentId == null`). If a single container root exists, unrolls its children as the top-level main criteria.
   - Recursively builds `CriteriaTreeNodeDto` top-down with siblings sorted by `childrenOrder` and `id`.

2. **Phase 2: Completion Decoration ([`StudentCriteriaProgressQueryServiceImpl.getProgress()`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressQueryServiceImpl.java#L37-L69))**:
   - Loads the student's progress records for the academic year in a single query via `@EntityGraph(attributePaths = {"criteria"})`.
   - Traverses the `CriteriaTreeNodeDto` hierarchy top-down:
     - **Leaf nodes**: Status resolved directly from `progressMap.get(node.id()).getIsCompleted()`.
     - **Parent nodes**: First aggregates child completion states; then evaluates:
       $$\text{completedChildrenCount} = \sum (\text{child.isCompleted} == \text{true})$$
       $$\text{isCompleted} = (\text{completedChildrenCount} \ge \text{requiredChildren})$$
   - Computes overall statistics (`completedMainCriteria`, `totalMainCriteria`) and returns [`StudentCriteriaProgressDto`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/dto/criteria/StudentCriteriaProgressDto.java).

---

## 4. Key DTO Interfaces & Contracts

### Pure Structural Tree Node
[`CriteriaTreeNodeDto.java`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/dto/criteria/CriteriaTreeNodeDto.java):
```java
public record CriteriaTreeNodeDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Integer requiredChildren,
        List<CriteriaTreeNodeDto> children
) {}
```

### Progress-Decorated Tree Node
[`CriteriaProgressItemDto.java`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/dto/criteria/CriteriaProgressItemDto.java):
```java
public record CriteriaProgressItemDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Integer requiredChildren,
        int completedChildrenCount,
        boolean isCompleted,
        List<CriteriaProgressItemDto> children
) {}
```

### Response Payload
[`StudentCriteriaProgressResponseDto.java`](file:///home/shrimp/code/ddd/ddd-api/src/main/java/com/ddd/api/dto/criteria/res/StudentCriteriaProgressResponseDto.java):
```java
public record StudentCriteriaProgressResponseDto(
        Long academicYearId,
        String academicYear,
        int completedMainCriteria,
        int totalMainCriteria,
        List<CriteriaProgressItemResponseDto> criterias
) {}
```

> [!IMPORTANT]
> Detailed transaction histories (e.g., event evidence, completion timestamps, manual notes) are retrieved via the dedicated `StudentCriteriaTransactionQueryService` endpoints to maintain strict separation of concerns and keep progress tree retrieval lightweight.
