# Implementation of Google OAuth2 & OTP Verification

This document provides a detailed architectural and technical explanation of the **Google OAuth2 Login** flow and the **Email OTP Verification** flow implemented in the application.

---

## Part 1: Google OAuth2 Login Implementation

### 1. Architectural Overview & Design Pattern
Google OAuth2 is integrated using the **Token-Verification Pattern** (also called Client-Side Flow with Server-Side ID Token Validation). Instead of the traditional redirect-heavy Authorization Code flow handled entirely on the backend, the client application (Frontend/Web/Mobile) signs the user in via Google SDK, obtains a **Google ID Token (JWT)**, and sends it to our backend endpoint `POST /api/auth/google`.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Client as Frontend (Google SDK)
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthServiceImpl
    participant GVerifier as GoogleTokenVerifierImpl
    participant GAPI as Google Auth Public Keys
    participant UserCmd as UserCommandService
    participant ProfileCmd as StudentProfileCommandService
    participant Jwt as JwtUtil
    participant DB as PostgreSQL

    User->>Client: Click "Sign in with Google"
    Client->>Client: Google Identity Services SDK prompt
    Client-->>User: User authorizes account
    Client->>AuthCtrl: POST /api/auth/google { idToken: "eyJhbGciOi..." }
    AuthCtrl->>AuthSvc: loginWithGoogle(idToken)
    AuthSvc->>GVerifier: verifyToken(idToken)
    GVerifier->>GAPI: Fetch Google Certs & verify signature & audience
    GVerifier-->>AuthSvc: GoogleUserInfo (email, googleId, name, avatar)
    
    AuthSvc->>DB: userRepository.findByEmail(email)
    alt Case 1: First-time Google user (Auto-registration)
        AuthSvc->>UserCmd: createUser(UserDto, ROLE_STUDENT)
        UserCmd->>DB: Save user (password=null, providerId=googleId)
        AuthSvc->>ProfileCmd: createGoogleStudentProfile(name, avatar, userId)
        ProfileCmd->>DB: Save student profile
    else Case 2: Existing User
        AuthSvc->>ProfileCmd: Ensure profile exists (idempotent)
        AuthSvc->>DB: Update avatarUrl / providerId if missing
    end

    AuthSvc->>Jwt: generateJwtTokenFromUser(user)
    Jwt-->>AuthSvc: Signed JWT Access Token (15m)
    AuthSvc->>AuthSvc: createAndSaveRefreshToken(user.getId())
    AuthSvc->>DB: Save hashed refresh token (7d)
    AuthSvc-->>AuthCtrl: LoginResult(loginDto, jwtToken, rawRefreshToken)
    AuthCtrl-->>Client: 200 OK + Set-Cookie: ddd_jwt + Set-Cookie: ddd_refresh
```

---

### 2. Deep Dive: Google ID Token Verification
- **Port Interface**: `GoogleTokenVerifier` defined in `com.ddd.application.service.googleTokenVerifier`.
- **Implementation**: `GoogleTokenVerifierImpl` uses Google's official client library (`google-api-client:2.7.0`):
  ```java
  HttpTransport transport = new NetHttpTransport();
  JsonFactory jsonFactory = new GsonFactory();

  GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
          .setAudience(Collections.singletonList(clientId))
          .build();

  GoogleIdToken idToken = verifier.verify(idTokenString);
  ```
- **Security Validations Performed by the Verifier**:
  1. **Cryptographic Signature Verification**: Validates the JWT signature against Google's rotating public JSON Web Keys (JWKs).
  2. **Audience Check (`aud`)**: Ensures `aud` matches `@Value("${google.client-id}")`. This prevents a token issued for another application from being accepted by ours.
  3. **Expiration Check (`exp`)**: Ensures the token is not expired.
  4. **Issuer Check (`iss`)**: Ensures the issuer is `accounts.google.com` or `https://accounts.google.com`.

### 3. User Auto-Provisioning & Data Synchronization
When verification succeeds, `GoogleTokenVerifier` returns a `GoogleUserInfo` record containing `email`, `googleId` (Google subject claim `sub`), `firstName`, `lastName`, and `pictureUrl`.

In `AuthServiceImpl`:
- **Auto Registration**: If no `User` exists with that email, a new account is automatically created with:
  - Role: `ROLE_STUDENT`
  - Password: `null` (since authentication is delegated to Google)
  - `providerId`: set to `googleId`
  - `avatarUrl`: set to Google profile image URL
- **Profile Creation**: Creates a minimal `StudentProfile` record linked to the `userId`. Fields like `majorId` and `studentId` are initially null and can be completed later by the student.
- **Issuing Application Credentials**:
  - The domain `User` model is passed to `JwtUtil.generateJwtTokenFromUser(user)` to issue our standard 15-minute `ddd_jwt` cookie.
  - A 7-day refresh token is generated, hashed, and stored via `createAndSaveRefreshToken(user.getId())`, issuing the `ddd_refresh` cookie.
  - **Result**: The Google-authenticated session behaves identically to an email/password session from this point onwards.

---

## Part 2: Email OTP Code Verification Implementation

