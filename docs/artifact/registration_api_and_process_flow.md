# Registration API and Process Flow Architectural Document

This document details the architectural design, domain models, lifecycle states, business validation rules, and end-to-end execution flows for the **Event Registration & Attendance Management** sub-system within the DDD platform.

---

## 1. Domain Model Architecture & Entity Relationships

The registration aggregate links a `User` (student attendee) to an `Event`, tracking the attendance state machine across registration, check-in, check-out, and reward distribution.

```mermaid
classDiagram
    direction LR

    class Registration {
        +Long userId
        +Long eventId
        +Instant regDate
        +Instant checkInAt
        +Instant checkOutAt
        +String checkInEvidenceUrl
        +String checkOutEvidenceUrl
        +Long checkInBy
        +Long checkOutBy
        +RegistrationStatusEnum status
        +String reason
        +Instant createdAt
        +Instant updatedAt
    }

    class Event {
        +Long id
        +String name
        +Instant dateOpen
        +Instant dateClose
        +Instant dateHappen
        +Integer capacity
        +String status
        +Boolean isActive
        +Long criteriaId
        +Long semesterId
        +Long fanpageId
        +List~EventPoint~ eventPoints
    }

    class PointTransaction {
        +Long id
        +Long userId
        +Long eventId
        +Double points
        +Long pointCategoryId
        +Long semesterId
        +String type
        +String status
        +String description
    }

    class StudentSemesterPoint {
        +Long id
        +Long userId
        +Long semesterId
        +Long pointCategoryId
        +Double totalPoint
        +Instant updatedAt
    }

    class StudentCriteriaTransaction {
        +Long id
        +Long userId
        +Long criteriaId
        +Long academicYearId
        +Long eventId
        +String completionSource
    }

    Event "1" <-- "0..*" Registration : joins
    Registration "1" ..> "0..*" PointTransaction : generates on checkout
    PointTransaction "0..*" --> "1" StudentSemesterPoint : updates aggregate
    Registration "1" ..> "0..1" StudentCriteriaTransaction : triggers if criteriaId present
```

### Component Responsibility Matrix

