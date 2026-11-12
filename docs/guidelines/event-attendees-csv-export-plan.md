# Implementation Plan: Export Event Attendees to CSV for Fanpage Members

## 1. Overview & Objectives
This feature provides a dedicated API endpoint allowing active **fanpage members** to export a complete list of attendees for any event belonging to their fanpage into a standard **CSV file**.

The exported file contains:
1. **Event Information Header**: High-level metadata of the event (Event ID, Name, Fanpage, Location, Dates, Capacity, Status).
2. **Attendees & Attendance Table**: Detailed record of each registered student, including profile information and real-time attendance verification (check-in/check-out timestamps, evidence URLs, reasons).

---

## 2. Architecture & DDD Layer Alignment

```
┌─────────────────────────────────────────────────────────────┐
│                       ddd-api Layer                         │
│  RegistrationController                                     │
│  GET /registrations/fanpage-member/fanpages/{fanpageId}/... │
│  Returns: ResponseEntity<byte[]> (text/csv; charset=UTF-8)  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   ddd-application Layer                     │
│  - RegistrationQueryService / RegistrationQueryServiceImpl  │
│  - EventAttendeeExportDto / AttendeeExportRowDto            │
│  - CsvExportUtil (RFC 4180 Escaping + UTF-8 BOM)            │
│  - verifyFanpageMember(fanpageId, eventId)                  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                  ddd-infrastructure Layer                   │
│  - RegistrationJpaRepository (@EntityGraph eager fetching)  │
│  - FanpageMemberJpaRepository (Membership authorization)    │
│  - EventJpaRepository (Fanpage ownership verification)      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Security & Access Control Contract

To protect student privacy and ensure proper fanpage isolation, the endpoint enforces the following security checks:
1. **Authentication**: Requester must have a valid JWT token (`SecurityContextPort.getCurrentUserId() != null`).
2. **Fanpage Membership**: The authenticated user must be an active member of the specified `fanpageId` (`fanpageMemberJpaRepository.existsByFanpageIdAndUserIdAndIsActive(fanpageId, currentUserId, true)`).
3. **Event Ownership**: The event must belong directly to `fanpageId` (`eventJpaRepository.existsByIdAndFanpageId(eventId, fanpageId)`).

If validation fails, a `BusinessException` (or `AuthException`) is thrown.

---

## 4. CSV File Structure & Formatting

To guarantee 100% compatibility with **Microsoft Excel** (Windows & Mac), **Google Sheets**, and **Apple Numbers**:
- **Encoding**: UTF-8 prefixed with the **Byte Order Mark (BOM)**: `\uFEFF`. Without this BOM, Excel defaults to ANSI and garbles Vietnamese characters (e.g., "Nguyễn", "Trần").
- **RFC 4180 Standard**:
  - Fields containing commas (`,`), quotes (`"`), or newlines (`\n`) are enclosed in double quotes.
  - Existing quotes inside fields are escaped as `""`.
  - Lines are terminated with standard CRLF (`\r\n`).

### File Layout

```csv
# EVENT INFORMATION
Event ID,105
Event Name,Tech Talk 2026: AI & Cloud Computing
Fanpage,IT Club FPT
Location,Hall A - Innovation Building
Event Date,2026-11-20 09:00:00 UTC
Registration Window,2026-11-01 00:00:00 UTC - 2026-11-18 23:59:59 UTC
Capacity,200
Status,PUBLISHED

# ATTENDEES & CHECK-IN DETAILS
No.,Student ID,Full Name,Email,Gender,Major,Registration Date,Status,Check-in Time,Check-in Evidence,Check-out Time,Check-out Evidence,Note
1,SE160001,Nguyen Van A,anguyen@fpt.edu.vn,Male,Software Engineering,2026-11-02 08:30:15 UTC,CHECKED_OUT,2026-11-20 08:45:00 UTC,https://res.cloudinary.com/.../in.jpg,2026-11-20 11:30:00 UTC,https://res.cloudinary.com/.../out.jpg,
2,SE160002,Tran Thi B,btran@fpt.edu.vn,Female,Information Assurance,2026-11-03 14:10:00 UTC,CHECKED_IN,2026-11-20 08:50:00 UTC,https://res.cloudinary.com/.../in2.jpg,,,,
```