### 1. Architectural Overview & Workflow
The OTP system provides out-of-band email-based one-time password verification (typically used before password recovery, sensitive changes, or two-factor actions).

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Client
    participant AuthCtrl as AuthController
    participant OtpCmd as OtpCommandServiceImpl
    participant OtpRepo as OtpRepository
    participant MailSvc as EmailServiceImpl
    participant MailServer as SMTP Server (Gmail / Mailjet)
    participant DB as PostgreSQL

    Note over User,DB: STEP 1: Send OTP Flow
    User->>Client: Enter Email and click "Send OTP"
    Client->>AuthCtrl: POST /api/auth/send-otp { email: "user@example.com" }
    AuthCtrl->>OtpCmd: sendOtp("user@example.com")
    OtpCmd->>OtpCmd: SecureRandom generates 6-digit code (e.g., "482915")
    OtpCmd->>OtpCmd: Calculate expiredAt = now() + 5 minutes
    OtpCmd->>OtpRepo: save(Otp{email, otpCode, expiredAt, isUsed=false})
    OtpRepo->>DB: INSERT into otps (...)
    OtpCmd->>MailSvc: sendOtpEmail("user@example.com", "482915")
    MailSvc->>MailServer: Send formatted HTML email via JavaMailSender
    AuthCtrl-->>Client: 200 OK

    Note over User,DB: STEP 2: Verify OTP Flow
    User->>Client: Enters 6-digit code received in email
    Client->>AuthCtrl: POST /api/auth/verify-otp { email: "user@example.com", otpCode: "482915" }
    AuthCtrl->>OtpCmd: verifyOtp("user@example.com", "482915")
    OtpCmd->>OtpRepo: findLatestValidOtp("user@example.com")
    OtpRepo->>DB: Query latest unused OTP for email
    DB-->>OtpCmd: Otp entity
    
    alt OTP Not Found or Already Used
        OtpCmd-->>AuthCtrl: throw BusinessException("OTP not found or already used.")
    else OTP Expired (expiredAt < now())
        OtpCmd-->>AuthCtrl: throw BusinessException("OTP has expired. Please resend a new OTP.")
    else Code Mismatch (otpCode != storedCode)
        OtpCmd-->>AuthCtrl: throw BusinessException("Invalid OTP code.")
    else Valid Match
        OtpCmd->>OtpCmd: otp.setIsUsed(true)
        OtpCmd->>OtpRepo: save(otp)
        OtpRepo->>DB: UPDATE otps SET is_used = true WHERE id = ...
        AuthCtrl-->>Client: 200 OK (OTP successfully consumed)
    end
```

---

### 2. Deep Dive: Key Components & Logic

#### 2.1 Cryptographic Generation
- Uses `java.security.SecureRandom` rather than `java.util.Random` to ensure non-predictability:
  ```java
  private final SecureRandom secureRandom = new SecureRandom();
  String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
  ```
- Produces a zero-padded 6-digit numerical string from `000000` to `999999`.

#### 2.2 Expiration & Lifecycle Window
- **TTL**: 5 minutes (`OTP_EXPIRATION_MINUTES = 5`).
- The expiration timestamp is calculated as `Instant.now().plus(Duration.ofMinutes(5))`.
- Stored in the database column `expired_at` (TIMESTAMP with timezone).

#### 2.3 Single-Use Guarantee (`isUsed`)
- Whenever an OTP is successfully validated against the input code, it is immediately marked as:
  ```java
  otp.setIsUsed(true);
  otpRepository.save(otp);
  ```
- `findLatestValidOtp(email)` only queries for OTPs where `is_used = false`.
- This ensures an OTP cannot be replayed, even if its 5-minute window has not yet elapsed.

#### 2.4 HTML Email Delivery (`EmailServiceImpl`)
- Utilizes Spring Boot's `JavaMailSender` and `MimeMessageHelper`.
- Dispatches a mobile-responsive HTML email template styled with brand colors, clear expiration warnings, and letter-spaced OTP digits for readability.
- Configured via standard Spring Mail properties (`spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`).

---

## Part 3: Summary of Relevant Files & Endpoints

| Component | Layer | File Location | Purpose |
| :--- | :--- | :--- | :--- |
| **`POST /api/auth/google`** | API | `AuthController.java` | Receives Google `idToken`, initiates login/registration, returns cookies |
| **`POST /api/auth/send-otp`** | API | `AuthController.java` | Generates and sends 6-digit code to email |
| **`POST /api/auth/verify-otp`** | API | `AuthController.java` | Validates code, marks OTP as used |
| **`GoogleTokenVerifier`** | Application | `service/googleTokenVerifier/GoogleTokenVerifier.java` | Port interface for token validation |
| **`GoogleTokenVerifierImpl`** | Application | `service/googleTokenVerifier/impl/GoogleTokenVerifierImpl.java` | Google SDK verifier adapter |
| **`OtpCommandService`** | Application | `service/otp/OtpCommandService.java` | Interface for OTP send and verify commands |
| **`OtpCommandServiceImpl`** | Application | `service/otp/impl/OtpCommandServiceImpl.java` | Business logic for generation, TTL check, and consumption |
| **`EmailService`** | Infrastructure | `service/impl/EmailServiceImpl.java` | Sends HTML MIME emails via JavaMailSender |
| **`Otp`** | Domain | `domain/model/Otp.java` | Domain entity modeling OTP attributes |
| **`OtpJpaEntity`** | Infrastructure | `entity/OtpJpaEntity.java` | JPA entity mapping to table `otps` |
