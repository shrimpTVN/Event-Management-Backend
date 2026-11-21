# Progressive Registration and Profile Gating Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the registration and authentication flow to enable quick onboarding (via Google OAuth 2.0 or Email + Password + OTP) with deferred profile completion, while enforcing strict profile completeness gates on event registration and fanpage creation.

**Architecture:** 
Account creation is decoupled from profile creation in `AuthService`. When signing up via Google OAuth2, accounts are provisioned just-in-time from token claims. When signing up with email/password, the OTP is validated and the base `User` record is created without requiring student profile fields up front. Downstream operations in `RegistrationCommandServiceImpl` and `FanpageCommandServiceImpl` enforce domain guards that throw dedicated HTTP 403 exceptions (`STUDENT_PROFILE_REQUIRED` and `FANPAGE_ADMIN_PROFILE_REQUIRED`) if the corresponding profile is missing or incomplete.

**Tech Stack:** Java 21, Spring Boot 3, MapStruct, Spring Security, JUnit 5, Mockito.

**Spec:** [`docs/superpowers/specs/2026-11-14-progressive-registration-and-profile-gating-design.md`](file:///home/shrimp/code/ddd/docs/superpowers/specs/2026-11-14-progressive-registration-and-profile-gating-design.md)

## Global Constraints
- Domain models and exceptions in `ddd-domain` must have zero framework dependencies (no Spring, no JPA).
- App-layer mappers use MapStruct with `componentModel = "spring"` under `com.ddd.application.mapper`.
- HTTP responses must always be wrapped in `BaseResponse<T>`.
- Error codes must use HTTP 403 Forbidden for gating exceptions with distinct codes: `USER.001` (`STUDENT_PROFILE_REQUIRED`) and `USER.002` (`FANPAGE_ADMIN_PROFILE_REQUIRED`).
- Never delete existing files without explicit authorization.

## Review Focus
1. User logs in with Google for the first time: user account is created with default role `STUDENT`, active status, and tokens are returned.
2. User registers with email/password: fails if OTP is invalid/expired, fails if email is duplicated, succeeds with valid OTP without profile creation.
3. User registers for an event: succeeds if `StudentProfile` exists with non-null `studentId`, `kNumber`, and `majorId`; throws `StudentProfileRequiredException` if profile is missing or fields are incomplete.
4. User registers a fanpage: succeeds if `FanpageAdminProfile` exists with non-null `staffId` and `orgName`; throws `FanpageAdminProfileRequiredException` if missing or fields are incomplete.
5. User completes student profile later via `PUT /users/student-profile`: lazily creates the `StudentProfile` if it didn't exist, unblocking event registration.

---

### Task 1: Domain Exceptions & API Error Codes

**Files:**
- Create: `ddd-domain/src/main/java/com/ddd/domain/exception/StudentProfileRequiredException.java`
- Create: `ddd-domain/src/main/java/com/ddd/domain/exception/FanpageAdminProfileRequiredException.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/common/enums/ErrorCodeEnum.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Produces:
  - `StudentProfileRequiredException`: `public class StudentProfileRequiredException extends BusinessException`
  - `FanpageAdminProfileRequiredException`: `public class FanpageAdminProfileRequiredException extends BusinessException`
  - `ErrorCodeEnum.STUDENT_PROFILE_REQUIRED`: code `"USER.001"`, `HttpStatus.FORBIDDEN`, `"Student profile is required to perform this action"`
  - `ErrorCodeEnum.FANPAGE_ADMIN_PROFILE_REQUIRED`: code `"USER.002"`, `HttpStatus.FORBIDDEN`, `"Fanpage admin profile is required to perform this action"`

- [ ] **Step 1: Create domain exceptions in `ddd-domain`**
  - Create `StudentProfileRequiredException.java` in `com.ddd.domain.exception` extending `BusinessException`. Default message: `"Please complete your student profile before registering for events."`.
  - Create `FanpageAdminProfileRequiredException.java` in `com.ddd.domain.exception` extending `BusinessException`. Default message: `"Please complete your fanpage admin profile before creating a fanpage."`.

- [ ] **Step 2: Add error codes to `ErrorCodeEnum` in `ddd-api`**
  - Add `STUDENT_PROFILE_REQUIRED("USER.001", HttpStatus.FORBIDDEN, "Student profile is required to perform this action")`
  - Add `FANPAGE_ADMIN_PROFILE_REQUIRED("USER.002", HttpStatus.FORBIDDEN, "Fanpage admin profile is required to perform this action")`

- [ ] **Step 3: Add exception handlers in `GlobalExceptionHandler`**
  - Add `@ExceptionHandler(StudentProfileRequiredException.class)` returning `ResponseEntity.status(HttpStatus.FORBIDDEN).body(BaseResponse.error(ErrorCodeEnum.STUDENT_PROFILE_REQUIRED))`.
  - Add `@ExceptionHandler(FanpageAdminProfileRequiredException.class)` returning `ResponseEntity.status(HttpStatus.FORBIDDEN).body(BaseResponse.error(ErrorCodeEnum.FANPAGE_ADMIN_PROFILE_REQUIRED))`.

- [ ] **Step 4: Verify compilation**
  - Run: `./mvnw compile -pl ddd-domain,ddd-api`
  - Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**
  ```bash
  git add ddd-domain/src/main/java/com/ddd/domain/exception/StudentProfileRequiredException.java \
          ddd-domain/src/main/java/com/ddd/domain/exception/FanpageAdminProfileRequiredException.java \
          ddd-api/src/main/java/com/ddd/api/common/enums/ErrorCodeEnum.java \
          ddd-api/src/main/java/com/ddd/api/exception/GlobalExceptionHandler.java
  git commit -m "feat: add StudentProfileRequiredException and FanpageAdminProfileRequiredException"
  ```

---

### Task 2: Refactor Email + Password + OTP Registration

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/auth/req/RegisterRequestDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/mapper/AuthApiMapper.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/AuthService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/impl/AuthServiceImpl.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/AuthController.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/impl/AuthServiceImplTest.java`

**Interfaces:**
- Consumes:
  - `OtpCommandService.verifyOtp(String email, String otpCode)`
  - `UserCommandService.createUser(UserDto userDto, String role)`
- Produces:
  - `RegisterRequestDto(email, password, otpCode, firstName, lastName)`
  - `AuthService.register(UserDto userDto, String otpCode)` (or refactored `createUser(UserDto userDto, String otpCode)`)

- [ ] **Step 1: Write unit tests in `AuthServiceImplTest` for quick registration**
  - Test: `shouldRegisterSuccessfullyWithValidOtp`: verifies OTP, calls `userCommandService.createUser` with `RoleEnum.STUDENT.name()`, and does NOT invoke `studentProfileCommandService`.
  - Test: `shouldThrowExceptionWhenOtpInvalid`: verifies OTP failure halts execution before creating user.

- [ ] **Step 2: Update `RegisterRequestDto` in `ddd-api`**
  - Change `RegisterRequestDto` to:
    ```java
    public record RegisterRequestDto(
            @NotNull @Size(min = 6) String email,
            @NotNull @Size(min = 6) String password,
            @NotNull @NotBlank String otpCode,
            @NotNull @NotBlank String firstName,
            @NotNull @NotBlank String lastName
    ) implements Serializable {}
    ```

- [ ] **Step 3: Update `AuthApiMapper` and `AuthService`**
  - In `AuthService`: add `void register(UserDto userDto, String otpCode)`.
  - In `AuthServiceImpl`:
    - Call `otpCommandService.verifyOtp(userDto.email(), otpCode)`.
    - Call `userCommandService.createUser(userDto, RoleEnum.STUDENT.name())`.
    - Do not call `studentProfileCommandService`.

- [ ] **Step 4: Update `AuthController.register`**
  - Update `register(@RequestBody @Valid RegisterRequestDto requestDto)`:
    - Map `UserDto userDto = authApiMapper.toUserDto(requestDto)`.
    - Call `authService.register(userDto, requestDto.otpCode())`.
    - Return `BaseResponse.ok()`.

- [ ] **Step 5: Run tests and verify**
  - Run: `./mvnw test -pl ddd-application,ddd-api -Dtest=*Auth* -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: All auth tests pass.

- [ ] **Step 6: Commit**
  ```bash
  git add ddd-api/src/main/java/com/ddd/api/dto/auth/req/RegisterRequestDto.java \
          ddd-api/src/main/java/com/ddd/api/mapper/AuthApiMapper.java \
          ddd-application/src/main/java/com/ddd/application/service/AuthService.java \
          ddd-application/src/main/java/com/ddd/application/service/impl/AuthServiceImpl.java \
          ddd-api/src/main/java/com/ddd/api/controller/AuthController.java
  git commit -m "refactor: simplify registration to email, password, and OTP without upfront profile"
  ```

---

### Task 3: Seamless Google OAuth2 First-Time Account Provisioning

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/impl/AuthServiceImpl.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/impl/AuthServiceImplTest.java`

**Interfaces:**
- Consumes:
  - `GoogleTokenVerifier.verifyToken(String idToken)` -> `GoogleUserInfo(email, givenName, familyName, picture, googleId)`
  - `UserRepository.findByEmail(String email)`
  - `UserRepository.save(User user)`
  - `JwtUtil.generateJwtTokenFromUser(User user)`
- Produces:
  - `AuthService.loginWithGoogle(String idToken)` -> `LoginResult` (auto-creates User if not present)

- [ ] **Step 1: Write unit tests in `AuthServiceImplTest` for Google OAuth2 provisioning**
  - Test: `shouldAutoProvisionNewUserOnGoogleLogin`: when `userRepository.findByEmail` returns null, verify new user is created with `email`, `firstName`, `lastName`, `avatarUrl`, `providerId`, role `STUDENT`, and valid `LoginResult` is returned.
  - Test: `shouldLoginExistingGoogleUser`: when user already exists, update `providerId` if missing, and return valid `LoginResult`.

- [ ] **Step 2: Update `AuthServiceImpl.loginWithGoogle`**
  - Modify `loginWithGoogle(String idToken)`:
    ```java
    if (user == null) {
        UserDto newUserDto = new UserDto(
            null, googleUserInfo.email(), null, googleUserInfo.googleId(),
            googleUserInfo.givenName(), googleUserInfo.familyName(),
            null, googleUserInfo.picture(), null, null
        );
        Long userId = userCommandService.createUser(newUserDto, RoleEnum.STUDENT.name());
        user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found after creation: " + userId));
        log.info("Google user:{} auto-provisioned successfully with ID:{}", googleUserInfo.email(), userId);
    } else {
        if (user.getProviderId() == null || user.getProviderId().isBlank()) {
            user.setProviderId(googleUserInfo.googleId());
            userRepository.update(user);
        }
    }
    ```

- [ ] **Step 3: Run tests and verify**
  - Run: `./mvnw test -pl ddd-application -Dtest=AuthServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: PASS

- [ ] **Step 4: Commit**
  ```bash
  git add ddd-application/src/main/java/com/ddd/application/service/impl/AuthServiceImpl.java
  git commit -m "feat: auto-provision new users just-in-time on Google OAuth2 sign-in"
  ```

---

### Task 4: Event Registration Profile Completeness Gate

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImpl.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImplTest.java`

**Interfaces:**
- Consumes:
  - `StudentProfileRepository.findByUserId(Long userId)` (or `studentProfileJpaRepository.findByUserId(Long userId)`)
  - `SecurityContextPort.getCurrentUserId()`
- Produces:
  - Throws `StudentProfileRequiredException` when profile is missing or required fields (`studentId`, `kNumber`, `majorId`) are incomplete.

- [ ] **Step 1: Write unit tests in `RegistrationCommandServiceImplTest` for student profile gating**
  - Test: `shouldThrowStudentProfileRequiredExceptionWhenProfileDoesNotExist`: user has no `StudentProfile` -> throws `StudentProfileRequiredException`.
  - Test: `shouldThrowStudentProfileRequiredExceptionWhenProfileIsIncomplete`: user has `StudentProfile` but `studentId` or `kNumber` or `majorId` is null -> throws `StudentProfileRequiredException`.
  - Test: `shouldProceedWithRegistrationWhenStudentProfileIsComplete`: user has complete `StudentProfile` -> registration proceeds as normal.

- [ ] **Step 2: Add validation guard in `RegistrationCommandServiceImpl.registerEvent`**
  - Inject `StudentProfileRepository studentProfileRepository` (or `StudentProfileJpaRepository`).
  - In `registerEvent(Long eventId)`:
    ```java
    StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
            .orElseThrow(StudentProfileRequiredException::new);
    if (profile.getStudentId() == null || profile.getStudentId().isBlank()
            || profile.getKNumber() == null
            || profile.getMajorId() == null) {
        throw new StudentProfileRequiredException();
    }
    ```

- [ ] **Step 3: Run tests and verify**
  - Run: `./mvnw test -pl ddd-application -Dtest=RegistrationCommandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: PASS

- [ ] **Step 4: Commit**
  ```bash
  git add ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImpl.java
  git commit -m "feat: enforce complete StudentProfile prerequisite for event registration"
  ```

---

### Task 5: Fanpage Creation Profile Completeness Gate

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImpl.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImplTest.java` (or create if needed)

**Interfaces:**
- Consumes:
  - `FanpageAdminProfileJpaRepository.findByUserEmail(String email)` (or `findByUserId`)
- Produces:
  - Throws `FanpageAdminProfileRequiredException` when fanpage admin profile is missing or required fields (`staffId`, `orgName`) are incomplete.

- [ ] **Step 1: Write unit tests for fanpage creation gating**
  - Test: `shouldThrowFanpageAdminProfileRequiredExceptionWhenProfileMissing`: caller without `FanpageAdminProfile` -> throws `FanpageAdminProfileRequiredException`.
  - Test: `shouldAllowFanpageCreationWhenProfileComplete`: caller with complete `FanpageAdminProfile` -> fanpage creation proceeds.

- [ ] **Step 2: Add validation guard in `FanpageCommandServiceImpl.createFanpage`**
  - Inject `FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository`.
  - In `createFanpage(FanpageRegisterDto registerDto, String email)`:
    ```java
    FanpageAdminProfileJpaEntity profile = fanpageAdminProfileJpaRepository.findByUserEmail(email)
            .orElseThrow(FanpageAdminProfileRequiredException::new);
    if (profile.getStaffId() == null || profile.getStaffId().isBlank()
            || profile.getOrgName() == null || profile.getOrgName().isBlank()) {
        throw new FanpageAdminProfileRequiredException();
    }
    ```

- [ ] **Step 3: Run tests and verify**
  - Run: `./mvnw test -pl ddd-application -Dtest=*Fanpage* -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: PASS

- [ ] **Step 4: Commit**
  ```bash
  git add ddd-application/src/main/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImpl.java
  git commit -m "feat: enforce complete FanpageAdminProfile prerequisite for fanpage creation"
  ```

---

### Task 6: Full Verification & Build Check

**Files:**
- Verification across all modules.

- [ ] **Step 1: Run full Maven compile**
  - Run: `./mvnw clean compile`
  - Expected: `BUILD SUCCESS` across all 6 modules (`ddd-domain`, `ddd-infrastructure`, `ddd-application`, `ddd-api`, `ddd-start`, parent).

- [ ] **Step 2: Run all relevant test suites**
  - Run: `./mvnw test -Dtest=AuthServiceImplTest,RegistrationCommandServiceImplTest,FanpageStatisticQueryServiceImplTest,FanpageStatisticControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
  - Expected: 100% tests passing with 0 failures and 0 errors.

- [ ] **Step 3: Final check**
  - Run: `git status` to verify working tree is clean.
