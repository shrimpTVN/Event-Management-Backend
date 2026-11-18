# Progressive Registration and Profile Gating — Design Specification

> **Document Version**: 1.0.0  
> **Date**: 2026-11-14  
> **Status**: Ready for Review  
> **Target Path**: `docs/superpowers/specs/2026-11-14-progressive-registration-and-profile-gating-design.md`

---

## 1. Executive Summary & Goals

### 1.1 Problem Statement
Currently, registration (`POST /auth/register`) tightly couples account creation with student profile creation, forcing users to provide `studentId`, `kNumber`, `majorId`, `associationId`, and `gender` up front. Similarly, Google OAuth2 login (`POST /auth/login/google`) fails with `ResourceNotFoundException` if the user is not pre-registered in the database. This creates significant onboarding friction and drop-offs.

### 1.2 Proposed Solution
Implement **Progressive Onboarding & Profile Gating**:
1. **Low-Friction Sign-Up**:
   - **Google OAuth 2.0 (`POST /auth/login/google`)**: Automatically provisions a new user record in the `users` table just-in-time from token claims (`email`, `firstName`, `lastName`, `avatarUrl`, `googleId`) with default `STUDENT` role. Does not require a profile up front.
   - **Email & Password (`POST /auth/register`)**: Minimal credentials payload: `email`, `password`, `otpCode`, `firstName`, `lastName`. Verifies the OTP, hashes the password, and creates the base user with role `STUDENT`. Skips student profile creation.
2. **Deferred Profile Completion**:
   - Users can fill in their `StudentProfile` (`studentId`, `kNumber`, `majorId`) or `FanpageAdminProfile` (`staffId`, `orgName`, `title`, `dob`, `phoneNumber`) later via user profile endpoints.
3. **Gated Downstream Operations**:
   - **Event Registration (`POST /registrations/me/events/{eventId}`)**: Caller must have a complete `StudentProfile`. Otherwise, rejected with HTTP 403 Forbidden and error code `STUDENT_PROFILE_REQUIRED`.
   - **Fanpage Creation (`POST /fanpages/fanpage-admin/register`)**: Caller must have a complete `FanpageAdminProfile`. Otherwise, rejected with HTTP 403 Forbidden and error code `FANPAGE_ADMIN_PROFILE_REQUIRED`.

---

## 2. Architecture & Data Flow

```
[ Unregistered User ]
       │
       ├── Google OAuth ────────────────────────────────────────┐
       │   POST /auth/login/google                              │
       │   (verifies idToken, creates User JIT, issues JWT)     │
       │                                                        ▼
       └── Email + Password + OTP ────────────────────────▶ [ Active User ]
           POST /auth/register                               (Base User in DB,
           (verifies OTP, hashes password, issues JWT)        role: STUDENT)
                                                                │
                     ┌──────────────────────────────────────────┴──────────────────────────────────────────┐
                     │                                                                                     │
                     ▼                                                                                     ▼
           [ Register For Event ]                                                               [ Create Fanpage ]
         POST /registrations/me/events/{id}                                                  POST /fanpages/fanpage-admin/register
                     │                                                                                     │
                     ▼                                                                                     ▼
           Has complete StudentProfile?                                                        Has complete FanpageAdminProfile?
           ├─ YES ──▶ Proceed with Registration                                                ├─ YES ──▶ Proceed with Fanpage Creation
           └─ NO  ──▶ Throw StudentProfileRequiredException                                    └─ NO  ──▶ Throw FanpageAdminProfileRequiredException
                      (HTTP 403, Code: STUDENT_PROFILE_REQUIRED)                                          (HTTP 403, Code: FANPAGE_ADMIN_PROFILE_REQUIRED)
```

---

## 3. Detailed Component Design

### 3.1 Domain Layer (`ddd-domain`)

#### Exceptions
- `StudentProfileRequiredException`:
  - Location: `com.ddd.domain.exception.StudentProfileRequiredException`
  - Extends: `BusinessException` (or `AuthException`)
  - Message: `"Please complete your student profile before registering for events."`
- `FanpageAdminProfileRequiredException`:
  - Location: `com.ddd.domain.exception.FanpageAdminProfileRequiredException`
  - Extends: `BusinessException` (or `AuthException`)
  - Message: `"Please complete your fanpage admin profile before creating a fanpage."`

---

### 3.2 Application Layer (`ddd-application`)

#### 1. Quick Registration & Google OAuth2 in `AuthService` / `AuthServiceImpl`
- **`register(RegisterUserDto dto)`**:
  1. Verify OTP: `otpCommandService.verifyOtp(dto.email(), dto.otpCode())`.
  2. Validate email uniqueness: `userRepository.findByEmail(dto.email()) != null` -> throw `DuplicateResourceException("Email already exists")`.
  3. Create `User`:
     - `email = dto.email()`
     - `password = passwordEncoder.encode(dto.password())`
     - `firstName = dto.firstName()`
     - `lastName = dto.lastName()`
     - `providerId = "local"`
     - Role: Assign `RoleEnum.STUDENT.name()`
  4. Save `User` via `userCommandService.createUser(userDto, RoleEnum.STUDENT.name())`.
  5. Do NOT create `StudentProfile`.