| Component | Layer | Primary Responsibility |
| :--- | :--- | :--- |
| [`RegistrationController`](file:///home/shrimp/code/ddd/ddd-api/src/main/java/com/ddd/api/controller/RegistrationController.java) | API | Exposes REST endpoints for student self-service and fanpage organizer batch attendance. |
| [`RegistrationCommandService`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/service/registration/RegistrationCommandService.java) | Application (Write) | Orchestrates state transitions, business validation, point reward calculation, and criteria event triggers. |
| [`RegistrationQueryService`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/service/registration/RegistrationQueryService.java) | Application (Read) | Provides read models for registration history, upcoming events, and organizer attendee rosters. |
| [`RegistrationRepository`](file:///home/shrimp/code/ddd/ddd-domain/src/main/java/com/ddd/domain/repository/RegistrationRepository.java) | Domain Port | Abstraction for reading and persisting `Registration` domain models. |
| [`RegistrationRepositoryImpl`](file:///home/shrimp/code/ddd/ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/RegistrationRepositoryImpl.java) | Infrastructure Adapter | JPA persistence implementation using composite primary key [`RegistrationId`](file:///home/shrimp/code/ddd/ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/RegistrationId.java). |
| [`SecurityContextPort`](file:///home/shrimp/code/ddd/ddd-domain/src/main/java/com/ddd/domain/model/port/security/SecurityContextPort.java) | Domain Port | Provides current authenticated actor identity (`getCurrentUserId()`). |

---

## 2. Registration Lifecycle State Machine

The registration status moves through well-defined transitions governed by [`RegistrationStatusEnum`](file:///home/shrimp/code/ddd/ddd-domain/src/main/java/com/ddd/domain/enums/RegistrationStatusEnum.java):

```mermaid
stateDiagram-v2
    [*] --> REGISTERED : Student registers (registerEvent)
    CANCELLED --> REGISTERED : Student re-registers before dateClose

    REGISTERED --> CANCELLED : Student cancels (cancelRegistration, now <= dateClose)
    REGISTERED --> CHECKED_IN : Self or Fanpage check-in (now >= dateHappen)

    CHECKED_IN --> COMPLETED : Self or Fanpage check-out (awards Points & Criteria)

    COMPLETED --> [*]
    CANCELLED --> [*]
```

### State Invariant Rules

1. **`REGISTERED`**:
   - Initial state when registering.
   - May be transitioned to `CANCELLED` prior to `event.dateClose`.
   - Cannot check-in unless in `REGISTERED` state.
2. **`CANCELLED`**:
   - Can transition back to `REGISTERED` if the student re-registers before `event.dateClose`.
   - Re-registration resets `regDate = Instant.now()` and clears previous cancellation state.
3. **`CHECKED_IN`**:
   - Requires `event.status == PUBLISHED`, `event.isActive == true`, and `now >= event.dateHappen`.
   - Records `checkInAt`, `checkInBy` (student ID or fanpage organizer ID), and optional `checkInEvidenceUrl`.
   - Cannot check-in again (idempotency guard).
4. **`COMPLETED`**:
   - Requires previous status to be `CHECKED_IN`.
   - Records `checkOutAt`, `checkOutBy`, and optional `checkOutEvidenceUrl`.
   - Triggers atomic point rewarding and criteria transaction creation.
   - Terminal state: cannot be checked-in, checked-out, or cancelled again.

---

## 3. Detailed Process Flows

### 3.1. Student Self-Registration Flow

Endpoint: `POST /registrations/me/events/{eventId}`

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant API as RegistrationController
    participant Sec as SecurityContextPort
    participant Cmd as RegistrationCommandServiceImpl
    participant EvRepo as EventRepository
    participant RegRepo as RegistrationRepository

    Student ->> API: POST /registrations/me/events/{eventId}
    API ->> Cmd: registerEvent(email, eventId)
    Cmd ->> EvRepo: findById(eventId)
    EvRepo -->> Cmd: event

    Note over Cmd: Validate Event:<br/>1. status == PUBLISHED<br/>2. now >= dateOpen<br/>3. now <= dateClose
    alt Invalid Event Status or Window
        Cmd -->> API: throw EventRegistrationException
        API -->> Student: 400 Bad Request
    end

    Cmd ->> RegRepo: isRegistered(userId, eventId)
    alt Already registered and status != CANCELLED
        Cmd -->> API: throw DuplicateResourceException("Already registered")
        API -->> Student: 409 Conflict
    else Previously CANCELLED
        Cmd ->> Cmd: reuse existing record, set status=REGISTERED, regDate=now
    else New Registration
        Cmd ->> Cmd: create new Registration(status=REGISTERED, regDate=now)
    end

    Cmd ->> RegRepo: save(registration, userId, eventId)
    Cmd -->> API: void
    API -->> Student: 200 OK
```

---

### 3.2. Student Cancellation Flow

Endpoint: `PATCH /registrations/me/events/{eventId}/cancel`

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant API as RegistrationController
    participant Sec as SecurityContextPort
    participant Cmd as RegistrationCommandServiceImpl
    participant EvRepo as EventRepository
    participant RegRepo as RegistrationRepository

    Student ->> API: PATCH /registrations/me/events/{eventId}/cancel { reason }
    API ->> Cmd: cancelRegistration(email, eventId, reason)
    Cmd ->> Sec: getCurrentUserId()
    Sec -->> Cmd: userId
    Cmd ->> EvRepo: findById(eventId)
    EvRepo -->> Cmd: event
    Cmd ->> RegRepo: findByUserAndEvent(userId, eventId)
    RegRepo -->> Cmd: registration

    Note over Cmd: Validation:<br/>1. now <= event.dateClose<br/>2. registration.status == REGISTERED
    alt now > event.dateClose
        Cmd -->> API: throw EventRegistrationException("Cannot CANCEL after event close")
        API -->> Student: 400 Bad Request
    else registration.status != REGISTERED
        Cmd -->> API: throw EventRegistrationException("Can only cancel currently REGISTERED")
        API -->> Student: 400 Bad Request
    end

    Cmd ->> Cmd: registration.setStatus(CANCELLED), registration.setReason(reason)
    Cmd ->> RegRepo: save(registration, userId, eventId)
    Cmd -->> API: void
    API -->> Student: 200 OK
```

---

### 3.3. Student Self Check-In Flow

Endpoint: `PATCH /registrations/me/events/{eventId}/check-in`

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant API as RegistrationController
    participant Sec as SecurityContextPort
    participant Cmd as RegistrationCommandServiceImpl
    participant EvRepo as EventRepository
    participant RegRepo as RegistrationRepository

    Student ->> API: PATCH /registrations/me/events/{eventId}/check-in { proofUrl }
    API ->> Sec: getCurrentUserId()
    Sec -->> API: userId
    API ->> Cmd: checkIn(eventId, proofUrl)
    Cmd ->> Sec: getCurrentUserId()
    Sec -->> Cmd: userId

    Cmd ->> EvRepo: findById(eventId)
    EvRepo -->> Cmd: event

    Note over Cmd: validateEventAttendanceReadiness(event):<br/>1. event.isActive == true<br/>2. event.status == PUBLISHED<br/>3. event.dateHappen != null<br/>4. now >= event.dateHappen

    Cmd ->> RegRepo: findByUserAndEvent(userId, eventId)
    RegRepo -->> Cmd: registration

    Note over Cmd: Validate Registration Status:<br/>1. status != CHECKED_IN (Already checked in)<br/>2. status != COMPLETED (Already completed)<br/>3. status != CANCELLED (Cancelled)<br/>4. status == REGISTERED

    Cmd ->> Cmd: registration.setStatus(CHECKED_IN)<br/>registration.setCheckInAt(now)<br/>registration.setCheckInBy(userId)<br/>registration.setCheckInEvidenceUrl(proofUrl)
    Cmd ->> RegRepo: save(registration, userId, eventId)
    Cmd -->> API: void
    API -->> Student: 200 OK
```

---

### 3.4. Student Self Check-Out & Reward Flow

Endpoint: `PATCH /registrations/me/events/{eventId}/check-out`

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant API as RegistrationController
    participant Cmd as RegistrationCommandServiceImpl
    participant EvRepo as EventRepository
    participant RegRepo as RegistrationRepository
    participant PointRepo as EventPointRepository
    participant TxRepo as PointTransactionRepository
    participant SemPtRepo as StudentSemesterPointRepository
    participant CritService as StudentCriteriaTransactionCommandService

    Student ->> API: PATCH /registrations/me/events/{eventId}/check-out { proofUrl }
    API ->> Cmd: checkOut(eventId, proofUrl)

    Note over Cmd: 1. validateEventAttendanceReadiness(event)<br/>2. validateAndCompleteRegistration (status must be CHECKED_IN)
    Cmd ->> Cmd: registration.setStatus(COMPLETED)<br/>registration.setCheckOutAt(now)<br/>registration.setCheckOutBy(userId)<br/>registration.setCheckOutEvidenceUrl(proofUrl)
    Cmd ->> RegRepo: save(registration, userId, eventId)

    rect rgb(240, 248, 255)
        Note over Cmd, SemPtRepo: Step 3: awardPointsForStudents(event, [userId], now)
        Cmd ->> PointRepo: findByEventId(eventId)
        PointRepo -->> Cmd: List<EventPoint>
        Cmd ->> SemPtRepo: findAllIn([userId], semesterId, categoryIds)
        SemPtRepo -->> Cmd: existing StudentSemesterPoints
        Cmd ->> Cmd: Calculate delta points & build PointTransactions (CHECKOUT_REWARD)
        Cmd ->> TxRepo: saveAll(pointTransactions)
        Cmd ->> SemPtRepo: saveAll(updatedOrNewStudentSemesterPoints)
    end

    rect rgb(255, 250, 240)
        Note over Cmd, CritService: Step 4: Criteria Progress Recording
        opt event.getCriteriaId() != null
            Cmd ->> CritService: createTransactionFromEvent(userId, eventId)
            CritService ->> CritService: Record transaction & propagate progress hierarchy
        end
    end

    Cmd -->> API: void
    API -->> Student: 200 OK
```

---

### 3.5. Fanpage Organizer Batch Attendance (Check-In & Check-Out)

Organizer Endpoints:
- Check-In: `PATCH /registrations/fanpage-member/events/{eventId}/check-in`
- Check-Out: `PATCH /registrations/fanpage-member/events/{eventId}/check-out`

```mermaid
sequenceDiagram
    autonumber
    actor Organizer as Fanpage Member
    participant API as RegistrationController
    participant Sec as SecurityContextPort
    participant Cmd as RegistrationCommandServiceImpl
    participant FanRepo as FanpageMemberRepository
    participant RegRepo as RegistrationRepository
    participant Reward as Point & Criteria Services

    Organizer ->> API: PATCH /registrations/fanpage-member/events/{eventId}/check-in { studentUserIds }
    API ->> Cmd: fanpageMemberCheckIn(eventId, studentUserIds)
    Cmd ->> Sec: getCurrentUserId()
    Sec -->> Cmd: organizerUserId

    Note over Cmd: Deduplicate studentUserIds: distinct()
    Cmd ->> FanRepo: isActiveMember(event.fanpageId, organizerUserId)
    alt Not Active Member
        Cmd -->> API: throw EventAttendanceException("No permission")
        API -->> Organizer: 403 Forbidden
    end

    Cmd ->> RegRepo: findAllByUserIdsAndEvent(distinctStudentUserIds, eventId)
    RegRepo -->> Cmd: List<Registration>

    loop For each distinct studentUserId
        Note over Cmd: Verify registration exists<br/>Verify status == REGISTERED (for check-in) or CHECKED_IN (for check-out)<br/>Set checkInBy / checkOutBy = organizerUserId
    end

    Cmd ->> RegRepo: saveAll(registrations, eventId)

    opt If Check-Out
        Cmd ->> Reward: awardPointsForStudents(event, distinctStudentUserIds, now)
        opt event.criteriaId != null
            Cmd ->> Reward: createTransactionFromEvent(distinctStudentUserIds, eventId)
        end
    end

    Cmd -->> API: void
    API -->> Organizer: 200 OK
```

---

## 4. Query APIs & Read Models

The query service [`RegistrationQueryServiceImpl`](file:///home/shrimp/code/ddd/ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationQueryServiceImpl.java) serves read-only queries with pagination:

### 4.1. My Registration History
- **Endpoint**: `GET /registrations/me/history?page=0&size=10`
- **Logic**: Resolves student `userId` via email and queries all registrations sorted descending by `createdAt`.
- **Response**: Paginated `Page<RegistrationResponseDto>` containing event details, attendance status, and check-in/out timestamps.

### 4.2. My Upcoming Events
- **Endpoint**: `GET /registrations/me/upcoming`
- **Logic**: Retrieves registrations for the student where `event.dateHappen >= now` and status is active (`REGISTERED`).
- **Response**: `List<RegistrationResponseDto>`.

### 4.3. Organizer Attendee Roster
- **Endpoint**: `GET /registrations/fanpage-member/fanpages/{fanpageId}/events/{eventId}/attendees`
- **Parameters**: `page`, `size`, `sortBy` (default: `regDate`), `sortDir` (default: `asc`).
- **Security Check**: Verifies that the requester is an active member of `fanpageId` and that `eventId` belongs to `fanpageId`.
- **Response**: `List<AttendeeResponseDto>` including student details, registration timestamp, evidence URLs, and status.

---

## 5. Summary of Business Validation Rules

| Rule Category | Condition | Throws Exception | Error Message / HTTP Status |
| :--- | :--- | :--- | :--- |
| **Registration Window** | `now < event.dateOpen` or `now > event.dateClose` | `EventRegistrationException` | *"The event registration is not open yet or has already closed!"* (400) |
| **Event Status** | `event.status != PUBLISHED` or `!event.isActive` | `EventRegistrationException` / `EventAttendanceException` | *"The event registration is not open yet or has already closed!"* / *"Cannot attend an event that is not active or published"* (400) |
| **Duplicate Registration** | Record exists and `status != CANCELLED` | `DuplicateResourceException` | *"You have already registered for this event"* (409) |
| **Cancellation Window** | `event.dateClose != null` and `now > event.dateClose` | `EventRegistrationException` | *"You can not CANCEL registration after event close"* (400) |
| **Cancellation Status** | Existing `status != REGISTERED` | `EventRegistrationException` | *"You can only cancel a registration that is currently registered."* (400) |
| **Attendance Timing** | `now < event.dateHappen` | `EventAttendanceException` | *"This event has not started yet"* (400) |
| **Check-In Status** | Existing `status == CHECKED_IN` | `EventAttendanceException` | *"You are already checked in"* (400) |
| **Check-In Status** | Existing `status == COMPLETED` | `EventAttendanceException` | *"You have already completed attendance for this event"* (400) |
| **Check-In Status** | Existing `status == CANCELLED` | `EventAttendanceException` | *"Your registration for this event was cancelled"* (400) |
| **Check-Out Status** | Existing `status != CHECKED_IN` | `EventAttendanceException` | *"Student ID {id} has not checked in!"* (400) |
| **Fanpage Permission** | Organizer not active member of event's fanpage | `EventAttendanceException` | *"You not have permission to manage this event"* (403) |
| **Batch Attendance** | Duplicate user IDs in request body | Automatically handled | Cleaned via `.distinct()` before processing |
| **Semester Assignment** | `event.semesterId == null` on checkout reward | `BusinessException` | *"Event is not assigned to any semester"* (400) |
