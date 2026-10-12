# Backend REST API Documentation (DDD Architecture)

This document provides complete, production-ready specifications for Frontend Developers to integrate with the backend API.

---

## 1. Global Frontend Integration Guide

### 1.1 Base URL & Environment
- **Default Local Development URL**: `http://localhost:8080`
- **Reverse Proxy / Production**: Usually mounted at root `/` or prefixed with `/api` depending on deployment.
- **Port**: `8080` (configured in `application.yml` via `${SERVER_PORT:8080}`).

> [!IMPORTANT]
> Note on Route Prefixing: Most controllers mount directly on root paths (e.g. `/auth`, `/users`, `/events`, `/fanpages`, `/schools`, `/majors`, `/associations`, `/event-types`, `/semesters`, `/point-categories`, `/criterias`, `/me/registrations`), while `RoleController` is explicitly prefixed at `/api/v1/roles`.

---

### 1.2 Authentication & Cookie Session
The backend uses **JWT token stored inside an HttpOnly Cookie** (`ddd_jwt`):
- Upon successful login (`POST /auth/login`), the server sends a `Set-Cookie: ddd_jwt=...; Path=/; HttpOnly; Max-Age=86400; SameSite=Lax`.
- **Frontend HTTP clients (Axios, Fetch, TanStack Query) MUST enable credentials on all requests:**
  - **Axios**: `axios.defaults.withCredentials = true;` or `{ withCredentials: true }`
  - **Fetch**: `fetch(url, { credentials: 'include' })`
