# Backend REST API Documentation (DDD Architecture)

This document provides complete, production-ready specifications for Frontend Developers to integrate with the backend API.

---

## 1. Global Frontend Integration Guide

### 1.1 Base URL & Environment
- **Default Local Development URL**: `http://localhost:8080`
- **Reverse Proxy / Production**: Usually mounted at root `/` or prefixed with `/api` depending on deployment.
- **Port**: `8080` (configured in `application.yml` via `${SERVER_PORT:8080}`).

> [!IMPORTANT]
> Note on Route Prefixing: Most controllers mount directly on root paths (e.g. `/auth`, `/users`, `/events`, `/registrations`, `/fanpages`, `/schools`, `/majors`, `/associations`, `/event-types`, `/semesters`, `/point-categories`, `/point-transactions`, `/student-semester-points`, `/criterias`), while `RoleController` is explicitly prefixed at `/api/v1/roles`.

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
- **`PUT /users/me/profile`**: Update current student profile details.
- **`PATCH /users/me/profile/change-avatar`**: Update current avatar.
  - Body: `{ "avatarUrl": "https://example.com/new-avatar.png" }`
- **`GET /users/admin/{id}/profile?id={id}`**: Retrieve student profile by ID.
- **`PUT /users/admin/{id}/profile?id={id}`**: Admin update student profile.
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
- **Description**: Search active events available for registration and view.
- **Query Parameters (`EventFilterRequestDto`)**:
  - `keyword`: string (search by name, description, location)
  - `semesterId`: Long
  - `fanpageId`: Long
  - `eventTypeId`: Long
  - `criteriaId`: Long
  - `status`: string (defaults to `ACTIVE`)
  - `page`: integer (default: `0`)
  - `size`: integer (default: `10`)
  - `sortBy`: string (default: `"createdAt"`)
  - `sortDir`: string (default: `"desc"`)
- **Response `200 OK`**: `BaseResponse<Page<EventResponseDto>>`

---

#### 2.3.3 Admin - Search Events
- **Endpoint**: `GET /events/admin`
- **Access**: `ROLE_ADMIN`
- **Query Parameters**: Same filter model as `EventFilterRequestDto`, allowing viewing of drafts, pending approvals, and closed events.
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

#### 2.4.8 Organizer - Batch Attendance Actions
- **Batch Check-in**: `PATCH /registrations/fanpage-member/events/{eventId}/check-in`
- **Batch Check-out**: `PATCH /registrations/fanpage-member/events/{eventId}/check-out`
- **Access**: `ROLE_FANPAGE_ADMIN` / Organizer
- **Request Body**:
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

#### 2.4.9 Organizer - Individual Student Attendance Actions
- **Check-in Single Student**: `PATCH /registrations/fanpage-member/events/{eventId}/users/{studentUserId}/check-in`
- **Check-out Single Student**: `PATCH /registrations/fanpage-member/events/{eventId}/users/{studentUserId}/check-out`
- **Access**: `ROLE_FANPAGE_ADMIN` / Organizer
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
- **Add Member**: `POST /fanpages/fanpage-admin/{fanpageId}/add-member/{userId}`
- **Remove Member**: `POST /fanpages/fanpage-admin/{fanpageId}/remove-member/{userId}`
- **Access**: `ROLE_FANPAGE_ADMIN`

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

### 2.14 Student Criteria Progress (`StudentCriteriaProgressController` - `/student`)

#### 2.14.1 Get Student Criteria Progress Tree
- **Endpoint**: `GET /student/criteria-progress`
- **Access**: `STUDENT` (requires JWT cookie)
- **Query Parameter**: `academicYearId` (optional; defaults to current active academic year)
- **Response `200 OK`**: `BaseResponse<StudentCriteriaProgressResponseDto>`
- **Description**: Returns hierarchical criteria tree with completion statuses evaluated based on child criteria and student check-ins.

#### 2.14.2 Get Specific Criterion Progress Node
- **Endpoint**: `GET /student/criteria/{criteriaId}/progress`
- **Access**: `STUDENT` (requires JWT cookie)
- **Query Parameter**: `academicYearId` (optional; defaults to current active academic year)
- **Response `200 OK`**: `BaseResponse<CriteriaProgressNodeResponseDto>`
- **Description**: Evaluates and returns the status, completed children count, and source for a specific node in the criteria tree.

#### 2.14.3 Manually Complete or Uncomplete Leaf Criterion
- **Endpoint**: `PATCH /student/criteria/{criteriaId}/progress`
- **Access**: `STUDENT` (requires JWT cookie)
- **Request Body**:
```json
{
  "academicYearId": 1,
  "completed": true
}
```
- **Rules**:
  - Only leaf criteria can be toggled manually.
  - Inactive criteria cannot be toggled.
  - Criteria verified via `EVENT_CHECKOUT` cannot be manually unchecked or downgraded to `MANUAL`.
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
export interface CriteriaProgressNodeResponseDto {
  criteriaId: number;
  name: string;
  note?: string | null;
  scope?: string | null;
  level: number;
  childrenOrder: number;
  requiredChildren?: number | null;
  isCompleted: boolean;
  completedChildren: number;
  totalChildren: number;
  completionSource?: 'MANUAL' | 'EVENT_CHECKOUT' | 'SYSTEM' | null;
  completedAt?: string | null;
  children: CriteriaProgressNodeResponseDto[];
}

export interface StudentCriteriaProgressResponseDto {
  academicYearId: number;
  academicYear: string;
  completedLeafCriteria: number;
  totalLeafCriteria: number;
  completedMainCriteria: number;
  totalMainCriteria: number;
  criteria: CriteriaProgressNodeResponseDto[];
}

export interface ManualCriteriaProgressRequestDto {
  academicYearId: number;
  completed: boolean;
}
```
