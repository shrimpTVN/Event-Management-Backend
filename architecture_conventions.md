# Architecture & Coding Conventions

> **Purpose**: This document is a living reference for AI agents and developers working on this project.
> It tells you **what already exists**, **how to use it**, and **what NOT to do** (legacy traps).
> Always consult this document before writing new code — do not reinvent things that already exist.

---

## Table of Contents

1. [Module Structure (Multi-Module Maven)](#1-module-structure-multi-module-maven)
2. [DDD Layer Boundaries & Dependency Rules](#2-ddd-layer-boundaries--dependency-rules)
3. [Golden Path: End-to-End Request Flow](#3-golden-path-end-to-end-request-flow)
4. [Shared Components Catalog — Use These, Don't Reinvent](#4-shared-components-catalog--use-these-dont-reinvent)
5. [Service Tier Catalog](#5-service-tier-catalog)
6. [Naming & Package Conventions](#6-naming--package-conventions)
7. [Anti-Patterns & Legacy Traps](#7-anti-patterns--legacy-traps)
8. [MapStruct Mapper Conventions](#8-mapstruct-mapper-conventions)
9. [JPA Entity Conventions](#9-jpa-entity-conventions)

---

## 1. Module Structure (Multi-Module Maven)

```
ddd/
├── ddd-domain         # Pure Java domain layer. ZERO framework dependencies (no Spring, no JPA).
├── ddd-infrastructure # Spring/JPA implementation layer. Implements domain interfaces.
├── ddd-application    # Application services (CQRS), DTOs, app-layer mappers, utils.
├── ddd-api            # REST controllers, API request/response DTOs, API mappers.
└── ddd-start          # Spring Boot entry point only. @EnableJpaAuditing, @EntityScan, etc.
```

### Allowed Dependency Flow

```
ddd-api  →  ddd-application  →  ddd-domain
                ↑                   ↑
        ddd-infrastructure  ────────┘
              ↑
          ddd-start
```

| Module | Can import from | CANNOT import from |
|---|---|---|
| `ddd-domain` | Nothing (pure Java) | Any other module |
| `ddd-infrastructure` | `ddd-domain` | `ddd-application`, `ddd-api` |
| `ddd-application` | `ddd-domain`, `ddd-infrastructure`* | `ddd-api` |
| `ddd-api` | `ddd-application`, `ddd-domain` | `ddd-infrastructure`** |
| `ddd-start` | All modules | — |

> *Application services MAY use `*JpaRepository` directly when no domain `Repository` port is defined yet — but prefer using the domain `Repository` interface via `ddd-domain` port.
> 
> **API controllers MUST NOT import JPA entities or infrastructure classes.

---

## 2. DDD Layer Boundaries & Dependency Rules

### ddd-domain — The Sacred Core

**Contains**: Domain models, repository interfaces, port interfaces, enums, exceptions.

**Rules**:
- All domain models extend `BaseModel` (`com.ddd.domain.model.BaseModel`)
- Repository interfaces live under `com.ddd.domain.repository`
- Port interfaces (non-repository adapters) live under `com.ddd.domain.model.port`
- Exceptions live under `com.ddd.domain.exception` — use existing ones (see §4)
- **NO Spring annotations** — no `@Service`, `@Repository`, `@Component`, etc.
- **NO JPA annotations** — no `@Entity`, `@Column`, etc.

### ddd-infrastructure — Implementation Only

**Contains**: JPA entities, Spring Data JPA repositories, repository/port implementations, MapStruct infra-layer mappers, security configs, JWT, audit config.

**Rules**:
- All JPA entities that need audit columns extend `BaseEntityJpa` (`com.ddd.infrastructure.entity.BaseEntityJpa`) + `@EntityListeners(AuditingEntityListener.class)`
- If the entity does NOT extend `BaseEntityJpa` (stand-alone entity), still add `@EntityListeners(AuditingEntityListener.class)` and declare `@CreatedDate`, `@LastModifiedDate`, etc. manually
- Infra-layer mappers (domain model ↔ JPA entity) are named `*Mapper` and live under `com.ddd.infrastructure.mapper`
- Repository implementations implement the domain `Repository` interface from `ddd-domain`

### ddd-application — Application Logic

**Contains**: Application service interfaces and implementations, DTOs (`*InfoDto`, `*Dto`), app-layer MapStruct mappers, utilities.

**Rules**:
- Tier 1 services MUST be split into `*CommandService` + `*QueryService` (see §5)
- Command service implementations are annotated `@Transactional` (write transactions)
- Query service implementations are annotated `@Transactional(readOnly = true)`
- App-layer mappers are named `*DtoMapper` and live under `com.ddd.application.mapper`
- Application services inject `SecurityContextPort` to get the current user — they do NOT accept `String email` / `Long userId` as method parameters from the controller

### ddd-api — HTTP Interface Only

**Contains**: `@RestController` classes, API request/response DTO records (`*RequestDto`, `*ResponseDto`), API MapStruct mappers (`*ApiMapper`), `BaseResponse<T>`, `GlobalExceptionHandler`.

**Rules**:
- All controller methods MUST return `BaseResponse<T>` (see §4)
- Controllers inject application services and delegate immediately — no business logic
- Controllers MUST NOT inject `SecurityContextPort` or domain ports directly
- Controllers MUST NOT extract `Authentication` from Spring Security and pass user identity to services as a method argument — the application service uses `SecurityContextPort` directly
- API mappers are named `*ApiMapper` and live under `com.ddd.api.mapper`

---

## 3. Golden Path: End-to-End Request Flow

```
HTTP Request
    │
    ▼
JwtTokenValidatorFilter (ddd-infrastructure)
    │  Validates JWT, populates SecurityContextHolder with UserDetailsCustom
    ▼
@RestController (ddd-api)
    │  Receives HTTP params/body as *RequestDto records
    │  Returns BaseResponse<*ResponseDto>
    │  Delegates to Application Service — NO business logic here
    ▼
*CommandService / *QueryService (ddd-application)
    │  Injects SecurityContextPort → getCurrentUserId() / getCurrentUserEmail()
    │  Business logic, validation, exception throwing
    │  Uses domain Repository interfaces OR *JpaRepository directly
    ▼
Domain Repository interface (ddd-domain) ← implemented by →
*RepositoryImpl (ddd-infrastructure)
    │  Wraps *JpaRepository
    │  Uses *Mapper to convert JPA entity ↔ Domain model
    ▼
*JpaRepository (ddd-infrastructure)
    │  Spring Data JPA, JPQL, Specifications
    ▼
Database (MySQL)
    ▲
    │  On return: JPA Entity → *Mapper → Domain Model → *DtoMapper → *InfoDto → *ApiMapper → *ResponseDto → BaseResponse<T>
```

---

## 4. Shared Components Catalog — Use These, Don't Reinvent

### 4.1 SecurityContextPort ⚠️ CRITICAL — Always Use This for Identity

**File**: [`ddd-domain/src/main/java/com/ddd/domain/model/port/security/SecurityContextPort.java`](ddd-domain/src/main/java/com/ddd/domain/model/port/security/SecurityContextPort.java)  
**Impl**: [`ddd-infrastructure/src/main/java/com/ddd/infrastructure/adapter/security/SpringSecurityContextAdaptor.java`](ddd-infrastructure/src/main/java/com/ddd/infrastructure/adapter/security/SpringSecurityContextAdaptor.java)

```java
public interface SecurityContextPort {
    Long getCurrentUserId();       // Returns the authenticated user's DB id
    String getCurrentUserEmail();  // Returns the authenticated user's email
    boolean hasRole(String role);  // e.g., hasRole("ADMIN") — adds "ROLE_" prefix automatically
    boolean isAuthenticated();     // true if getCurrentUserId() != null
}
```

**Usage** (inject in `ddd-application` services):
```java
@Service
@RequiredArgsConstructor
public class MyCommandServiceImpl implements MyCommandService {
    private final SecurityContextPort securityContextPort; // ← inject this

    public void doSomething() {
        Long userId = securityContextPort.getCurrentUserId();
        // use userId directly — do NOT receive it from the controller
    }
}
```

**❌ NEVER do this** (see §7 Anti-Patterns):
```java
// In Controller — WRONG
public BaseResponse<Void> doSomething(Authentication authentication) {
    fanpageService.doSomething(authentication.getName()); // ← passing email is wrong
}

// In Service — WRONG
public void doSomething(String userEmail) { // ← receiving email/userId from controller is wrong
```

---

### 4.2 Domain Exceptions

All exceptions live in `com.ddd.domain.exception`. Use these — do NOT throw raw `RuntimeException` or `IllegalArgumentException` in service layer.

| Exception | HTTP Status | When to Use |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity not found by ID or key |
| `AuthException` | 401 | Action not allowed for current user (identity/permission mismatch) |
| `BusinessException` | 400 | Business rule violation (domain constraint) |
| `DuplicateResourceException` | 409 | Resource already exists (unique constraint) |
| `UnauthorizedException` | 401 | Not authenticated |

```java
// ✅ Correct usage
throw new ResourceNotFoundException("Invitation not found with id: " + id);
throw new AuthException("You are not the recipient of this invitation");
throw new BusinessException("Invitation is no longer pending");
throw new DuplicateResourceException("User is already a member of this fanpage");
```

All exceptions are automatically handled by `GlobalExceptionHandler` in `ddd-api` — no try/catch needed in controllers or services.

---

### 4.3 BaseResponse<T>

**File**: `ddd-api/src/main/java/com/ddd/api/common/BaseResponse.java`

Every controller method MUST return `BaseResponse<T>`. Never return a raw object or `ResponseEntity` directly.

```java
// ✅ Success with data
return BaseResponse.of(someDto);

// ✅ Success, no data (void operations)
return BaseResponse.ok();

// ✅ Paginated data
return BaseResponse.of(page.map(mapper::toResponseDto));
```

---

### 4.4 ServiceUtil — Pagination Helper

**File**: [`ddd-application/src/main/java/com/ddd/application/utils/ServiceUtil.java`](ddd-application/src/main/java/com/ddd/application/utils/ServiceUtil.java)

```java
// ✅ Always use this for Pageable creation in query services
Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
```

- Defaults `sortBy` to `"createdAt"` if null/blank
- Defaults direction to DESC
- Guards against negative page/size values

**❌ Do NOT** create a private `createPageable()` helper in your service — this utility already exists.

---

### 4.5 BaseModel (Domain Models)

**File**: [`ddd-domain/src/main/java/com/ddd/domain/model/BaseModel.java`](ddd-domain/src/main/java/com/ddd/domain/model/BaseModel.java)

All domain model classes extend `BaseModel`:

```java
public abstract class BaseModel {
    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private boolean isActive = true;
}
```

---

### 4.6 BaseEntityJpa (JPA Entities with Auditing)

**File**: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/BaseEntityJpa.java`

JPA entities that map to `BaseModel` extend `BaseEntityJpa`. It has:
- `@EntityListeners(AuditingEntityListener.class)` already on the class
- `@CreatedDate` on `createdAt` (`updatable = false`)
- `@CreatedBy` on `createdBy` (`updatable = false`)
- `@LastModifiedDate` on `updatedAt`
- `@LastModifiedBy` on `updatedBy`

```java
@Entity
@Table(name = "my_table")
public class MyJpaEntity extends BaseEntityJpa {
    // Don't re-declare createdAt, updatedAt, createdBy, updatedBy, isActive
    // They are inherited from BaseEntityJpa
}
```

For stand-alone entities (like `FanpageMemberInvitationJpaEntity`) that don't extend `BaseEntityJpa`, add `@EntityListeners(AuditingEntityListener.class)` manually and declare audit fields with Spring Data annotations.

---

### 4.7 AuditorAwareImpl

**File**: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/config/audit/AuditorAwareImpl.java`  
Bean name: `"auditorAwareImpl"`

Automatically populates `createdBy` / `updatedBy` with the current user's DB ID via `SecurityContextPort`.  
Configured by `@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")` in `StartApplication`.

---

## 5. Service Tier Catalog

### Tier 1 — CQRS Mandatory (Business Domains)

These domains have complex transactional logic and read/write split. They **MUST** use `*CommandService` + `*QueryService`.

| Domain | CommandService | QueryService | Package |
|---|---|---|---|
| `Event` | `EventCommandService` | `EventQueryService` | `com.ddd.application.service.event` |
| `Fanpage` | `FanpageCommandService` | `FanpageQueryService` | `com.ddd.application.service.fanpage` |
| `Registration` | `RegistrationCommandService` | `RegistrationQueryService` | `com.ddd.application.service.registration` |
| `User` | `UserCommandService` | `UserQueryService` | `com.ddd.application.service.user` |
| `StudentCriteriaProgress` | `StudentCriteriaProgressCommandService` | `StudentCriteriaProgressQueryService` | `com.ddd.application.service.criteriaprogress` |
| `PointTransaction` | `PointTransactionCommandService` | `PointTransactionQueryService` | `com.ddd.application.service.pointtransaction` |
| `StoredEvent` | `StoredEventCommandService` | `StoredEventQueryService` | `com.ddd.application.service.storedevent` |
| `FanpageMemberInvitation` | `FanpageMemberInvitationCommandService` | `FanpageMemberInvitationQueryService` | `com.ddd.application.service.fanpage` |

**Rules for Tier 1**:
- `*CommandService` impl → `@Transactional` (class-level)
- `*QueryService` impl → `@Transactional(readOnly = true)` (class-level)
- Never combine reads and writes in the same service class
- Interface must be defined in `ddd-application`; impl class goes in `.impl` sub-package

### Tier 2 — Single Service (Master Data)

These domains manage relatively static master/reference data. A single `*Service` is acceptable.

| Domain | Service | Package |
|---|---|---|
| `School` | `SchoolService` | `com.ddd.application.service` |
| `Major` | `MajorService` | `com.ddd.application.service` |
| `Role` | `RoleService` | `com.ddd.application.service` |
| `EventType` | `EventTypeService` | `com.ddd.application.service` |
| `Association` | `AssociationService` | `com.ddd.application.service` |
| `Semester` | `SemesterService` | `com.ddd.application.service` |
| `PointCategory` | `PointCategoryService` | `com.ddd.application.service` |
| `Criteria` | `CriteriaService` | `com.ddd.application.service` |
| `AcademicYear` | `AcademicYearService` | `com.ddd.application.service` |

**When a new feature is a Tier 1 or Tier 2?**  
If the domain involves user-triggered events, state machines, financial/point transactions, or moderation flows → **Tier 1**.  
If it's simple CRUD on reference data with no complex business rules → **Tier 2**.

---

## 6. Naming & Package Conventions

### 6.1 Package Structure per Module

```
com.ddd.domain
├── model/              # Domain models (extend BaseModel)
│   └── port/           # Port interfaces (SecurityContextPort, etc.)
│       └── security/
├── repository/         # Repository interfaces (domain contracts)
├── enums/              # Domain enums
└── exception/          # Domain exceptions

com.ddd.infrastructure
├── entity/             # JPA entities (extend BaseEntityJpa or standalone)
├── repository/
│   ├── impl/           # Repository implementation classes
│   └── jpaRepository/  # Spring Data JPA interfaces (extend JpaRepository)
│   └── specification/  # JPA Specifications for dynamic queries
├── mapper/             # Infra-layer MapStruct mappers (*Mapper)
├── adapter/
│   └── security/       # Port implementations (SpringSecurityContextAdaptor)
└── config/             # Spring configs (security, JPA, audit, etc.)
    └── audit/          # AuditorAwareImpl

com.ddd.application
├── service/
│   └── <domain>/       # Service interfaces
│       └── impl/       # Service implementations
├── dto/
│   └── <domain>/       # Application-layer DTOs (*InfoDto, *Dto)
├── mapper/             # App-layer MapStruct mappers (*DtoMapper)
└── utils/              # ServiceUtil, etc.

com.ddd.api
├── controller/         # REST controllers
├── dto/
│   └── <domain>/
│       ├── req/        # Request DTOs (*RequestDto — Java records)
│       └── res/        # Response DTOs (*ResponseDto — Java records)
├── mapper/             # API-layer MapStruct mappers (*ApiMapper)
├── common/             # BaseResponse, ErrorCodeEnum
└── exception/          # GlobalExceptionHandler
```

### 6.2 File Naming Rules

| Artifact | Naming Pattern | Location | Example |
|---|---|---|---|
| Domain model | `<Entity>` | `ddd-domain/.../model/` | `Fanpage`, `User` |
| Domain repository interface | `<Entity>Repository` | `ddd-domain/.../repository/` | `FanpageRepository` |
| JPA entity | `<Entity>JpaEntity` | `ddd-infrastructure/.../entity/` | `FanpageJpaEntity` |
| JPA Spring Data repo | `<Entity>JpaRepository` | `ddd-infrastructure/.../jpaRepository/` | `FanpageJpaRepository` |
| Repository impl | `<Entity>RepositoryImpl` | `ddd-infrastructure/.../repository/impl/` | `FanpageRepositoryImpl` |
| Infra mapper | `<Entity>Mapper` | `ddd-infrastructure/.../mapper/` | `FanpageMapper` |
| App service interface (Cmd) | `<Entity>CommandService` | `ddd-application/.../service/<domain>/` | `FanpageCommandService` |
| App service interface (Qry) | `<Entity>QueryService` | `ddd-application/.../service/<domain>/` | `FanpageQueryService` |
| App service impl (Cmd) | `<Entity>CommandServiceImpl` | `ddd-application/.../service/<domain>/impl/` | `FanpageCommandServiceImpl` |
| App service impl (Qry) | `<Entity>QueryServiceImpl` | `ddd-application/.../service/<domain>/impl/` | `FanpageQueryServiceImpl` |
| App-layer DTO (read) | `<Entity>InfoDto` | `ddd-application/.../dto/<domain>/` | `FanpageInfoDto` |
| App-layer DTO (write) | `<Entity>Dto` or `<Entity><Action>Dto` | `ddd-application/.../dto/<domain>/` | `FanpageRegisterDto` |
| App-layer mapper | `<Entity>DtoMapper` | `ddd-application/.../mapper/` | `FanpageDtoMapper` |
| API request DTO | `<Entity><Action>RequestDto` | `ddd-api/.../dto/<domain>/req/` | `FanpageCreateRequestDto` |
| API response DTO | `<Entity>ResponseDto` | `ddd-api/.../dto/<domain>/res/` | `FanpageResponseDto` |
| API mapper | `<Entity>ApiMapper` | `ddd-api/.../mapper/` | `FanpageApiMapper` |

### 6.3 DTO Types

- **`*InfoDto`** (application layer): Rich read model, used as the return type of `*QueryService` methods. Java record. Contains all display-relevant fields.
- **`*RequestDto`** (API layer): Input from HTTP client. Java record with `@NotNull`/`@Size` bean validation. Never reuse as an app-layer DTO.
- **`*ResponseDto`** (API layer): Returned to HTTP client, wrapped in `BaseResponse<T>`. Java record. Mapped from `*InfoDto` by `*ApiMapper`.

---

## 7. Anti-Patterns & Legacy Traps

> These patterns exist in unrefactored legacy code. **Do NOT copy them.** When you encounter them, treat them as bugs.

### 7.1 ❌ Passing Identity from Controller to Service

The most common mistake. Controllers should NEVER extract user identity and pass it as a parameter.

```java
// ❌ LEGACY / WRONG — Controller extracts email and passes it
@PostMapping("/{id}/accept")
public BaseResponse<Void> acceptInvitation(@PathVariable Long id, Authentication authentication) {
    String email = authentication.getName(); // ← WRONG: extracting here
    invitationCommandService.acceptInvitation(id, email); // ← WRONG: passing as param
    return BaseResponse.ok();
}

// ❌ LEGACY / WRONG — Service accepts email/userId as parameter
public void acceptInvitation(Long invitationId, String userEmail) { // ← WRONG
    if (!invitation.getUser().getEmail().equalsIgnoreCase(userEmail)) { ... }
}
```

```java
// ✅ CORRECT — Controller delegates immediately, no identity extraction
@PostMapping("/{id}/accept")
public BaseResponse<Void> acceptInvitation(@PathVariable Long id) {
    invitationCommandService.acceptInvitation(id);
    return BaseResponse.ok();
}

// ✅ CORRECT — Service injects SecurityContextPort and reads identity itself
@Service @RequiredArgsConstructor
public class InvitationCommandServiceImpl implements InvitationCommandService {
    private final SecurityContextPort securityContextPort;

    public void acceptInvitation(Long invitationId) {
        Long currentUserId = securityContextPort.getCurrentUserId(); // ← CORRECT
        // ...
    }
}
```

**Existing violation**: `FanpageMemberInvitationCommandServiceImpl` and `FanpageMemberInvitationController` — not yet refactored.

---

### 7.2 ❌ Injecting Infrastructure in Application Service

Application services should use **domain repository interfaces** from `ddd-domain`, not `*JpaRepository` directly.

```java
// ❌ LEGACY / WRONG — Application service imports JPA repository or JPA entity
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberInvitationJpaRepository;
import com.ddd.infrastructure.entity.FanpageMemberInvitationJpaEntity;

// ✅ CORRECT — Use domain repository interface
import com.ddd.domain.repository.FanpageMemberInvitationRepository;
```

**Exception**: If a domain `Repository` interface has not been created yet for a new feature, using `*JpaRepository` directly in the application service is acceptable **temporarily**, but create the domain `Repository` interface and implementation as part of the same PR.

---

### 7.3 ❌ Manual Timestamp Management

```java
// ❌ LEGACY — Manual @PrePersist for timestamps
@PrePersist
protected void onCreate() {
    this.createdAt = Instant.now(); // ← WRONG
}

// ❌ LEGACY — Hibernate @CreationTimestamp
@CreationTimestamp
private Instant createdAt; // ← WRONG (Hibernate-specific, bypasses Spring auditing)
```

```java
// ✅ CORRECT — Spring Data JPA Auditing
@CreatedDate
@Column(name = "created_at", updatable = false)
private Instant createdAt;

@LastModifiedDate
@Column(name = "updated_at")
private Instant updatedAt;
```

Also ensure `@EntityListeners(AuditingEntityListener.class)` is on the class (or inherited from `BaseEntityJpa`).

---

### 7.4 ❌ Creating a Private createPageable() Helper

```java
// ❌ LEGACY — Private helper duplicated in every query service
private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
    Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    return PageRequest.of(page, size, sort);
}
```

```java
// ✅ CORRECT — Use the shared utility
import com.ddd.application.utils.ServiceUtil;
Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
```

**Existing violation**: `EventQueryServiceImpl` has its own private `createPageable()` — this is a known legacy issue.

---

### 7.5 ❌ Missing BaseResponse Wrapper

```java
// ❌ WRONG — Raw object return
@GetMapping("/something")
public MyDto getSomething() { ... }

// ❌ WRONG — ResponseEntity without BaseResponse
@GetMapping("/something")
public ResponseEntity<MyDto> getSomething() { ... }
```

```java
// ✅ CORRECT
@GetMapping("/something")
public BaseResponse<MyDto> getSomething() {
    return BaseResponse.of(myService.getSomething());
}
```

---

### 7.6 ❌ Using IllegalArgumentException Instead of Domain Exceptions

```java
// ❌ WRONG — IllegalArgumentException from service layer
throw new IllegalArgumentException("User not found"); // ← loses HTTP status context
```

```java
// ✅ CORRECT — Use domain exceptions
throw new ResourceNotFoundException("User not found with email: " + email);
```

---

### 7.7 ❌ Mapper Update Methods Without Ignoring Audit Fields

When a MapStruct mapper has an `updateEntityFromDomain(@MappingTarget entity, ...)` method, always ignore audit fields to prevent overwriting Spring-managed values:

```java
// ✅ CORRECT in Infra mapper
@Mapping(target = "createdAt", ignore = true)
@Mapping(target = "updatedAt", ignore = true)
@Mapping(target = "createdBy", ignore = true)
@Mapping(target = "updatedBy", ignore = true)
void updateEntityFromDomain(MyDomainModel domain, @MappingTarget MyJpaEntity entity);
```

---

## 8. MapStruct Mapper Conventions

Three mapper layers, each with a distinct purpose and naming:

| Layer | Type Name Suffix | Package | Maps Between |
|---|---|---|---|
| `ddd-infrastructure` | `*Mapper` | `com.ddd.infrastructure.mapper` | JPA Entity ↔ Domain Model |
| `ddd-application` | `*DtoMapper` | `com.ddd.application.mapper` | JPA Entity or Domain Model → `*InfoDto` |
| `ddd-api` | `*ApiMapper` | `com.ddd.api.mapper` | `*InfoDto` → `*ResponseDto` |

**Mapper annotations**:
```java
@Mapper(componentModel = "spring")
public interface MyDtoMapper { ... }
```

**Naming methods**:
- Infra: `toDomainModel(entity)`, `toJpaEntity(domain)`, `updateEntityFromDomain(domain, @MappingTarget entity)`
- App: `toInfoDto(entity)` or `toInfoDto(domain)`
- API: `toResponseDto(infoDto)`

---

## 9. JPA Entity Conventions

### Standard Entity (extends BaseEntityJpa)

```java
@Getter
@Setter
@Entity
@Table(name = "my_table")
// ✅ No @EntityListeners needed — inherited from BaseEntityJpa
public class MyJpaEntity extends BaseEntityJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // ... your fields
    // DO NOT declare: createdAt, updatedAt, createdBy, updatedBy, isActive
}
```

### Stand-alone Entity (does NOT extend BaseEntityJpa)

```java
@Getter
@Setter
@Entity
@Table(name = "my_table")
@EntityListeners(AuditingEntityListener.class) // ✅ Required for auditing
public class MyStandaloneJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;
}
```

### FetchType Rules

- Use `FetchType.LAZY` for all `@ManyToOne` / `@OneToMany` associations by default
- Use `@EntityGraph` or JPQL `JOIN FETCH` in the JPA repository method when you need eager loading for a specific query

---

## Quick Reference — Before You Write a Line of Code

| Task | Use This |
|---|---|
| Get current user ID | `securityContextPort.getCurrentUserId()` |
| Get current user email | `securityContextPort.getCurrentUserEmail()` |
| Check if user has role | `securityContextPort.hasRole("ADMIN")` |
| Create pageable | `ServiceUtil.createPageable(page, size, sortBy, sortDir)` |
| Resource not found | `throw new ResourceNotFoundException("...")` |
| Business rule violated | `throw new BusinessException("...")` |
| Permission denied | `throw new AuthException("...")` |
| Duplicate resource | `throw new DuplicateResourceException("...")` |
| Wrap controller response | `BaseResponse.of(data)` / `BaseResponse.ok()` |
| New business domain service | Create `*CommandService` + `*QueryService` interfaces + impls |
| New master data service | Single `*Service` interface + impl is OK |
