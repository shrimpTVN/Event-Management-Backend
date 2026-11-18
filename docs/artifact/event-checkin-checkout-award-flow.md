# Event Check-In, Check-Out, and Award Recording Flow Specification

> **Target Service**: [`RegistrationCommandServiceImpl`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImpl.java)  
> **Architecture Pattern**: Domain-Driven Design (DDD) with CQRS and Event-Driven Architecture  
> **Status**: Production Reference Document  

---

## 1. Executive Summary

In this application, the attendance and reward lifecycle models a student's participation in an event from initial registration to point and criteria attainment. The flow enforces a multi-tier verification process:
1. **Registration & Readiness**: Validates event publication, registration time windows, and user duplicate registration checks.
2. **Dual Check-In Modality**: Supports student photo self-verification (`SELF_CHECKED_IN`) or staff-assisted batch check-in (`CHECKED_IN`).
3. **Dual Check-Out & Confirmation**: Enforces evidence collection (`SPENDING_CONFIRM`) requiring staff verification (`CONFIRMED`), or direct staff batch check-out (`COMPLETED`).
4. **Automated Award Engine & Ledger**: When an event is marked completed, valid attendees are set to `RECORDED`, immutable point transactions are generated, category points are propagated through hierarchical category trees, evaluation criteria progress is advanced, and domain events are emitted.

---

## 2. Registration State Machine

```mermaid
stateDiagram-v2
    [*] --> REGISTERED : Student registers (registerEvent)

    REGISTERED --> CANCELLED : Student cancels (cancelRegistration)
    REGISTERED --> SELF_CHECKED_IN : Student self check-in with photo (checkIn)
    REGISTERED --> CHECKED_IN : Organizer batch check-in (fanpageMemberCheckIn)
    REGISTERED --> ABSENT : Organizer marks absent (fanpageMemberBatchAbsent)

    SELF_CHECKED_IN --> SPENDING_CONFIRM : Student self check-out with photo (checkOut)
    CHECKED_IN --> SPENDING_CONFIRM : Student self check-out with photo (checkOut)

    SELF_CHECKED_IN --> COMPLETED : Organizer batch check-out (fanpageMemberCheckOut)
    CHECKED_IN --> COMPLETED : Organizer batch check-out (fanpageMemberCheckOut)

    SPENDING_CONFIRM --> CONFIRMED : Organizer confirms attendance (fanpageMemberCheckOut with CONFIRMED)
    SPENDING_CONFIRM --> ABSENT : Organizer marks absent (rejected evidence)

    COMPLETED --> RECORDED : Event finalized (fanpageMemberMarkEventAsCompleted)
    CONFIRMED --> RECORDED : Event finalized (fanpageMemberMarkEventAsCompleted)

    REGISTERED --> ABSENT : Event finalized (Unattended)
    SELF_CHECKED_IN --> ABSENT : Event finalized (Incomplete attendance)
    CHECKED_IN --> ABSENT : Event finalized (Incomplete attendance)
    SPENDING_CONFIRM --> ABSENT : Event finalized (Unconfirmed)

    RECORDED --> [*]
    CANCELLED --> [*]
    ABSENT --> [*]
```

---

## 3. Detailed Phase Breakdown

### Phase 1: Event Registration & Cancellation

* **Methods**: `registerEvent(Long eventId)`, `cancelRegistration(Long eventId, String reason)`
* **Business Invariants**:
  * An event must have status `PUBLISHED`.
  * The current timestamp must fall strictly within `[dateOpen, dateClose]`.
  * If a student was previously `CANCELLED`, their record is revived to `REGISTERED`; active registrations throw `DuplicateResourceException`.
  * Cancellations are forbidden once `dateClose` has elapsed.

---

### Phase 2: Check-In Execution

* **Guard Method**: `validateEventAttendanceReadiness(Event event)`
  * Ensures `event.isActive() == true` and `event.getStatus().equals("PUBLISHED")`.
  * Enforces `now >= event.getDateHappen()` (cannot check in before start time).

```mermaid
flowchart TD
    StartCheckIn([Student / Staff initiates Check-In]) --> CheckReadiness{Event started & Published?}
    CheckReadiness -- No --> ErrNotReady[Throw EventAttendanceException]
    CheckReadiness -- Yes --> CheckRole{Caller Type?}

    CheckRole -- Student --> StudentSelfCheckIn[Self Check-In]
    StudentSelfCheckIn --> VerifyStatus{Status == REGISTERED?}
    VerifyStatus -- No --> ErrInvalidStatus[Throw EventAttendanceException]
    VerifyStatus -- Yes --> SaveSelf[Record checkInAt, proofUrl<br/>status = SELF_CHECKED_IN]

    CheckRole -- Organizer --> VerifyMember{Is active Fanpage Member?}
    VerifyMember -- No --> ErrForbidden[Throw EventAttendanceException]
    VerifyMember -- Yes --> BatchLoop[For each distinct student]
    BatchLoop --> SaveStaff[Record checkInAt, checkInBy<br/>status = CHECKED_IN]
```

