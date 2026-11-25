# Frontend Integration Guide: Event Creation & Registration Constraints

This document describes the request payload required from the frontend when creating or updating an event, as well as the server-side registration logic, concurrency behavior, and error responses.

---

## 1. Creating an Event

- **Endpoint:** `POST /events/fanpage-member`
- **Response:** `BaseResponse<Long>` containing the newly created `eventId` (initial status is `DRAFT`).

### Request Body (`EventCreateRequestDto`)

| Field | Type | Required | Validation / Constraints | Default | Description |
|---|---|---|---|---|---|
| `name` | `string` | Yes | `@NotNull`, `@NotBlank` | — | Event title |
| `description` | `string` | Yes | `@NotNull`, `@NotBlank` | — | Event description |
| `location` | `string` | Yes | `@NotNull`, `@NotBlank` | — | Venue / location |
| `dateOpen` | `string` (ISO-8601 `Instant`) | Yes | `@NotNull`, must not be in the past, `dateOpen <= dateClose` | — | Registration open timestamp (UTC) |
| `dateClose` | `string` (ISO-8601 `Instant`) | Yes | `@NotNull`, `dateOpen <= dateClose <= dateHappen` | — | Registration close timestamp (UTC) |
| `dateHappen` | `string` (ISO-8601 `Instant`) | Yes | `@NotNull`, `dateClose <= dateHappen` | — | Event start timestamp (UTC) |
| `capacity` | `number` (`integer`) | Yes | `@NotNull`, `@Positive` (`>= 1`) | — | Maximum total registrations |
| `maleQuantity` | `number` (`integer`) | Yes | `@NotNull`, `@Min(0)` | — | Maximum male registrations when `genderConstraint` is `true` |
| `femaleQuantity` | `number` (`integer`) | Yes | `@NotNull`, `@Min(0)` | — | Maximum female registrations when `genderConstraint` is `true` |
| `genderConstraint` | `boolean` | No | Optional boolean | `false` | When `true`, enforces `maleQuantity` / `femaleQuantity` quotas and requires `MALE`/`FEMALE` user gender |
| `bannerUrl` | `string` | Yes | `@NotNull`, `@NotBlank` | — | Event banner image URL |
| `eventTypeId` | `number` (`long`) | Yes | `@NotNull`, `@Positive` | — | Active `EventType` ID |
| `criteriaId` | `number` (`long`) | No | `@Positive` if provided | `null` | Optional `Criteria` ID |
| `fanpageId` | `number` (`long`) | Yes | `@NotNull`, `@Positive` (caller must be an active fanpage member) | — | Owning `Fanpage` ID |
| `semesterId` | `number` (`long`) | Yes | `@NotNull`, `@Positive` | — | Active `Semester` ID |
| `schoolId` | `number` (`long`) | No | `@Positive` if provided | `null` | Restricts registration to students of this school; `null` = open to all schools |
| `allowSelfCheck` | `boolean` | No | Optional boolean | `true` | Whether students can self check-in / check-out |
| `eventPoints` | `array` | No | List of `{ pointCategoryId: number (>0, required), point: number (>0, required) }` | `[]` | Point awards per category |

### Example Request Payload

```json
{
  "name": "Tech Career Talk 2026",
  "description": "Sharing session with industry engineers.",
  "location": "Hall A",
  "dateOpen": "2026-12-01T01:00:00Z",
  "dateClose": "2026-12-05T16:59:59Z",
  "dateHappen": "2026-12-06T01:30:00Z",
  "capacity": 100,
  "maleQuantity": 50,
  "femaleQuantity": 50,
  "genderConstraint": true,
  "bannerUrl": "https://cdn.example.com/events/banner.png",
  "eventTypeId": 1,
  "criteriaId": 2,
  "fanpageId": 3,
  "semesterId": 4,
  "schoolId": null,
  "allowSelfCheck": true,
  "eventPoints": [
    {
      "pointCategoryId": 1,
      "point": 5
    }
  ]
}
```

### Capacity & Quantity Rules on Create and Update (`PUT /events/fanpage-member/{id}`)

1. `maleQuantity >= 0` and `femaleQuantity >= 0`.
2. `maleQuantity + femaleQuantity <= capacity`.
   - Note: `maleQuantity + femaleQuantity` does **not** have to equal `capacity`; it must simply not exceed `capacity`.
   - When `genderConstraint` is `true`, `0` for `maleQuantity` or `femaleQuantity` means **0 allowed slots** for that gender (not unlimited).