---

## 5. Detailed Implementation Steps

### Step 1: Infrastructure Layer (`ddd-infrastructure`)
In `RegistrationJpaRepository.java`, add an optimized query with `@EntityGraph` to fetch attendees, student profiles, and majors in a single query:
```java
@EntityGraph(attributePaths = {"user", "user.studentProfile", "user.studentProfile.major"})
@Query("SELECT r FROM RegistrationJpaEntity r WHERE r.event.id = :eventId ORDER BY r.createdAt ASC")
List<RegistrationJpaEntity> findAllByEventIdWithDetails(@Param("eventId") Long eventId);
```

### Step 2: Application Layer DTOs (`ddd-application`)
Create records under `com.ddd.application.dto.export`:
1. `AttendeeExportRowDto`:
   - `int index`
   - `String studentId`
   - `String fullName`
   - `String email`
   - `String gender`
   - `String majorName`
   - `Instant regDate`
   - `String status`
   - `Instant checkInAt`
   - `String checkInEvidenceUrl`
   - `Instant checkOutAt`
   - `String checkOutEvidenceUrl`
   - `String reason`
2. `EventAttendeeExportDto`:
   - `Long eventId`, `String eventName`, `String fanpageName`, `String location`
   - `Instant dateHappen`, `Instant dateOpen`, `Instant dateClose`
   - `Integer capacity`, `String status`
   - `List<AttendeeExportRowDto> attendees`

### Step 3: Application Utility (`ddd-application`)
Create `com.ddd.application.utils.CsvExportUtil`:
- Generates byte array with `\uFEFF` prefix.
- Emits Event Summary Header block.
- Emits Column Headers.
- Loops through attendees and writes escaped rows.
- Helper method `escapeCsv(String field)`.

### Step 4: Application Service (`ddd-application`)
1. Extend `RegistrationQueryService`:
   ```java
   byte[] exportAttendeesToCsv(Long fanpageId, Long eventId);
   ```
2. Implement in `RegistrationQueryServiceImpl`:
   - Invoke `verifyFanpageMember(fanpageId, eventId);`
   - Retrieve event entity via `eventJpaRepository.findById(eventId)`
   - Retrieve registration entities via `registrationJpaRepository.findAllByEventIdWithDetails(eventId)`
   - Map entities into `EventAttendeeExportDto`
   - Call `CsvExportUtil.generateAttendeesCsv(exportDto)` and return `byte[]`

### Step 5: REST API Controller (`ddd-api`)
In `RegistrationController.java`:
```java
@GetMapping(value = "/fanpage-member/fanpages/{fanpageId}/events/{eventId}/attendees/export", produces = "text/csv")
public ResponseEntity<byte[]> exportAttendeesCsv(
        @PathVariable Long fanpageId,
        @PathVariable Long eventId) {
    byte[] csvBytes = registrationQueryService.exportAttendeesToCsv(fanpageId, eventId);
    String filename = "attendees-event-" + eventId + ".csv";
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csvBytes);
}
```

---

## 6. Testing & Quality Assurance Plan
1. **Unit Tests in `RegistrationQueryServiceImplTest`**:
   - `exportAttendeesToCsv_success`: Valid member, event with attendees $\rightarrow$ returns valid CSV bytes starting with UTF-8 BOM.
   - `exportAttendeesToCsv_notMember_throwsBusinessException`: Non-member requester $\rightarrow$ throws `BusinessException`.
   - `exportAttendeesToCsv_eventNotBelongToFanpage_throwsBusinessException`: Mismatched fanpage and event $\rightarrow$ throws `BusinessException`.
2. **Unit Tests in `RegistrationControllerTest`**:
   - Verify HTTP status `200 OK`, `Content-Disposition` header with filename, and `Content-Type: text/csv; charset=UTF-8`.
3. **CSV Formatting Tests**:
   - Verify proper escaping of commas, quotes, and newlines in event names, locations, and student names.
