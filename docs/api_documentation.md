# Backend REST API Documentation (DDD Architecture)

This document provides complete, production-ready specifications for Frontend Developers to integrate with the backend API.

---

## 1. Global Frontend Integration Guide

### 1.1 Base URL & Environment
- **Default Local Development URL**: `http://localhost:8080`
- **Reverse Proxy / Production**: Usually mounted at root `/` or prefixed with `/api` depending on deployment.
- **Port**: `8080` (configured in `application.yml` via `${SERVER_PORT:8080}`).

> [!IMPORTANT]
> Note on Route Prefixing: Most controllers mount directly on root paths (e.g. `/auth`, `/users`, `/events`, `/registrations`, `/fanpages`, `/fanpage-member-invitations`, `/notifications`, `/schools`, `/majors`, `/associations`, `/event-types`, `/semesters`, `/point-categories`, `/point-transactions`, `/student-semester-points`, `/criterias`, `/criteria-progress`, `/criteria-transactions`, `/media`), while `RoleController` is explicitly prefixed at `/api/v1/roles`.

---

### 1.2 Authentication & Cookie Session
The backend uses **JWT token stored inside an HttpOnly Cookie** (`ddd_jwt` and `ddd_refresh`):
- Upon successful login (`POST /auth/login`), the server sends:
  - `Set-Cookie: ddd_jwt=...; Path=/; HttpOnly; Max-Age=...; SameSite=Lax`
  - `Set-Cookie: ddd_refresh=...; Path=/; HttpOnly; Max-Age=...; SameSite=Lax`
- **Frontend HTTP clients (Axios, Fetch, TanStack Query) MUST enable credentials on all requests:**
  - **Axios**: `axios.defaults.withCredentials = true;` or `{ withCredentials: true }`
  - **Fetch**: `fetch(url, { credentials: 'include' })`
- On token refresh (`POST /auth/refresh-token`), new cookies are automatically set.
- On logout (`POST /auth/logout`), the server returns cleared cookies with `Max-Age=0`.

#### Recommended Axios Client Setup (TypeScript)
```typescript
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  withCredentials: true, // MANDATORY for cookie exchange
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        await apiClient.post('/auth/refresh-token');
        return apiClient(originalRequest);
      } catch (refreshError) {
        console.warn('Session expired or unauthorized');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);
```

---

### 1.3 Standard Response Envelope (`BaseResponse<T>`)
Every REST endpoint (except endpoints returning raw `void`) wraps its response payload inside `BaseResponse<T>`:

```typescript
export interface BaseResponse<T> {
  data: T | null;
  message: string;
  exceptionCode: string | null;
}
```

#### Success Example:
```json
{
  "data": {
    "userId": 1,
    "email": "student@university.edu.vn",
    "role": "STUDENT"
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

### 1.4 Global Error Envelope & Exception Codes
When an error occurs, the Spring Boot `GlobalExceptionHandler` intercepts it and returns HTTP status codes with the envelope:

```json
{
  "data": null,
  "message": "Detailed error message or validation summary",
  "exceptionCode": "AUTH_INVALID_CREDENTIALS"
}
```

#### Common HTTP Status Codes & Exception Codes
| HTTP Status | Exception Code | Description |
|:---|:---|:---|
| **400 Bad Request** | `VALIDATION_ERROR` | Bean validation failed (`@NotNull`, `@Size`, `@NotBlank`, invalid parameters) |
| **401 Unauthorized** | `AUTH_INVALID_CREDENTIALS` | Invalid email or password during login |
| **401 Unauthorized** | `UNAUTHORIZED_ERROR` | Missing or expired JWT cookie |
| **403 Forbidden** | `ACCESS_DENIED_ERROR` | Logged-in user lacks necessary role / permissions |
| **404 Not Found** | `RESOURCE_NOT_FOUND` | Queried ID does not exist in the database |
| **409 Conflict** | `DUPLICATE_RESOURCE_ERROR` | Duplicate unique record (e.g., email, fanpage name) |
| **500 Server Error** | `INTERNAL_SERVER_ERROR` | Unexpected backend runtime failure |

---

### 1.5 Spring Data Pagination Format (`Page<T>`)
Endpoints that paginate data return a Spring Data `Page<T>` inside `data`:

```typescript
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: { sorted: boolean; empty: boolean; unsorted: boolean };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number; // Current page index (0-indexed)
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}
```

---

## 2. API Endpoints by Domain

---

### 2.1 Authentication (`AuthController` - `/auth`)

#### 2.1.1 Login
- **Endpoint**: `POST /auth/login`
- **Access**: `PUBLIC`
- **Description**: Authenticate with email and password. On success, sets `ddd_jwt` and `ddd_refresh` cookies and returns user profile info.
- **Request Body**:
```json
{
  "email": "student@university.edu.vn",
  "password": "Password123!"
}
```
- **Validation Rules**:
  - `email`: Required, Not blank.
  - `password`: Required, Not blank.
- **Response `200 OK`**:
```json
{
  "data": {
    "userId": 101,
    "email": "student@university.edu.vn",
    "role": "STUDENT"
  },
  "message": "Success",
  "exceptionCode": null
}
```
- **Header**: `Set-Cookie: ddd_jwt=eyJhbGciOi...; Path=/; HttpOnly; SameSite=Lax`
- **Errors**: `401 Unauthorized` (`AUTH_INVALID_CREDENTIALS`).

---

#### 2.1.2 Refresh Token
- **Endpoint**: `POST /auth/refresh-token`
- **Access**: `PUBLIC` (Requires `ddd_refresh` cookie)
- **Description**: Exchanges valid refresh cookie for a newly issued access token and refreshed refresh cookie.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.1.3 Register Student
- **Endpoint**: `POST /auth/register`
- **Access**: `PUBLIC`
- **Description**: Register a new student user with profile attributes (school association, major, student ID).
- **Request Body**:
```json
{
  "email": "student@university.edu.vn",
  "password": "Password123!",
  "providerId": "LOCAL",
  "firstName": "Van A",
  "lastName": "Nguyen",
  "studentId": "B21DCCN001",
  "gender": "MALE",
  "kNumber": 21,
  "associationId": 1,
  "majorId": 2
}
```
- **Validation Rules**:
  - `email`: Required, minimum 6 characters.
  - `password`: Required, minimum 6 characters.
  - `providerId`: Required, not blank (e.g. `"LOCAL"` or Google OAuth provider ID).
  - `firstName`, `lastName`, `studentId`, `gender`: Required, not blank.
  - `kNumber`: Positive integer, max 100.
  - `associationId`, `majorId`: Positive integers.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```
- **Errors**: `400 Bad Request` (`VALIDATION_ERROR`), `409 Conflict` (`DUPLICATE_RESOURCE_ERROR` if email already exists).

---

#### 2.1.4 Register Fanpage Admin
- **Endpoint**: `POST /auth/register/fanpage-admin`
- **Access**: `PUBLIC`
- **Description**: Register an account with role `FANPAGE_ADMIN` and manager profile.
- **Request Body**:
```json
{
  "email": "clubadmin@university.edu.vn",
  "password": "Password123!",
  "providerId": "LOCAL",
  "firstName": "Thi B",
  "lastName": "Tran",
  "staffId": "NV001",
  "orgName": "IT Youth Club",
  "title": "Club Leader",
  "DoB": "2002-05-15T00:00:00Z",
  "gender": "FEMALE",
  "phoneNumber": "0987654321"
}
```
- **Validation Rules**:
  - `email`, `password`: Min 6 characters.
  - `staffId`, `orgName`, `title`, `DoB`, `phoneNumber`: Required, not blank.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.1.5 Logout