3. **Updating an existing event (`PUT /events/fanpage-member/{id}`):**
   - Uses the same fields as create except `fanpageId` is omitted and `eventPoints` is `@NotNull` (pass `[]` if none).
   - **Important:** Omitting `genderConstraint` (or sending `null`) in `PUT` defaults it to `false` (just as omitting `allowSelfCheck` defaults it to `true`). Always send the current `genderConstraint` value on edit forms.
   - `capacity` cannot be reduced below the current number of non-cancelled registrations.
   - When `genderConstraint` is `true` (including when toggling `false -> true`), `maleQuantity` cannot be lower than current non-cancelled male registrations, and `femaleQuantity` cannot be lower than current non-cancelled female registrations.

---

## 2. Event Registration Logic & Constraints

- **Register Endpoint:** `POST /registrations/me/events/{eventId}`
- **Cancel Endpoint:** `PATCH /registrations/me/events/{eventId}/cancel` with `{ "reason": "..." }`

### Server-Side Validation & Locking Flow

All registration writes acquire a pessimistic row lock on the `events` row (`READ_COMMITTED` isolation, 3-second lock timeout) so concurrent requests for the last slot are serialized safely:

1. **Event Eligibility:**
   - Event must be active (`isActive = true`), `status = "PUBLISHED"`, and current time must satisfy `dateOpen <= now <= dateClose`.
2. **User & Student Profile Completeness:**
   - Student must have a profile with non-blank `studentId`, non-null `kNumber`, and non-null `majorId`.
3. **Designated School Restriction:**
   - If `event.schoolId` is set, the student's profile must have a `schoolId` matching `event.schoolId`.
   - If `event.schoolId` is `null`, students from any school may register.
4. **Gender Constraint (`event.genderConstraint == true`):**
   - The student's account (`user.gender`) must be `"MALE"` or `"FEMALE"` (case-insensitive).
   - Students with `gender = null` or `"OTHER"` are rejected with an error prompting them to update their gender in their profile.
   - If `event.genderConstraint == false`, `user.gender` and `maleQuantity`/`femaleQuantity` are not enforced during registration.
5. **Duplicate Check (Checked Before Capacity/Quota):**
   - If the student already has a registration for this event with any status other than `CANCELLED`, the request fails with `409 Conflict` (`002`), even if the event or gender quota is full.
   - If the student previously cancelled (`status == CANCELLED`), their existing row is reused (`status` becomes `REGISTERED`, `reason` is cleared) and consumes 1 slot.
6. **Per-Gender Quota Check (`event.genderConstraint == true`):**
   - Counts non-cancelled registrations for that event where `user.gender` matches `MALE` or `FEMALE`.
   - If `occupiedForGender >= maleQuantity` (or `femaleQuantity`), registration is rejected with **HTTP 409 (`EVENT.005`)**, even if the other gender or total `capacity` still has room.
7. **Total Capacity Check:**
   - Counts all non-cancelled registrations (`status <> 'CANCELLED'`) for the event.
   - If `occupiedTotal >= capacity`, registration is rejected with **HTTP 409 (`EVENT.004`)**.

### Cancellation & Re-Registration

- Cancelling a registration sets `status = CANCELLED` and immediately frees both the total capacity slot and (if applicable) the student's gender quota slot upon commit.
- Cancellation is only allowed before `event.dateClose` and cannot be repeated on an already `CANCELLED` registration.
- Re-registering after cancellation goes through the full validation, gender-quota, and capacity checks above.

---

## 3. Error Responses Frontend Should Handle

All errors follow `BaseResponse` (`{ "exceptionCode": "...", "message": "...", "data": null }`):

| HTTP Status | `exceptionCode` | When It Happens | Recommended UI Handling |
|---|---|---|---|
| `400 Bad Request` | `EVENT.002` | Event is not `PUBLISHED` / inactive / outside `dateOpen..dateClose`, missing/mismatched `schoolId`, or `genderConstraint=true` with `OTHER`/`null` user gender | Show backend `message` (e.g., prompt user to update profile gender or school) |
| `403 Forbidden` | `USER.001` (`STUDENT_PROFILE_REQUIRED`) | Student profile is missing or incomplete (`studentId`, `kNumber`, `majorId`) | Redirect/prompt user to complete student profile |
| `409 Conflict` | `002` (`DUPLICATE_RESOURCE`) | Student is already registered (non-cancelled) | Show "Already registered" state |
| `409 Conflict` | `EVENT.004` (`EVENT_FULL`) | Total event `capacity` is reached | Disable register button / show "Event is full" |
| `409 Conflict` | `EVENT.005` (`EVENT_GENDER_FULL`) | Event slots for the student's gender (`MALE` / `FEMALE`) are full | Show backend `message` (e.g., "Slots for male participants are full.") |
| `409 Conflict` | `005` (`CONCURRENT_MODIFICATION`) | High concurrency lock timeout (3s) on `/registrations/**` | Show retryable toast: `"Registration is busy. Please try again."` |