- **`loginWithGoogle(String idToken)`**:
  1. Verify Google token via `googleTokenVerifier.verifyToken(idToken)`.
  2. Look up `userRepository.findByEmail(googleUserInfo.email())`.
  3. If user does NOT exist:
     - Automatically create new `User`:
       - `email = googleUserInfo.email()`
       - `firstName = googleUserInfo.givenName()`
       - `lastName = googleUserInfo.familyName()`
       - `avatarUrl = googleUserInfo.picture()`
       - `providerId = googleUserInfo.googleId()`
       - Role: Assign `RoleEnum.STUDENT.name()`
     - Persist user via `userRepository.save(user)`.
  4. If user exists:
     - If `providerId` is null, link Google ID: `user.setProviderId(googleUserInfo.googleId())`.
  5. Issue JWT and Refresh tokens as normal.

#### 2. Event Registration Gating in `RegistrationCommandServiceImpl`
- In `registerEvent(Long eventId)`:
  - Retrieve current user ID via `securityContextPort.getCurrentUserId()`.
  - Validate student profile presence and completeness:
    - Query `studentProfileRepository.findByUserId(userId)` (or check via repository).
    - If profile is `null` OR `studentId` is blank OR `kNumber == null` OR `majorId == null`:
      - Throw `new StudentProfileRequiredException()`.
  - Proceed with existing event registration capacity and timestamp checks.

#### 3. Fanpage Creation Gating in `FanpageCommandServiceImpl`
- In `createFanpage(FanpageRegisterDto registerDto, String email)`:
  - Look up user by email or `securityContextPort.getCurrentUserId()`.
  - Validate fanpage admin profile presence and completeness:
    - Query `fanpageAdminProfileRepository.findByUserId(userId)`.
    - If profile is `null` OR `staffId` is blank OR `orgName` is blank:
      - Throw `new FanpageAdminProfileRequiredException()`.
  - Proceed with existing fanpage creation logic.

#### 4. Profile Completion Endpoints in `UserCommandServiceImpl`
- Existing `updateStudentProfile(email, updateDto)`:
  - Already supports upserting: creates `StudentProfile` lazily if it doesn't exist yet for the user.
- Add `createOrUpdateFanpageAdminProfile(email, updateDto)`:
  - Supports upserting `FanpageAdminProfile` lazily for users wishing to become fanpage admins.

---

### 3.3 API Layer (`ddd-api`)

#### 1. Request DTOs
- Refactor `RegisterRequestDto`:
  ```java
  public record RegisterRequestDto(
          @NotNull @Email String email,
          @NotNull @Size(min = 6) String password,
          @NotNull @NotBlank String otpCode,
          @NotNull @NotBlank String firstName,
          @NotNull @NotBlank String lastName
  ) implements Serializable {}
  ```

#### 2. Controller Endpoints
- `POST /auth/register`:
  - Accepts refactored `RegisterRequestDto`.
  - Delegates to `authService.register(authApiMapper.toUserRegisterDto(userRegisterRequestDto))`.
  - Returns `BaseResponse.ok()`.
- `POST /auth/login/google`:
  - Unchanged signature (`GoogleLoginRequestDto`).
  - Supports seamless account creation for first-time Google sign-ins.

#### 3. Error Codes & Exception Handling
- Update `ErrorCodeEnum`:
  ```java
  STUDENT_PROFILE_REQUIRED("USER.001", HttpStatus.FORBIDDEN, "Student profile is required to perform this action"),
  FANPAGE_ADMIN_PROFILE_REQUIRED("USER.002", HttpStatus.FORBIDDEN, "Fanpage admin profile is required to perform this action")
  ```
- In `GlobalExceptionHandler`:
  ```java
  @ExceptionHandler(StudentProfileRequiredException.class)
  public ResponseEntity<BaseResponse<Void>> handleStudentProfileRequired(StudentProfileRequiredException ex) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(BaseResponse.error(ErrorCodeEnum.STUDENT_PROFILE_REQUIRED));
  }

  @ExceptionHandler(FanpageAdminProfileRequiredException.class)
  public ResponseEntity<BaseResponse<Void>> handleFanpageAdminProfileRequired(FanpageAdminProfileRequiredException ex) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
              .body(BaseResponse.error(ErrorCodeEnum.FANPAGE_ADMIN_PROFILE_REQUIRED));
  }
  ```

---

## 4. Testing Strategy

1. **`AuthServiceImplTest`**:
   - `loginWithGoogle`: First-time user auto-provisioned with `STUDENT` role and valid tokens returned.
   - `loginWithGoogle`: Existing user logs in and links `providerId` if not yet linked.
   - `register`: Successful registration with valid OTP; base user created without profile.
   - `register`: Invalid OTP throws exception; duplicate email throws `DuplicateResourceException`.
2. **`RegistrationCommandServiceImplTest`**:
   - `registerEvent`: Caller without `StudentProfile` throws `StudentProfileRequiredException`.
   - `registerEvent`: Caller with complete `StudentProfile` successfully registers for published open event.
3. **`FanpageCommandServiceImplTest`**:
   - `createFanpage`: Caller without `FanpageAdminProfile` throws `FanpageAdminProfileRequiredException`.
   - `createFanpage`: Caller with complete `FanpageAdminProfile` successfully creates fanpage.
4. **`AuthControllerTest` / `RegistrationControllerTest`**:
   - HTTP 403 returned with error codes `USER.001` and `USER.002` respectively.