---

### Phase 3: Check-Out & Attendance Confirmation

Handled by the internal validator `validateAndCompleteRegistration(...)`:

| Operation | Trigger Method | Required Source Status | Resulting Status | Description |
|---|---|---|---|---|
| **Student Self Check-Out** | `checkOut(eventId, proofUrl)` | `CHECKED_IN` or `SELF_CHECKED_IN` | `SPENDING_CONFIRM` | Attaches check-out proof photo. Enters review queue. |
| **Organizer Batch Check-Out** | `fanpageMemberCheckOut(eventId, ids, COMPLETED)` | `CHECKED_IN` or `SELF_CHECKED_IN` | `COMPLETED` | Direct attendance completion by staff on site. |
| **Organizer Confirmation** | `fanpageMemberCheckOut(eventId, ids, CONFIRMED)` | `SPENDING_CONFIRM` | `CONFIRMED` | Staff reviews evidence and approves attendance. |
| **Organizer Absence Marking** | `fanpageMemberBatchAbsent(eventId, ids)` | `REGISTERED` or `CHECKED_IN` | `ABSENT` | Marks student absent. Blocked if `SPENDING_CONFIRM` or `COMPLETED`. |

---

### Phase 4: Event Finalization & Award Engine

When organizers invoke `fanpageMemberMarkEventAsCompleted(Long eventId)`:

```mermaid
sequenceDiagram
    autonumber
    actor Organizer as Fanpage Organizer
    participant RCS as RegistrationCommandServiceImpl
    participant RegRepo as RegistrationRepository
    participant EPR as EventPointRepository
    participant PTR as PointTransactionRepository
    participant SSP as StudentSemesterPointCommandService
    participant SCT as StudentCriteriaTransactionCommandService
    participant EventBus as ApplicationEventPublisher

    Organizer->>RCS: fanpageMemberMarkEventAsCompleted(eventId)
    RCS->>RCS: validateFanpageMemberPermission(fanpageMemberId, event)
    RCS->>RegRepo: findAllByEvent(eventId)
    RegRepo-->>RCS: List<Registration>

    Note over RCS: Status Partitioning<br/>COMPLETED/CONFIRMED -> RECORDED<br/>All others -> ABSENT
    RCS->>RegRepo: saveAll(registrations)

    alt Award Eligible Students Exist
        RCS->>EPR: findByEventId(eventId)
        EPR-->>RCS: List<EventPoint>

        Note over RCS: Create PointTransaction ledger records (CHECKOUT_REWARD, CONFIRMED)
        RCS->>PTR: saveAll(pointTransactions)

        Note over RCS: Aggregate points by category
        RCS->>SSP: bulkRecordPointsAndPropagate(userIds, semesterId, categoryPointsMap, CHECKOUT_REWARD)

        opt event.criteriaId != null
            RCS->>SCT: createTransactionFromEvent(userIds, eventId)
        end

        RCS->>EventBus: publishEvent(EventCheckoutAwardedEvent)
    end
```

#### Award Engine Subcomponents

1. **Status Finalization**:
   * Eligible: `COMPLETED` and `CONFIRMED` &rarr; `RECORDED` (terminal success).
   * Ineligible: Unchecked-out or unconfirmed registrations &rarr; `ABSENT` (terminal failure).
   * Event status is updated to `EventStatusEnum.COMPLETED`.

2. **Immutable Point Ledger**:
   * For every `(Student, EventPoint)` pair, a `PointTransaction` record is persisted with:
     * `type = PointTransactionTypeEnum.CHECKOUT_REWARD`
     * `status = PointTransactionStatusEnum.CONFIRMED`
     * `points`, `pointCategoryId`, `semesterId`, `eventId`, and description.

3. **Hierarchical Point Propagation**:
   * Invokes `StudentSemesterPointCommandService.bulkRecordPointsAndPropagate(...)`.
   * Automatically rolls point increments upward from child categories to parent category nodes in the student's semester ledger.

4. **Training Criteria Advancement**:
   * If `event.getCriteriaId() != null`, invokes `StudentCriteriaTransactionCommandService.createTransactionFromEvent(...)` to update university graduation criteria indicators.

5. **Domain Event Publication**:
   * Emits `EventCheckoutAwardedEvent` containing event metadata and the list of awarded student user IDs for asynchronous listeners (notifications, certificate generator, etc.).

---

## 4. Key Security & Boundary Safeguards

> [!IMPORTANT]
> - **Zero-Trust Identity**: User identity is resolved server-side via `SecurityContextPort.getCurrentUserId()`, never accepted from request payloads.
> - **Fanpage Permission Enforcement**: Every administrative attendance operation requires `fanpageMemberRepository.isActiveMember(fanpageId, memberId) == true`.
> - **Temporal Protection**: `validateEventAttendanceReadiness` prohibits attendance recording before `event.dateHappen`.
> - **Idempotency & Replay Defense**: Status checks guarantee students cannot check in multiple times, check out before checking in, or re-award points after event completion.