- **Endpoint**: `POST /auth/logout`
- **Access**: `AUTHENTICATED`
- **Description**: Invalidates refresh token session in database and clears both `ddd_jwt` and `ddd_refresh` cookies (`Max-Age=0`).
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.1.6 Change Password
- **Endpoint**: `POST /auth/change-password`
- **Access**: `AUTHENTICATED`
- **Description**: Change the account password.
- **Request Body**:
```json
{
  "oldPassword": "OldPassword123!",
  "newPassword": "NewStrongPassword456!"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.1.7 Send OTP
- **Endpoint**: `POST /auth/send-otp`
- **Access**: `PUBLIC`
- **Description**: Sends a 6-digit one-time password (OTP) verification email to the user.
- **Request Body**:
```json
{
  "email": "student@university.edu.vn"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.1.8 Verify OTP
- **Endpoint**: `POST /auth/verify-otp`
- **Access**: `PUBLIC`
- **Description**: Verifies the 6-digit OTP code sent to the given email address.
- **Request Body**:
```json
{
  "email": "student@university.edu.vn",
  "otpCode": "123456"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.1.9 Google OAuth2 Login
- **Endpoint**: `POST /auth/login/google`
- **Access**: `PUBLIC`
- **Description**: Authenticate using a Google OAuth2 ID Token. On success, sets `ddd_jwt` and `ddd_refresh` HttpOnly cookies and returns the user login profile payload.
- **Request Body**:
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```
- **Response Headers**:
  - `Set-Cookie: ddd_jwt=...; Path=/; HttpOnly; Max-Age=...; SameSite=Lax`
  - `Set-Cookie: ddd_refresh=...; Path=/; HttpOnly; Max-Age=...; SameSite=Lax`
- **Response `200 OK`**:
```json
{
  "data": {
    "userId": 1,
    "email": "student@university.edu.vn",
    "role": "STUDENT"
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

### 2.2 Users & Profiles (`UserController` - `/users`)

#### 2.2.1 Get Current Student Profile
- **Endpoint**: `GET /users/me/profile`
- **Access**: `AUTHENTICATED` (Student)
- **Description**: Retrieve detailed student profile information of the currently logged-in user.
- **Response `200 OK`**:
```json
{
  "data": {
    "email": "student@university.edu.vn",
    "role": "STUDENT",
    "isActive": true,
    "firstName": "Van A",
    "lastName": "Nguyen",
    "studentId": "B21DCCN001",
    "gender": "MALE",
    "kNumber": "21",
    "avatarUrl": "https://cdn.university.edu.vn/avatars/b21dccn001.png",
    "associationName": "Doan Thanh Nien",
    "majorName": "Software Engineering",
    "majorCode": "7480103",
    "schoolName": "Information Technology"
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.2.2 Admin - Get All Student Profiles (Paginated)
- **Endpoint**: `GET /users/admin/profiles`
- **Access**: `ROLE_ADMIN`
- **Query Parameters**:
  - `isActive` (boolean, default: `true`)
  - `page` (integer, default: `0`)
  - `size` (integer, default: `10`)
  - `sortBy` (string, default: `"createdAt"`)
  - `sortDir` (string, default: `"desc"`)
- **Response `200 OK`**: `BaseResponse<Page<StudentProfileResponseDto>>`

---

#### 2.2.3 Admin - Get All Users (Paginated)
- **Endpoint**: `GET /users/admin`
- **Access**: `ROLE_ADMIN`
- **Query Parameters**:
  - `isActive` (boolean, default: `true`)
  - `page` (integer, default: `0`)
  - `size` (integer, default: `10`)
  - `sortBy` (string, default: `"createdAt"`)
  - `sortDir` (string, default: `"desc"`)
- **Response `200 OK`**: `BaseResponse<Page<UserResponseDto>>`
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "email": "student@university.edu.vn",
        "role": "STUDENT",
        "isActive": true
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.2.4 User Profile Updates & Admin Management
- **`GET /users/search-users?keyword={keyword}`**: Search active users by name or email (e.g. for inviting members).
  - Access: `AUTHENTICATED`
  - Query Param: `keyword` (optional string)
  - Response: `BaseResponse<List<UserResponseDto>>`
- **`PUT /users/me/profile`**: Update current student profile details.
- **`PATCH /users/me/profile/change-avatar`**: Update current student avatar.
  - Body: `{ "avatarUrl": "https://example.com/new-avatar.png" }`
- **`DELETE /users/me/out-fanpage-member/{fanpageId}`**: Leave / resign as a member from a fanpage.
  - Access: `AUTHENTICATED`
  - Response: `BaseResponse<Void>`
- **`GET /users/admin/{id}/profile`**: Retrieve student profile by ID.
- **`PUT /users/admin/{id}/profile`**: Admin update student profile.
- **`PATCH /users/admin/{id}/change-status`**: Toggle user account active status.
- **`PATCH /users/admin/{id}/change-role?role={ROLE_NAME}`**: Admin change single user role (`ADMIN`, `FANPAGE_ADMIN`, `STUDENT`).
- **`PATCH /users/admin/{id}/change-roles`**: Admin change user multiple roles.
  - Body: `["ADMIN", "FANPAGE_ADMIN"]`

---

### 2.3 Events (`EventController` - `/events`)

#### 2.3.1 Create Event
- **Endpoint**: `POST /events/fanpage-member`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Description**: Publish a new event with quota, dates, and training point allocations.
- **Request Body**:
```json
{
  "name": "Tech Seminar 2026",
  "description": "Annual Technology Innovation Conference",
  "location": "Auditorium Hall A",
  "dateOpen": "2026-10-15",
  "dateClose": "2026-10-25",
  "dateHappen": "2026-10-30",
  "capacity": 200,
  "maleQuantity": 100,
  "femaleQuantity": 100,
  "bannerUrl": "https://cdn.example.com/banner-tech2026.jpg",
  "eventTypeId": 1,
  "criteriaId": 2,
  "fanpageId": 5,
  "semesterId": 1,
  "eventPoints": [
    {
      "pointCategoryId": 10,
      "point": 5
    }
  ]
}
```
- **Validation Rules**:
  - `name`, `description`, `location`, `bannerUrl`: Required, not blank.
  - `dateOpen`, `dateClose`, `dateHappen`: Required dates (`YYYY-MM-DD`).
  - `capacity`: Positive integer.
  - `maleQuantity`, `femaleQuantity`: Integer >= 0.
  - `eventTypeId`, `fanpageId`, `semesterId`: Positive integers.
  - `eventPoints`: List of point categories and assigned score.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.3.2 Search Events (Public)
- **Endpoint**: `GET /events` or `GET /events/`
- **Access**: `PUBLIC`
- **Description**: Search active events available for registration and view (defaults to `isActive = true` and `status = 'PUBLISHED'`).
- **Query Parameters (`EventFilterRequestDto`)**:
  - `keyword`: string (optional fuzzy search matched against `name` and `description`)
  - `pointCategoryIds`: List<Long> (e.g. `?pointCategoryIds=63` or `?pointCategoryIds=63,64` / `?pointCategoryIds=63&pointCategoryIds=64`)
  - `eventTypeIds`: List<Long> (e.g. `?eventTypeIds=1`)
  - `criteriaIds`: List<Long> (e.g. `?criteriaIds=2`)
  - `semesterId`: Long (optional filter by semester)
  - `dateHappenFrom`: Instant (optional ISO-8601 string, e.g. `2026-10-01T00:00:00Z`)
  - `dateHappenTo`: Instant (optional ISO-8601 string, e.g. `2026-10-31T23:59:59Z`)
  - `status`: string (default: `"PUBLISHED"`. Public search forces active events)
  - `isActive`: boolean (default: `true`)
  - `page`: integer (default: `0`)
  - `size`: integer (default: `10`)
  - `sortBy`: string (default: `"createdAt"`)
  - `sortDir`: string (default: `"desc"`)

> [!WARNING]
> Filter parameter names for IDs with multiple values are **plural** (`pointCategoryIds`, `eventTypeIds`, `criteriaIds`). Passing a singular form such as `?pointCategoryId=63` will **not** bind to the DTO and will result in the filter being ignored (returning all events).

- **Response `200 OK`**: `BaseResponse<Page<EventResponseDto>>`

---

#### 2.3.3 Admin - Search Events
- **Endpoint**: `GET /events/admin`
- **Access**: `ROLE_ADMIN`
- **Description**: Search events with full administrative visibility (drafts, pending, published, closed, or inactive events).
- **Query Parameters**: Same filter model as `EventFilterRequestDto`, allowing viewing of drafts, pending approvals, and closed events (`status=DRAFT`, `status=PUBLISHED`, `status=CLOSED`, `isActive=false`, etc.).
- **Response `200 OK`**: `BaseResponse<Page<EventResponseDto>>`

---

#### 2.3.4 Get Event by ID
- **Endpoint**: `GET /events/{id}`
- **Access**: `PUBLIC`
- **Description**: Get complete event details, semester, and point allocation breakdown.
- **Response `200 OK`**:
```json
{
  "data": {
    "id": 12,
    "name": "Tech Seminar 2026",
    "description": "Annual Technology Innovation Conference",
    "location": "Auditorium Hall A",
    "dateOpen": "2026-10-15",
    "dateClose": "2026-10-25",
    "dateHappen": "2026-10-30",
    "capacity": 200,
    "maleQuantity": 100,
    "femaleQuantity": 100,
    "bannerUrl": "https://cdn.example.com/banner-tech2026.jpg",
    "status": "APPROVED",
    "eventTypeName": "Workshop",
    "criteriaName": "Scientific Research & Academic Activities",
    "fanpageName": "IT Youth Club",
    "fanpageId": 5,
    "isActive": true,
    "semester": {
      "id": 1,
      "number": 1,
      "academicYear": "2026-2027",
      "startDate": "2026-09-01",
      "endDate": "2027-01-15",
      "status": "ACTIVE"
    },
    "eventPoints": [
      {
        "pointCategoryId": 10,
        "pointCategoryName": "Participation in seminars",
        "point": 5
      }
    ],
    "updatedAt": "2026-10-12T09:30:00Z"
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.3.5 Get Events by Fanpage ID (Paginated)
- **Endpoint**: `GET /events/fanpage/{fanpageId}`
- **Access**: `PUBLIC`
- **Query Parameters**: `page` (default 0), `size` (default 10), `sortBy` (default `"createdAt"`), `sortDir` (default `"desc"`)
- **Response `200 OK`**: `BaseResponse<Page<EventResponseDto>>`

---

#### 2.3.6 Update Event
- **Endpoint**: `PUT /events/fanpage-member/{id}`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Request Body**: `EventUpdateRequestDto`
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.3.7 Change Event Status
- **Endpoint**: `PATCH /events/fanpage-member/{id}/status?status={status}`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Query Parameters**: `status` (Enum: `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `OPEN_REGISTRATION`, `CLOSED_REGISTRATION`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`)
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.3.8 Delete Event
- **Endpoint**: `DELETE /events/admin/{id}` or `DELETE /events/fanpage-member/{id}`
- **Access**: `ROLE_ADMIN` or Fanpage Member
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.3.9 Store / Bookmark Event
- **Endpoint**: `POST /events/{eventId}/store`
- **Access**: `AUTHENTICATED` (Student)
- **Description**: Bookmark an event to the student's personal saved list.
- **Path Parameter**: `eventId` (Long) - ID of the active event to bookmark.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```
- **Errors**:
  - `404 Not Found`: Event does not exist or is inactive.
  - `409 Conflict`: Event is already stored.

---

#### 2.3.10 Unstore / Remove Stored Event
- **Endpoint**: `DELETE /events/{eventId}/store`
- **Access**: `AUTHENTICATED` (Student)
- **Description**: Remove/un-bookmark an event from the student's stored collection (hard delete).
- **Path Parameter**: `eventId` (Long) - ID of the event to un-save.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```
- **Errors**:
  - `404 Not Found`: Stored record not found.

---

#### 2.3.11 Check If Event Is Stored
- **Endpoint**: `GET /events/{eventId}/is-stored`
- **Access**: `AUTHENTICATED` (Student)
- **Description**: Checks whether the authenticated student has bookmarked the given event.
- **Path Parameter**: `eventId` (Long)
- **Response `200 OK`**:
```json
{
  "data": {
    "eventId": 12,
    "isStored": true
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.3.12 Get Student's Stored Events (Paginated)
- **Endpoint**: `GET /events/me/stored` (or `GET /events/stored`)
- **Access**: `AUTHENTICATED` (Student)
- **Description**: Retrieves a paginated list of active events bookmarked by the student.
- **Query Parameters**:
  - `page`: integer (default: `0`)
  - `size`: integer (default: `10`)
  - `sortBy`: string (default: `"createdAt"`)
  - `sortDir`: string (default: `"desc"`)
- **Response `200 OK`**: `BaseResponse<Page<EventResponseDto>>`

---

### 2.4 Student Registrations & Check-in (`RegistrationController` - `/registrations`)

#### 2.4.1 Register for an Event
- **Endpoint**: `POST /registrations/me/events/{eventId}`
- **Access**: `ROLE_STUDENT`
- **Description**: Registers the current authenticated student for the event. Validates quota, gender limitations, and registration window.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```
- **Errors**:
  - `400 Bad Request`: Event registration closed or event full.
  - `409 Conflict`: Student already registered for this event.

---

#### 2.4.2 Cancel Event Registration
- **Endpoint**: `PATCH /registrations/me/events/{eventId}/cancel`
- **Access**: `ROLE_STUDENT`
- **Request Body**:
```json
{
  "reason": "Unexpected conflict with semester examination schedule"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.4.3 Get Registration History (Paginated)
- **Endpoint**: `GET /registrations/me/history`
- **Access**: `ROLE_STUDENT`
- **Query Parameters**: `page` (default 0), `size` (default 10)
- **Response `200 OK`**:
```json
{
  "data": {
    "content": [
      {
        "eventId": 12,
        "regDate": "2026-10-16T08:00:00Z",
        "status": "CHECKED_IN",
        "reason": null,
        "checkInAt": "2026-10-30T07:45:00Z",
        "checkOutAt": null,
        "evidenceUrl": null,
        "eventName": "Tech Seminar 2026",
        "location": "Auditorium Hall A",
        "dateOpen": "2026-10-15T00:00:00Z",
        "dateClose": "2026-10-25T00:00:00Z",
        "dateHappen": "2026-10-30T00:00:00Z",
        "bannerUrl": "https://cdn.example.com/banner-tech2026.jpg"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  },
  "message": "Success",
  "exceptionCode": null
}
```
- **Possible `status` values**: `REGISTERED`, `CHECKED_IN`, `COMPLETED`, `CANCELLED`, `REJECTED`.

---

#### 2.4.4 Get Upcoming Registered Events
- **Endpoint**: `GET /registrations/me/upcoming`
- **Access**: `ROLE_STUDENT`
- **Response `200 OK`**: `BaseResponse<List<RegistrationResponseDto>>`

---

#### 2.4.5 Student Self Check-in
- **Endpoint**: `PATCH /registrations/me/events/{eventId}/check-in`
- **Access**: `ROLE_STUDENT`
- **Description**: Records student presence at event check-in window.
- **Request Body (Optional)**:
```json
{
  "proofUrl": "https://s3.amazonaws.com/event-proofs/checkin_user101.jpg"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.4.6 Student Self Check-out (with Proof Photo)
- **Endpoint**: `PATCH /registrations/me/events/{eventId}/check-out`
- **Access**: `ROLE_STUDENT`
- **Description**: Submits self check-out with proof photo URL (e.g. uploaded photo or selfie at event).
- **Request Body (Optional)**:
```json
{
  "proofUrl": "https://s3.amazonaws.com/event-proofs/checkout_user101_event12.jpg"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.4.7 Organizer - Get Event Attendees (Paginated)
- **Endpoint**: `GET /registrations/fanpage-member/fanpages/{fanpageId}/events/{eventId}/attendees`
- **Access**: `ROLE_FANPAGE_ADMIN` / Organizer
- **Query Parameters**: `page` (default 0), `size` (default 10), `sortBy` (default `"regDate"`), `sortDir` (default `"asc"`)
- **Response `200 OK`**: `BaseResponse<List<AttendeeResponseDto>>`
```json
{
  "data": [
    {
      "userId": 101,
      "status": "REGISTERED",
      "regDate": "2026-10-18T10:00:00Z",
      "email": "student@university.edu.vn",
      "firstName": "Van A",
      "lastName": "Nguyen",
      "gender": "MALE",
      "avatarUrl": "https://cdn.example.com/avatar.png"
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.4.8 Organizer - Attendance Management (Single & Batch)
- **Check-in Students**: `PATCH /registrations/fanpage-member/events/{eventId}/check-in`
- **Check-out Students**: `PATCH /registrations/fanpage-member/events/{eventId}/check-out`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member (Organizer)
- **Description**: Mark attendance for one or more students who registered for the event. Passing a single ID in `studentUserIds` processes that individual student.
- **Path Variable**: `eventId` (Long)
- **Request Body (`BatchAttendanceRequestDto`)**:
```json
{
  "studentUserIds": [101, 102, 105]
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

### 2.5 Fanpages & Organizations (`FanpageController` - `/fanpages`)

#### 2.5.1 Search Fanpages (Public)
- **Endpoint**: `GET /fanpages` or `GET /fanpages/`
- **Access**: `PUBLIC`
- **Description**: Search and list approved, active fanpages (clubs, associations, student unions). Forces filter `status=ACTIVE` and `isActive=true`.
- **Query Parameters (`FanpageFilterRequestDto`)**:
  - `keyword`: string (search by name, orgName)
  - `orgType`: string (`CLUB`, `UNION`, `ASSOCIATION`)
  - `page`: integer (default: `0`)
  - `size`: integer (default: `10`)
  - `sortBy`: string (default: `"createdAt"`)
  - `sortDir`: string (default: `"desc"`)
- **Response `200 OK`**: `BaseResponse<Page<FanpageResponseDto>>`

---

#### 2.5.2 Admin - Search Fanpages
- **Endpoint**: `GET /fanpages/admin` or `GET /fanpages/admin/`
- **Access**: `ROLE_ADMIN`
- **Description**: Search all fanpages with unfiltered access to `PENDING`, `BANNED`, or inactive statuses.
- **Query Parameters**: Same as `FanpageFilterRequestDto` with `status` and `isActive` configurable.
- **Response `200 OK`**: `BaseResponse<Page<FanpageResponseDto>>`

---

#### 2.5.3 Get Fanpage Detail
- **Endpoint**: `GET /fanpages/{fanpageId}`
- **Access**: `PUBLIC` / `AUTHENTICATED`
- **Description**: Retrieve fanpage overview. Unapproved fanpages are only viewable by system admins.
- **Response `200 OK`**:
```json
{
  "data": {
    "id": 5,
    "name": "Robotics Club",
    "description": "University Robotics & Automation Student Club",
    "avatarUrl": "https://cdn.example.com/robotics-logo.png",
    "orgType": "CLUB",
    "orgName": "Youth Union",
    "parentOrg": "University Student Association",
    "helpContact": "robotics@university.edu.vn",
    "helpPhone": "0912345678",
    "createdAt": "2026-09-01T00:00:00Z",
    "status": "ACTIVE",
    "isActive": true
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.4 Get My Managed Fanpages
- **Endpoint**: `GET /fanpages/fanpage-admin/my-fanpages` or `GET /fanpages/my-fanpages`
- **Access**: `ROLE_FANPAGE_ADMIN`
- **Description**: Returns all fanpages currently managed by the authenticated fanpage admin.
- **Response `200 OK`**: `BaseResponse<List<FanpageResponseDto>>`

---

#### 2.5.5 Register a Fanpage / Organization
- **Endpoint**: `POST /fanpages/fanpage-admin/register`
- **Access**: `ROLE_FANPAGE_ADMIN`
- **Request Body**:
```json
{
  "name": "Robotics Club",
  "description": "University Robotics & Automation Student Club",
  "avatarUrl": "https://cdn.example.com/robotics-logo.png",
  "orgType": "CLUB",
  "orgName": "Youth Union",
  "parentOrg": "University Student Association",
  "helpContact": "robotics@university.edu.vn",
  "helpPhone": "0912345678"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.6 Update Fanpage by Fanpage Admin
- **Endpoint**: `PUT /fanpages/fanpage-admin/{fanpageId}`
- **Access**: `ROLE_FANPAGE_ADMIN`
- **Description**: Fanpage admins can update description, avatar, and contact info.
- **Request Body**:
```json
{
  "description": "Updated club description and operational guidelines",
  "avatarUrl": "https://cdn.example.com/new-robotics-logo.png",
  "helpContact": "support.robotics@university.edu.vn",
  "helpPhone": "0987654321"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.7 Update Fanpage by System Admin
- **Endpoint**: `PUT /fanpages/admin/{fanpageId}`
- **Access**: `ROLE_ADMIN`
- **Description**: System admins can update core details, status, and activity flag.
- **Request Body**:
```json
{
  "name": "Robotics Club",
  "description": "Updated club description",
  "avatarUrl": "https://cdn.example.com/logo.png",
  "orgType": "CLUB",
  "orgName": "Youth Union",
  "parentOrg": "University Student Association",
  "helpContact": "robotics@university.edu.vn",
  "helpPhone": "0912345678",
  "status": "ACTIVE",
  "isActive": true
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.8 Delete Fanpage (Admin)
- **Endpoint**: `DELETE /fanpages/admin/{fanpageId}`
- **Access**: `ROLE_ADMIN`
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.9 Admin Moderation: Accept, Ban, Unban Fanpage
- **Accept Fanpage**: `POST /fanpages/admin/accept-fanpage/{fanpageId}`
- **Ban Fanpage**: `POST /fanpages/admin/ban-fanpage/{fanpageId}`
- **Unban Fanpage**: `POST /fanpages/admin/unban-fanpage/{fanpageId}`
- **Access**: `ROLE_ADMIN`
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.10 Fanpage Member Management
- **Get Members of Fanpage**:
  - `GET /fanpages/{fanpageId}/members`
  - Access: `PUBLIC` / `AUTHENTICATED`
  - Response:
```json
{
  "data": [
    {
      "userId": 50,
      "email": "lead@university.edu.vn",
      "firstName": "Bao",
      "lastName": "Le",
      "avatarUrl": "https://cdn.example.com/avatar.png",
      "role": "ADMIN"
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```
- **Invite Member**: `POST /fanpages/fanpage-admin/{fanpageId}/invite-member?email={email}`
  - Access: `ROLE_FANPAGE_ADMIN`
  - Description: Send an invitation to a registered user by email to join the fanpage team. Creates a pending invitation record in `/fanpage-member-invitations`.
- **Remove Member**: `DELETE /fanpages/fanpage-admin/{fanpageId}/remove-member?email={email}`
  - Access: `ROLE_FANPAGE_ADMIN`
  - Description: Remove a member from the fanpage team by their email.
- **Appoint Fanpage Admin**: `PATCH /fanpages/fanpage-admin/{fanpageId}/appoint-fanpage-admin?email={email}`
  - Access: `ROLE_FANPAGE_ADMIN`
  - Description: Promote an existing member to Fanpage Admin role for this organization.

---

#### 2.5.11 Follow Fanpage
- **Endpoint**: `POST /fanpages/{fanpageId}/follow`
- **Access**: `AUTHENTICATED`
- **Description**: Follow a specific fanpage.
- **Path Variable**: `fanpageId` (Long)
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.12 Unfollow Fanpage
- **Endpoints**:
  - `DELETE /fanpages/{fanpageId}/follow`
  - `POST /fanpages/{fanpageId}/unfollow` (convenience alias)
- **Access**: `AUTHENTICATED`
- **Description**: Unfollow a specific fanpage.
- **Path Variable**: `fanpageId` (Long)
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.13 Get Follower Count & Status
- **Endpoint**: `GET /fanpages/{fanpageId}/followers/count`
- **Access**: `PUBLIC` / `AUTHENTICATED` (If authenticated, `isFollowing` reflects the current user's follow status)
- **Path Variable**: `fanpageId` (Long)
- **Response `200 OK`**:
```json
{
  "data": {
    "fanpageId": 1,
    "followerCount": 128,
    "isFollowing": true
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.14 Get Followed Fanpages
- **Endpoint**: `GET /fanpages/following`
- **Access**: `AUTHENTICATED`
- **Description**: Retrieve paginated list of fanpages currently followed by the authenticated user.
- **Query Parameters**:
  - `page` (integer, default `0`)
  - `size` (integer, default `10`)
  - `sortBy` (string, default `createdAt`)
  - `sortDir` (string, default `DESC`)
- **Response `200 OK`**: `BaseResponse<PageResponse<FanpageResponseDto>>`
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Google Developer Student Clubs",
        "description": "Tech student community",
        "avatarUrl": "https://cdn.example.com/gdsc.png",
        "coverUrl": "https://cdn.example.com/cover.png",
        "contactEmail": "gdsc@university.edu.vn",
        "status": "ACTIVE",
        "orgType": "CLUB",
        "createdAt": "2026-01-10T08:00:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": { "sorted": true, "empty": false, "unsorted": false },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "size": 10,
    "number": 0,
    "numberOfElements": 1,
    "first": true,
    "empty": false
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

### 2.6 Semesters (`SemesterController` - `/semesters`)

#### 2.6.1 Get Active Semesters
- **Endpoint**: `GET /semesters` or `GET /semesters/`
- **Access**: `PUBLIC`
- **Response `200 OK`**:
```json
{
  "data": [
    {
      "id": 1,
      "number": 1,
      "academicYear": "2026-2027",
      "startDate": "2026-09-01",
      "endDate": "2027-01-15",
      "status": "ACTIVE"
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.6.2 Get Current Semester
- **Endpoint**: `GET /semesters/current`
- **Access**: `PUBLIC`
- **Response `200 OK`**: `BaseResponse<SemesterResponseDto>`

---

#### 2.6.3 Get Semester by ID
- **Endpoint**: `GET /semesters/{id}`
- **Access**: `PUBLIC`
- **Response `200 OK`**: `BaseResponse<SemesterResponseDto>`

---

#### 2.6.4 Admin - Semester Management
- **List All Semesters**: `GET /semesters/admin` or `GET /semesters/admin/`
- **Create Semester**: `POST /semesters/admin` or `POST /semesters/admin/`
  - Body:
```json
{
  "number": 1,
  "academicYear": "2026-2027",
  "startDate": "2026-09-01",
  "endDate": "2027-01-15"
}
```
- **Update Semester**: `PUT /semesters/admin/{id}`
- **Delete Semester**: `DELETE /semesters/admin/{id}`

---

### 2.7 Training Point Categories (`PointCategoryController` - `/point-categories`)

#### 2.7.1 List All Point Categories
- **Endpoint**: `GET /point-categories`
- **Access**: `PUBLIC`
- **Response `200 OK`**:
```json
{
  "data": [
    {
      "id": 1,
      "name": "Criterion 1: Academic & Study Activities",
      "note": "Maximum 20 points per semester",
      "maximum": 20,
      "dateApply": "2026-08-01",
      "level": 1,
      "parentCategoryId": null,
      "isActive": true,
      "childrenOrder": 1
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.7.2 Get Point Category by ID
- **Endpoint**: `GET /point-categories/{id}`
- **Access**: `PUBLIC`
- **Response `200 OK`**: `BaseResponse<PointCategoryResponseDto>`

---

#### 2.7.3 Get Point Categories by Level
- **Endpoint**: `GET /point-categories/level?level={level}`
- **Access**: `PUBLIC`
- **Query Parameter**: `level` (integer, default: `1`). Level 1 (main criterion), Level 2 (sub-group), Level 3 (specific activity).

---

#### 2.7.4 Get Point Categories by Parent Category ID
- **Endpoint**: `GET /point-categories/parentId?parentId={parentId}`
- **Access**: `PUBLIC`
- **Query Parameter**: `parentId` (Long, default: `0`).

---

#### 2.7.5 Admin - Point Category Management
- **Create Category**: `POST /point-categories/admin`
  - Body:
```json
{
  "name": "Participating in Coding Competitions",
  "note": "First prize gets full points",
  "maximum": 10,
  "dateApply": "2026-08-01",
  "parentCategoryId": 1,
  "childrenOrder": 2
}
```
- **Update Category**: `PATCH /point-categories/admin/{id}`
- **Delete Category**: `DELETE /point-categories/admin/{id}`

---

### 2.8 Evaluation Criteria (`CriteriaController` - `/criterias`)

#### 2.8.1 Get All Criteria
- **Endpoint**: `GET /criterias` or `GET /criterias/`
- **Access**: `PUBLIC`
- **Query Parameters**:
  - `isActive`: boolean (default: `true`)
  - `parentId`: Long (optional)
  - `level`: Integer (optional)
- **Response `200 OK`**:
```json
{
  "data": [
    {
      "id": 1,
      "name": "Political & Social Ideology",
      "note": "Compliance with university regulations and national laws",
      "scope": "UNIVERSITY",
      "level": 1,
      "childrenOrder": 1,
      "parentId": null,
      "isActive": true
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.8.2 Get Criteria by Parent ID
- **Endpoint**: `GET /criterias/parentId?parentId={parentId}`
- **Access**: `PUBLIC`
- **Query Parameter**: `parentId` (default: `0`)
- **Response `200 OK`**: `BaseResponse<List<CriteriaResponseDto>>`

---

#### 2.8.3 Get Criteria by Level
- **Endpoint**: `GET /criterias/level?level={level}`
- **Access**: `PUBLIC`
- **Query Parameter**: `level` (default: `0`)
- **Response `200 OK`**: `BaseResponse<List<CriteriaResponseDto>>`

---

#### 2.8.4 Get Criteria by ID
- **Endpoint**: `GET /criterias/{id}`
- **Access**: `PUBLIC`
- **Response `200 OK`**: `BaseResponse<CriteriaResponseDto>`

---

#### 2.8.5 Admin - Criteria Management
- **Create Criteria**: `POST /criterias/admin`
  - Body:
```json
{
  "name": "Community Volunteering",
  "note": "Green Summer and Blood Donation drives",
  "scope": "COMMUNITY",
  "childrenOrder": 1,
  "parentId": 1
}
```
- **Update Criteria**: `PUT /criterias/admin/{id}`
- **Toggle Active Status**: `PATCH /criterias/admin/{id}/change-status`
- **Delete Criteria**: `DELETE /criterias/admin/{id}`

---

### 2.9 Training Point Transactions (`PointTransactionController` - `/point-transactions`)

#### 2.9.1 Get Student Point Transactions
- **Endpoint**: `GET /point-transactions/me/semesters/{semesterId}/point-categories/{pointCategoryId}`
- **Access**: `ROLE_STUDENT`
- **Description**: Returns all point award transactions earned by the logged-in student for a specified semester and point category.
- **Response `200 OK`**:
```json
{
  "data": [
    {
      "id": 201,
      "pointCategoryId": 10,
      "semesterId": 1,
      "eventId": 12,
      "eventName": "Tech Seminar 2026",
      "points": 5,
      "type": "EVENT",
      "status": "APPROVED",
      "description": "Attended tech seminar with verified check-in/out",
      "createdAt": "2026-10-30T10:30:00Z"
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.9.2 Create Point Transaction for Student
- **Endpoint**: `POST /point-transactions/me`
- **Access**: `ROLE_STUDENT` / Authenticated
- **Description**: Submits a point transaction request for the authenticated user.
- **Request Body**:
```json
{
  "userId": 101,
  "semesterId": 1,
  "pointCategoryId": 10,
  "eventId": 12,
  "points": 5,
  "description": "Participated in university programming contest"
}
```
- **Validation Rules**:
  - `semesterId`: Required, positive integer.
  - `pointCategoryId`: Required, positive integer.
  - `points`: Required, positive integer.
  - `userId`, `eventId`: Optional, must be positive if supplied.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

### 2.10 Student Semester Point Aggregates (`StudentSemesterPointController` - `/student-semester-points`)

#### 2.10.1 Get Semester Total Points by Categories
- **Endpoint**: `GET /student-semester-points/me/semesters/{semesterId}`
- **Access**: `ROLE_STUDENT`
- **Description**: Retrieves aggregated total points by category earned by the authenticated student in a semester.
- **Response `200 OK`**:
```json
{
  "data": [
    {
      "id": 501,
      "pointCategoryId": 1,
      "totalPoint": 18,
      "finalizedAt": "2027-01-20T00:00:00Z"
    },
    {
      "id": 502,
      "pointCategoryId": 2,
      "totalPoint": 20,
      "finalizedAt": null
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

### 2.11 Master Data: Schools, Majors, Associations & Event Types

#### 2.11.1 Schools (`SchoolController` - `/schools`)
- `GET /schools?isActive={boolean}`: List all schools.
- `GET /schools/{id}`: Detailed school profile including list of linked majors (`SchoolDetailResponseDto`).
- `POST /schools/admin`: Create school (`CreateSchoolRequestDto`: `{ "name": "...", "code": "..." }`).
- `PUT /schools/admin/{id}`: Update school (`UpdateSchoolRequestDto`).
- `DELETE /schools/admin/{id}`: Delete school.

#### 2.11.2 Majors (`MajorController` - `/majors`)
- `GET /majors`: List all academic majors.
- `GET /majors/{id}`: Major detail.
- `POST /majors/admin`: Create major (`CreateMajorRequestDto`: `{ "name": "Computer Science", "code": "CS", "schoolId": 1 }`).
- `PUT /majors/admin/{id}`: Update major (`UpdateMajorRequestDto`).
- `DELETE /majors/admin/{id}`: Delete major.

#### 2.11.3 Youth Associations (`AssociationController` - `/associations`)
- `GET /associations`: List youth associations and union branches.
- `GET /associations/{id}`: Association detail.
- `POST /associations/admin`: Create association (`CreateAssociationRequestDto`: `{ "name": "...", "code": "..." }`).
- `PUT /associations/admin/{id}`: Update association (`UpdateAssociationRequestDto`).
- `DELETE /associations/admin/{id}`: Delete association.

#### 2.11.4 Event Types (`EventTypeController` - `/event-types`)
- `GET /event-types`: List event classifications (Workshop, Seminar, Competition, Volunteer).
- `GET /event-types/{id}`: Event type detail.
- `POST /event-types/admin`: Create event type (`CreateEventTypeRequestDto`: `{ "name": "...", "description": "..." }`).
- `PUT /event-types/admin/{id}`: Update event type (`UpdateEventTypeRequestDto`).
- `DELETE /event-types/admin/{id}`: Delete event type.

---

### 2.12 Roles Management (`RoleController` - `/api/v1/roles`)
- `GET /api/v1/roles`: List all system roles (`ADMIN`, `FANPAGE_ADMIN`, `STUDENT`).
- `GET /api/v1/roles/{id}`: Get role by ID.
- `POST /api/v1/roles`: Create role (`RoleRequestDto`: `{ "name": "...", "description": "..." }`).
- `PUT /api/v1/roles/{id}`: Update role (`RoleRequestDto`).
- `DELETE /api/v1/roles/{id}`: Delete role.

---

### 2.13 Academic Years (`AcademicYearController` - `/academic-years`)

#### 2.13.1 Get All Active Academic Years
- **Endpoint**: `GET /academic-years` or `GET /academic-years/`
- **Access**: `PUBLIC`
- **Response `200 OK`**: `BaseResponse<List<AcademicYearResponseDto>>`

#### 2.13.2 Get Current Academic Year
- **Endpoint**: `GET /academic-years/current`
- **Access**: `PUBLIC`
- **Response `200 OK`**: `BaseResponse<AcademicYearResponseDto>`

---

### 2.14 Student Criteria Progress (`StudentCriteriaProgressController` - `/criteria-progress`)

All endpoints in this controller require the user to have the `ROLE_STUDENT` authority.

#### 2.14.1 Get Student Criteria Progress Tree
- **Endpoint**: `GET /criteria-progress/me/academic-years/{academicYearId}`
- **Access**: `ROLE_STUDENT` (requires JWT cookie)
- **Path Variable**: `academicYearId` (Long, required)
- **Response `200 OK`**: `BaseResponse<StudentCriteriaProgressResponseDto>`
- **Description**: Returns the hierarchical evaluation criteria tree and completion statistics for the authenticated student in the specified academic year.
- **Response Example**:
```json
{
  "data": {
    "academicYearId": 1,
    "academicYear": "2026-2027",
    "completedMainCriteria": 2,
    "totalMainCriteria": 5,
    "criterias": [
      {
        "id": 1,
        "name": "Đạo đức tốt",
        "note": "Tiêu chuẩn rèn luyện đạo đức tác phong",
        "scope": "FACULTY",
        "level": 1,
        "childrenOrder": 1,
        "requiredChildren": 2,
        "completedChildrenCount": 2,
        "isCompleted": true,
        "children": [
          {
            "id": 11,
            "name": "Điểm rèn luyện đạt loại Tốt trở lên",
            "note": null,
            "scope": "FACULTY",
            "level": 2,
            "childrenOrder": 1,
            "requiredChildren": null,
            "completedChildrenCount": 0,
            "isCompleted": true,
            "children": []
          }
        ]
      }
    ]
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.14.2 Get Specific Criterion Progress Node
- **Endpoint**: `GET /criteria-progress/me/criterias/{criteriaId}/academic-years/{academicYearId}`
- **Access**: `ROLE_STUDENT` (requires JWT cookie)
- **Path Variables**:
  - `criteriaId` (Long, required)
  - `academicYearId` (Long, required)
- **Response `200 OK`**: `BaseResponse<CriteriaProgressItemResponseDto>`
- **Description**: Retrieves detailed evaluation progress for an individual criterion node (and its sub-criteria tree) for the authenticated student.
- **Response Example**:
```json
{
  "data": {
    "id": 11,
    "name": "Điểm rèn luyện đạt loại Tốt trở lên",
    "note": null,
    "scope": "FACULTY",
    "level": 2,
    "childrenOrder": 1,
    "requiredChildren": null,
    "completedChildrenCount": 0,
    "isCompleted": true,
    "children": []
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

### 2.15 Criteria Transactions (`CriteriaTransactionController` - `/criteria-transactions`)

#### 2.15.1 Create Criteria Transaction (Student Self-Report / Submission)
- **Endpoint**: `POST /criteria-transactions/me`
- **Access**: `AUTHENTICATED` (Student)
- **Description**: Submit a criteria progress transaction / evidence note for a specific criterion in an academic year.
- **Request Body**:
```json
{
  "criteriaId": 11,
  "academicYearId": 1,
  "note": "Participated in faculty youth volunteer cleanup project"
}
```
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.15.2 Get Criteria Transactions for Student
- **Endpoint**: `GET /criteria-transactions/me/criterias/{criteriaId}/academic-years/{academicYearId}`
- **Access**: `AUTHENTICATED` (Student)
- **Path Variables**:
  - `criteriaId` (Long, required)
  - `academicYearId` (Long, required)
- **Response `200 OK`**: `BaseResponse<List<CriteriaTransactionResponseDto>>`
- **Response Example**:
```json
{
  "data": [
    {
      "id": 101,
      "userId": 1,
      "criteriaId": 11,
      "academicYearId": 1,
      "eventId": 50,
      "eventName": "Youth Spring Campaign 2026",
      "completionSource": "EVENT_CHECKOUT",
      "note": "Attendance checked out automatically",
      "isActive": true,
      "createdAt": "2026-03-20T10:00:00Z"
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

### 2.16 Media & Direct Cloud Uploads (`MediaController` - `/media`)

#### 2.16.1 Request Presigned Upload Parameters
- **Endpoint**: `POST /media/presign`
- **Access**: `PUBLIC` / `AUTHENTICATED`
- **Description**: Generate signed parameters and authorization credentials to directly upload images/files to Cloudinary from frontend clients without routing heavy binary payloads through the application server.
- **Request Body** (optional):
```json
{
  "category": "AVATAR",
  "fileExtension": "png"
}
```
  - `category` (enum): `AVATAR`, `FANPAGE`, `EVENT`, `EVIDENCE`, `CERTIFICATE`, `GENERAL`
  - `fileExtension` (string, optional): e.g. `png`, `jpg`, `webp`
- **Response `200 OK`**:
```json
{
  "data": {
    "uploadUrl": "https://api.cloudinary.com/v1_1/cloud_name/image/upload",
    "apiKey": "123456789012345",
    "timestamp": 1793673600,
    "signature": "b1b2c3d4e5f67890...",
    "folder": "avatars",
    "assetFolder": "avatars",
    "publicId": "avatars/abc-123-uuid",
    "cloudName": "cloud_name"
  },
  "message": "Success",
  "exceptionCode": null
}
```

#### Uploading to Cloudinary with Presigned Payload (Frontend Example):
```typescript
const presignRes = await apiClient.post('/media/presign', {
  category: 'AVATAR',
  fileExtension: 'png'
});
const { uploadUrl, apiKey, timestamp, signature, folder, publicId } = presignRes.data.data;

const formData = new FormData();
formData.append('file', fileObject);
formData.append('api_key', apiKey);
formData.append('timestamp', timestamp.toString());
formData.append('signature', signature);
formData.append('folder', folder);
formData.append('public_id', publicId);

const uploadRes = await fetch(uploadUrl, {
  method: 'POST',
  body: formData
});
const cloudinaryData = await uploadRes.json();
const finalImageUrl = cloudinaryData.secure_url;
```

---

#### 2.16.2 Delete Uploaded Media
- **Endpoint**: `DELETE /media`
- **Access**: `PUBLIC` / `AUTHENTICATED`
- **Description**: Delete an uploaded media asset from Cloudinary storage by its `publicId`.
- **Query Parameter**:
  - `publicId` (string, required): The Cloudinary public ID (e.g. `avatars/abc-123-uuid`).
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

### 2.17 Fanpage Member Invitations (`FanpageMemberInvitationController` - `/fanpage-member-invitations`)

#### 2.17.1 Get My Pending Invitations
- **Endpoint**: `GET /fanpage-member-invitations/me`
- **Access**: `AUTHENTICATED`
- **Description**: Retrieve all membership invitations sent to the currently authenticated user.
- **Response `200 OK`**:
```json
{
  "data": [
    {
      "id": 1,
      "fanpageId": 5,
      "fanpageName": "Google Developer Student Clubs",
      "fanpageAvatarUrl": "https://cdn.example.com/gdsc-avatar.png",
      "userId": 101,
      "userEmail": "student@university.edu.vn",
      "userFirstName": "Van A",
      "userLastName": "Nguyen",
      "userAvatarUrl": "https://cdn.example.com/student.png",
      "inviterName": "Tran Van B",
      "status": "PENDING",
      "createdAt": "2026-11-01T10:00:00Z",
      "updatedAt": "2026-11-01T10:00:00Z"
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.17.2 Accept Fanpage Invitation
- **Endpoint**: `POST /fanpage-member-invitations/{invitationId}/accept`
- **Access**: `AUTHENTICATED` (Invited Student)
- **Description**: Accept a pending invitation to join the fanpage team. Automatically assigns the `FANPAGE_ADMIN` or member role in that fanpage.
- **Path Variable**: `invitationId` (Long, required)
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.17.3 Decline Fanpage Invitation
- **Endpoint**: `POST /fanpage-member-invitations/{invitationId}/decline`
- **Access**: `AUTHENTICATED` (Invited Student)
- **Description**: Decline a pending invitation.
- **Path Variable**: `invitationId` (Long, required)
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.17.4 Cancel Fanpage Invitation
- **Endpoint**: `POST /fanpage-member-invitations/{invitationId}/cancel`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Description**: Cancel a previously issued invitation.
- **Path Variable**: `invitationId` (Long, required)
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.17.5 Get Invitations by Fanpage ID (Paginated)
- **Endpoint**: `GET /fanpage-member-invitations/fanpage/{fanpageId}`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Description**: List all membership invitations associated with a specific fanpage, filterable by status.
- **Path Variable**: `fanpageId` (Long, required)
- **Query Parameters**:
  - `status`: string (`PENDING` (default), `ACCEPTED`, `REJECTED`, `CANCELED`)
  - `page`: integer (default: `0`)
  - `size`: integer (default: `10`)
  - `sortBy`: string (default: `"createdAt"`)
  - `sortDir`: string (default: `"DESC"`)
- **Response `200 OK`**: `BaseResponse<Page<FanpageMemberInvitationResponseDto>>`

---

### 2.18 Real-time Notifications & SSE (`NotificationController` - `/notifications`)

#### 2.18.1 Server-Sent Events (SSE) Stream
- **Endpoint**: `GET /notifications/stream`
- **Access**: `AUTHENTICATED`
- **Headers**:
  - `Accept: text/event-stream`
- **Description**: Connects to the real-time notification push stream. The server keeps this connection open and pushes events as they happen (moderation approvals, event notifications, invitations).
- **TypeScript / Frontend Client Example**:
```typescript
const eventSource = new EventSource('http://localhost:8080/notifications/stream', {
  withCredentials: true
});

eventSource.onmessage = (event) => {
  const notification: NotificationResponseDto = JSON.parse(event.data);
  console.log('Real-time notification received:', notification);
};

eventSource.onerror = (err) => {
  console.error('SSE connection error:', err);
};
```

---

#### 2.18.2 Get My Notifications (Paginated)
- **Endpoint**: `GET /notifications`
- **Access**: `AUTHENTICATED`
- **Description**: Retrieve paginated notification history for the authenticated user.
- **Query Parameters**:
  - `status`: string (optional: `UNREAD`, `READ`, `ARCHIVED`, `SENT`)
  - `page`: integer (default: `0`)
  - `size`: integer (default: `10`)
  - `sortBy`: string (default: `"createdAt"`)
  - `sortDir`: string (default: `"desc"`)
- **Response `200 OK`**: `BaseResponse<Page<NotificationResponseDto>>`

---

#### 2.18.3 Get My Unread Notification Count
- **Endpoint**: `GET /notifications/unread-count`
- **Access**: `AUTHENTICATED`
- **Description**: Retrieve the total unread notification count for the user (for navbar badges).
- **Response `200 OK`**:
```json
{
  "data": {
    "unreadCount": 3
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.18.4 Mark Notification as Read
- **Endpoint**: `PATCH /notifications/{id}/read`
- **Access**: `AUTHENTICATED`
- **Path Variable**: `id` (Long, required)
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.18.5 Mark All My Notifications as Read
- **Endpoint**: `PATCH /notifications/read-all`
- **Access**: `AUTHENTICATED`
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.18.6 Delete Notification
- **Endpoint**: `DELETE /notifications/{id}`
- **Access**: `AUTHENTICATED`
- **Path Variable**: `id` (Long, required)
- **Response `200 OK`**: `BaseResponse<Void>`

---

#### 2.18.7 Get Fanpage Notifications (Paginated)
- **Endpoint**: `GET /notifications/fanpages/{fanpageId}`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Description**: Retrieve notification history scoped to an organization / fanpage.
- **Path Variable**: `fanpageId` (Long, required)
- **Query Parameters**:
  - `status`: string (optional: `UNREAD`, `READ`, `ARCHIVED`, `SENT`)
  - `page`: integer (default: `0`)
  - `size`: integer (default: `10`)
  - `sortBy`: string (default: `"createdAt"`)
  - `sortDir`: string (default: `"desc"`)
- **Response `200 OK`**: `BaseResponse<Page<NotificationResponseDto>>`

---

#### 2.18.8 Get Fanpage Unread Notification Count
- **Endpoint**: `GET /notifications/fanpages/{fanpageId}/unread-count`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Path Variable**: `fanpageId` (Long, required)
- **Response `200 OK`**:
```json
{
  "data": {
    "unreadCount": 1
  },
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.18.9 Mark Fanpage Notification as Read
- **Endpoint**: `PATCH /notifications/fanpages/{fanpageId}/{id}/read`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Path Variables**: `fanpageId` (Long), `id` (Long)
- **Response `200 OK`**: `BaseResponse<Void>`

---

#### 2.18.10 Mark All Fanpage Notifications as Read
- **Endpoint**: `PATCH /notifications/fanpages/{fanpageId}/read-all`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Path Variable**: `fanpageId` (Long)
- **Response `200 OK`**: `BaseResponse<Void>`

---

#### 2.18.11 Delete Fanpage Notification
- **Endpoint**: `DELETE /notifications/fanpages/{fanpageId}/{id}`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Path Variables**: `fanpageId` (Long), `id` (Long)
- **Response `200 OK`**: `BaseResponse<Void>`

---

## 3. Frontend Type Definitions Summary (TypeScript)

```typescript
// Envelope
export interface BaseResponse<T> {
  data: T | null;
  message: string;
  exceptionCode: string | null;
}

// Auth
export interface LoginResponseDto {
  userId: number;
  email: string;
  role: 'ADMIN' | 'FANPAGE_ADMIN' | 'STUDENT';
}

export interface SendOtpRequestDto {
  email: string;
}

export interface VerifyOtpRequestDto {
  email: string;
  otpCode: string;
}

export interface GoogleLoginRequestDto {
  idToken: string;
}

// User Profile
export interface StudentProfileResponseDto {
  email: string;
  role: string;
  isActive: boolean;
  firstName: string;
  lastName: string;
  studentId: string;
  gender: string;
  kNumber: string;
  avatarUrl: string;
  associationName: string;
  majorName: string;
  majorCode: string;
  schoolName: string;
}

export interface UserResponseDto {
  id: number;
  email: string;
  role: string;
  isActive: boolean;
}

// Fanpage
export interface FanpageResponseDto {
  id: number;
  name: string;
  description: string;
  avatarUrl: string;
  orgType: string;
  orgName: string;
  parentOrg: string;
  helpContact?: string;
  helpPhone?: string;
  createdAt: string;
  status: 'ACTIVE' | 'PENDING' | 'BANNED';
  isActive: boolean;
}

export interface FanpageFollowCountResponseDto {
  fanpageId: number;
  followerCount: number;
  isFollowing: boolean;
}

export interface FanpageMemberResponseDto {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  avatarUrl: string;
  role: string;
}

export interface FanpageAdminUpdateRequestDto {
  description?: string;
  avatarUrl?: string;
  helpContact?: string;
  helpPhone?: string;
}

export interface FanpageSystemAdminUpdateRequestDto {
  name: string;
  description: string;
  avatarUrl: string;
  orgType: string;
  orgName: string;
  parentOrg: string;
  helpContact?: string;
  helpPhone?: string;
  status: string;
  isActive: boolean;
}

// Event
export interface EventPointDto {
  pointCategoryId: number;
  pointCategoryName?: string;
  point: number;
}

export interface EventResponseDto {
  id: number;
  name: string;
  description: string;
  location: string;
  dateOpen: string;
  dateClose: string;
  dateHappen: string;
  capacity: number;
  maleQuantity: number;
  femaleQuantity: number;
  bannerUrl: string;
  status: string;
  eventTypeName: string;
  criteriaName: string;
  fanpageName: string;
  fanpageId: number;
  isActive: boolean;
  semester: SemesterResponseDto;
  eventPoints: EventPointDto[];
  updatedAt: string;
}

// Semester
export interface SemesterResponseDto {
  id: number;
  number: number;
  academicYear: string;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'UPCOMING' | 'CLOSED';
}

// Registration & Attendance
export interface RegistrationResponseDto {
  eventId: number;
  regDate: string;
  status: 'REGISTERED' | 'CHECKED_IN' | 'COMPLETED' | 'CANCELLED' | 'REJECTED';
  reason?: string;
  checkInAt?: string;
  checkOutAt?: string;
  evidenceUrl?: string;
  eventName: string;
  location: string;
  dateOpen: string;
  dateClose: string;
  dateHappen: string;
  bannerUrl: string;
}

export interface AttendeeResponseDto {
  userId: number;
  status: string;
  regDate: string;
  email: string;
  firstName: string;
  lastName: string;
  gender: string;
  avatarUrl?: string;
}

export interface BatchAttendanceRequestDto {
  studentUserIds: number[];
}

// Point Categories & Criteria
export interface PointCategoryResponseDto {
  id: number;
  name: string;
  note: string;
  maximum: number;
  dateApply: string;
  level: number;
  parentCategoryId?: number | null;
  isActive: boolean;
  childrenOrder: number;
}

export interface CriteriaResponseDto {
  id: number;
  name: string;
  note: string;
  scope: string;
  level: number;
  childrenOrder: number;
  parentId?: number | null;
  isActive: boolean;
}

export interface CriteriaRequestDto {
  name: string;
  note?: string;
  scope: string;
  childrenOrder: number;
  parentId?: number | null;
}

// Point Transactions & Student Points
export interface PointTransactionRequestDto {
  userId?: number;
  semesterId: number;
  pointCategoryId: number;
  eventId?: number;
  points: number;
  description?: string;
}

export interface PointTransactionResponseDto {
  id: number;
  pointCategoryId: number;
  semesterId: number;
  eventId?: number;
  eventName?: string;
  points: number;
  type: string;
  status: string;
  description?: string;
  createdAt: string;
}

export interface StudentSemesterPointResponseDto {
  id: number;
  pointCategoryId: number;
  totalPoint: number;
  finalizedAt?: string | null;
}

// Academic Year
export interface AcademicYearResponseDto {
  id: number;
  code: string;
  startDate: string;
  endDate: string;
  isActive: boolean;
}

// Student Criteria Progress
export interface CriteriaProgressItemResponseDto {
  id: number;
  name: string;
  note?: string | null;
  scope?: string | null;
  level: number;
  childrenOrder: number;
  requiredChildren?: number | null;
  completedChildrenCount: number;
  isCompleted: boolean;
  children: CriteriaProgressItemResponseDto[];
}

export interface StudentCriteriaProgressResponseDto {
  academicYearId: number;
  academicYear: string;
  completedMainCriteria: number;
  totalMainCriteria: number;
  criterias: CriteriaProgressItemResponseDto[];
}

// Criteria Transactions
export interface CriteriaTransactionRequestDto {
  criteriaId: number;
  academicYearId: number;
  note: string;
}

export interface CriteriaTransactionResponseDto {
  id: number;
  userId: number;
  criteriaId: number;
  academicYearId: number;
  eventId?: number | null;
  eventName?: string | null;
  completionSource: string;
  note?: string | null;
  isActive: boolean;
  createdAt: string;
}

// Media
export type MediaCategoryEnum = 'AVATAR' | 'FANPAGE' | 'EVENT' | 'EVIDENCE' | 'CERTIFICATE' | 'GENERAL';

export interface PresignedRequestDto {
  category?: MediaCategoryEnum;
  fileExtension?: string;
}

export interface PresignedResponseDto {
  uploadUrl: string;
  apiKey: string;
  timestamp: number;
  signature: string;
  folder: string;
  assetFolder: string;
  publicId: string;
  cloudName: string;
}

// User Profile Actions
export interface UpdateAvatarRequestDto {
  avatarUrl: string;
}

// Event Query Filters
export interface EventFilterRequestDto {
  keyword?: string;
  status?: string;
  eventTypeIds?: number[];
  semesterId?: number;
  criteriaIds?: number[];
  pointCategoryIds?: number[];
  dateHappenFrom?: string; // ISO-8601
  dateHappenTo?: string;   // ISO-8601
  isActive?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

// Attendance
export interface BatchAttendanceRequestDto {
  studentUserIds: number[];
}

// Fanpage Follow & Invitations
export interface FanpageFollowCountResponseDto {
  fanpageId: number;
  followerCount: number;
  isFollowing: boolean;
}

export type InvatitationStatusEnum = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED';

export interface FanpageMemberInvitationResponseDto {
  id: number;
  fanpageId: number;
  fanpageName: string;
  fanpageAvatarUrl: string;
  userId: number;
  userEmail: string;
  userFirstName: string;
  userLastName: string;
  userAvatarUrl: string;
  inviterName: string;
  status: InvatitationStatusEnum | string;
  createdAt: string;
  updatedAt: string;
}

// Notifications
export type NotificationStatusEnum = 'UNREAD' | 'READ' | 'ARCHIVED' | 'SENT';

export type NotificationTypeEnum =
  | 'FANPAGE_APPROVED'
  | 'FANPAGE_REJECTED'
  | 'FANPAGE_MEMBER_INVITED'
  | 'FANPAGE_INVITATION_ACCEPTED'
  | 'FANPAGE_INVITATION_REJECTED'
  | 'FANPAGE_ADMIN_APPOINTED'
  | 'EVENT_CREATED'
  | 'EVENT_CHECKOUT_AWARDED'
  | 'SYSTEM';

export interface NotificationResponseDto {
  id: number;
  type: NotificationTypeEnum;
  message: string;
  status: NotificationStatusEnum;
  iconUrl?: string | null;
  url?: string | null;
  fanpageId?: number | null;
  createdAt: string;
}

export interface UnreadCountResponseDto {
  unreadCount: number;
}
```