- On logout (`POST /auth/logout`), the server returns a cleared cookie with `Max-Age=0`.

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
  (error) => {
    if (error.response?.status === 401) {
      // Redirect to login or refresh state
      console.warn('Session expired or unauthorized');
    }
    return Promise.reject(error);
  }
);
```

---

### 1.3 Standard Response Envelope (`BaseResponse<T>`)
Every REST endpoint (except empty delete endpoints returning HTTP 200/204) wraps its response payload inside `BaseResponse<T>`:

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
- **Description**: Authenticate with email and password. On success, sets `ddd_jwt` cookie and returns user profile info.
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
- **Header**: `Set-Cookie: ddd_jwt=eyJhbGciOi...; Path=/; HttpOnly`
- **Errors**: `401 Unauthorized` (`AUTH_INVALID_CREDENTIALS`).

---

#### 2.1.2 Register Student
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

#### 2.1.3 Register Fanpage Admin
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

#### 2.1.4 Logout
- **Endpoint**: `POST /auth/logout`
- **Access**: `AUTHENTICATED`
- **Description**: Invalidates session and clears the `ddd_jwt` cookie (`Max-Age=0`).
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.1.5 Change Password
- **Endpoint**: `POST /auth/change-password`
- **Access**: `AUTHENTICATED`
- **Description**: Change the account password.
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
```json
{
  "data": {
    "content": [
      {
        "email": "student@university.edu.vn",
        "role": "STUDENT",
        "isActive": true,
        "firstName": "Van A",
        "lastName": "Nguyen",
        "studentId": "B21DCCN001",
        "gender": "MALE",
        "kNumber": "21",
        "avatarUrl": "https://cdn.example.com/avatar.png",
        "associationName": "Doan Thanh Nien",
        "majorName": "Software Engineering",
        "majorCode": "7480103",
        "schoolName": "Information Technology"
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

#### 2.2.3 Admin - Get All Users (Paginated)
- **Endpoint**: `GET /users/admin`
- **Access**: `ROLE_ADMIN`
- **Query Parameters**:
  - `isActive` (boolean, default: `true`)
  - `page` (integer, default: `0`)
  - `size` (integer, default: `10`)
  - `sortBy` (string, default: `"createdAt"`)
  - `sortDir` (string, default: `"desc"`)
- **Response `200 OK`**:
```json
{
  "data": {
    "content": [
      {
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
- **`PATCH /users/admin/{id}/change-role?role={ROLE_NAME}`**: Admin change user role (e.g. `ADMIN`, `FANPAGE_ADMIN`, `STUDENT`).

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

#### 2.3.2 Get Event by ID
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

#### 2.3.3 Get Events by Fanpage ID (Paginated)
- **Endpoint**: `GET /events/fanpage/{fanpageId}`
- **Access**: `PUBLIC`
- **Query Parameters**: `page` (default 0), `size` (default 10), `sortBy` (default "createdAt"), `sortDir` (default "desc")
- **Response `200 OK`**: `BaseResponse<Page<EventResponseDto>>`

---

#### 2.3.4 Admin - Get All Events (Paginated)
- **Endpoint**: `GET /events/admin`
- **Access**: `ROLE_ADMIN`
- **Query Parameters**: `page` (default 0), `size` (default 10), `sortBy` (default "createdAt"), `sortDir` (default "desc")
- **Response `200 OK`**: `BaseResponse<Page<EventResponseDto>>`

---

#### 2.3.5 Update Event
- **Endpoint**: `PUT /events/fanpage-member/{id}`
- **Access**: `ROLE_FANPAGE_ADMIN` / Fanpage Member
- **Request Body**: `EventUpdateRequestDto` (same schema as create event).
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.3.6 Change Event Status
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

#### 2.3.7 Delete Event
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

### 2.4 Student Registrations & Check-in (`RegistrationController` - `/me/registrations`)

#### 2.4.1 Register for an Event
- **Endpoint**: `POST /me/registrations/events/{eventId}`
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
- **Endpoint**: `PATCH /me/registrations/events/{eventId}/cancel`
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
- **Endpoint**: `GET /me/registrations/history`
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
- **Endpoint**: `GET /me/registrations/upcoming`
- **Access**: `ROLE_STUDENT`
- **Response `200 OK`**: `BaseResponse<List<RegistrationResponseDto>>`

---

#### 2.4.5 Student Self Check-in
- **Endpoint**: `PATCH /me/registrations/events/{eventId}/check-in`
- **Access**: `ROLE_STUDENT`
- **Description**: Records student presence at the event check-in window.
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
- **Endpoint**: `PATCH /me/registrations/events/{eventId}/check-out`
- **Access**: `ROLE_STUDENT`
- **Description**: Submits self check-out with proof photo URL (e.g. uploaded photo or selfie at event).
- **Request Body**:
```json
{
  "proofUrl": "https://s3.amazonaws.com/event-proofs/evidence_user101_event12.jpg"
}
```
- **Validation Rules**: `proofUrl` is required.
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

#### 2.5.1 Register a Fanpage / Organization
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

#### 2.5.2 Admin - Fanpage Moderation
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

#### 2.5.3 Admin/Organizer Check-in Student
- **Endpoint**: `PATCH /fanpages/events/{eventId}/users/{studentUserId}/check-in`
- **Access**: `ROLE_FANPAGE_ADMIN` / Organizer
- **Description**: Event organizer manual check-in for a participant.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.4 Admin/Organizer Check-out Student
- **Endpoint**: `PATCH /fanpages/events/{eventId}/users/{studentUserId}/check-out`
- **Access**: `ROLE_FANPAGE_ADMIN` / Organizer
- **Description**: Event organizer manual check-out for a participant.
- **Response `200 OK`**:
```json
{
  "data": null,
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.5.5 Fanpage Member Management
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
- **List All (including inactive)**: `GET /semesters/admin`
- **Create Semester**: `POST /semesters/admin`
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

#### 2.7.2 Get Point Categories by Level
- **Endpoint**: `GET /point-categories/level?level={level}`
- **Access**: `PUBLIC`
- **Query Parameter**: `level` (integer, default: `1`). Level 1 (main criterion), Level 2 (sub-group), Level 3 (specific activity).

---

#### 2.7.3 Get Point Categories by Parent Category ID
- **Endpoint**: `GET /point-categories/parentId?parentId={parentId}`
- **Access**: `PUBLIC`
- **Query Parameter**: `parentId` (Long, default: `0`).

---

#### 2.7.4 Admin - Point Category Management
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
- **Endpoint**: `GET /criterias?isActive={true|false}`
- **Access**: `PUBLIC`
- **Response `200 OK`**:
```json
{
  "data": [
    {
      "id": 1,
      "name": "Political & Social Ideology",
      "description": "Compliance with university regulations and national laws",
      "scope": "UNIVERSITY",
      "isActive": true
    }
  ],
  "message": "Success",
  "exceptionCode": null
}
```

---

#### 2.8.2 Criteria CRUD
- **Get by ID**: `GET /criterias/{id}`
- **Create**: `POST /criterias`
  - Body:
```json
{
  "name": "Community Volunteering",
  "description": "Green Summer and Blood Donation drives",
  "scope": "COMMUNITY"
}
```
- **Update**: `PUT /criterias/{id}`
- **Toggle Active Status**: `PATCH /criterias/{id}/change-status`
- **Delete**: `DELETE /criterias/{id}`

---

### 2.9 Master Data: Schools, Majors & Associations

#### 2.9.1 Schools (`SchoolController` - `/schools`)
- `GET /schools?isActive={boolean}`: List all schools.
- `GET /schools/{id}`: Detailed school profile including list of linked majors (`SchoolDetailResponseDto`).
- `POST /schools/admin`: Create school (`CreateSchoolRequestDto`: `{ "name": "...", "code": "..." }`).
- `PUT /schools/admin/{id}`: Update school.
- `DELETE /schools/admin/{id}`: Delete school.

#### 2.9.2 Majors (`MajorController` - `/majors`)
- `GET /majors`: List all academic majors.
- `GET /majors/{id}`: Major detail.
- `POST /majors/admin`: Create major (`{ "name": "Computer Science", "code": "CS", "schoolId": 1 }`).
- `PUT /majors/admin/{id}`: Update major.
- `DELETE /majors/admin/{id}`: Delete major.

#### 2.9.3 Youth Associations (`AssociationController` - `/associations`)
- `GET /associations`: List youth associations and union branches.
- `GET /associations/{id}`: Association detail.
- `POST /associations/admin`: Create association (`{ "name": "...", "code": "..." }`).
- `PUT /associations/admin/{id}`: Update association.
- `DELETE /associations/admin/{id}`: Delete association.

#### 2.9.4 Event Types (`EventTypeController` - `/event-types`)
- `GET /event-types`: List event classifications (Workshop, Seminar, Competition, Volunteer).
- `GET /event-types/{id}`: Event type detail.
- `POST /event-types/admin`: Create event type (`{ "name": "...", "description": "..." }`).
- `PUT /event-types/admin/{id}`: Update event type.
- `DELETE /event-types/admin/{id}`: Delete event type.

---

### 2.10 Roles Management (`RoleController` - `/api/v1/roles`)
- `GET /api/v1/roles`: List all system roles (`ADMIN`, `FANPAGE_ADMIN`, `STUDENT`).
- `GET /api/v1/roles/{id}`: Get role by ID.
- `POST /api/v1/roles`: Create role (`{ "name": "...", "description": "..." }`).
- `PUT /api/v1/roles/{id}`: Update role.
- `DELETE /api/v1/roles/{id}`: Delete role.

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

// Registration
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
```
