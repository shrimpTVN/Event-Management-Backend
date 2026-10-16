# Exception Handling Guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Author a comprehensive, production-grade architectural guidance document on Exception Handling for the Event Management Backend adhering to Domain-Driven Design (DDD) principles and Spring Boot best practices.

**Architecture:** Define a clean-boundary exception strategy across DDD layers (`ddd-domain`, `ddd-application`, `ddd-infrastructure`, `ddd-api`). Ensure the domain layer contains zero web/framework dependencies (removing leaky `@ResponseStatus` / `HttpStatus` annotations), standardizing error definitions via `ErrorCodeEnum`, centralizing HTTP translation in `GlobalExceptionHandler`, and ensuring Spring Security filter chain errors conform to `BaseResponse<T>`.

**Tech Stack:** Java 21, Spring Boot 4.x / Spring 6.x, Spring Security, Domain-Driven Design (DDD), Slf4j / Lombok, Markdown.

**Spec:** `docs/guidelines/exception-handling-guidance.md`

## Global Constraints

- **DDD Layer Independence:** Domain layer (`ddd-domain`) must not depend on Presentation / Web annotations (`@ResponseStatus`, `HttpStatus`) or Infrastructure.
- **Uniform API Envelope:** All HTTP error responses must conform to `BaseResponse<T>` (`exceptionCode`, `message`, `data`).
- **Unchecked Exceptions:** All domain/application exceptions must extend `RuntimeException` to integrate cleanly with Spring's declarative transaction rollback mechanism (`@Transactional`).
- **Security & Hygiene:** Internal stack traces and raw SQL/infrastructure errors must never leak to API clients; 5xx errors return sanitized messages with full server-side logging (`log.error`), while 4xx errors log at `log.warn`.

## Review Focus

- Domain layer purity: Verification that guidance clearly deprecates/forbids `@ResponseStatus` in `ddd-domain` and provides migration steps for existing classes.
- Security filter chain gaps: Guidance on handling authentication entry points (`AuthenticationEntryPoint`) and access denied handlers (`AccessDeniedHandler`) where exceptions bypass `DispatcherServlet` / `@RestControllerAdvice`.
- Completeness of HTTP status & ErrorCode mapping: Alignment between `ErrorCodeEnum`, HTTP statuses (400, 401, 403, 404, 409, 422, 500), and business scenarios (Google OAuth2, Fanpage management, Event check-in, etc.).
- Validation handling: Consistent parsing and payload formatting for both `@RequestBody` bean validation (`MethodArgumentNotValidException`) and `@PathVariable`/`@RequestParam` method validation (`HandlerMethodValidationException`).
- Developer-friendly recipes: Ready-to-copy code snippets for creating new domain exceptions, throwing exceptions from application services, and registering error codes.

---

### Task 1: Document DDD Exception Architecture & Layer Responsibilities

**Files:**
- Create: `docs/guidelines/exception-handling-guidance.md`

**Interfaces:**
- Consumes: Current multi-module project structure (`ddd-domain`, `ddd-application`, `ddd-infrastructure`, `ddd-api`).
- Produces: Architectural boundary specifications and layer-by-layer exception handling rules.

- [ ] **Step 1: Write Architectural Overview & Layer Responsibilities Section**
  - Document the responsibility of each module:
    - `ddd-domain`: Pure business exceptions representing domain invariant violations (`ResourceNotFoundException`, `DuplicateResourceException`, `BusinessException`). No Spring Web dependencies.
    - `ddd-application`: Use case orchestration and command validation.
    - `ddd-infrastructure`: Technical exception mapping (translating JPA / DB / network errors into domain or technical exceptions).
    - `ddd-api`: Global error handling (`GlobalExceptionHandler`), mapping exceptions to HTTP status codes and `BaseResponse`.
  - Include an architectural ASCII/Mermaid diagram showing exception flow from domain/infra through application up to presentation.

- [ ] **Step 2: Document Domain Purity Rule & Existing Codebase Remediation**
  - Highlight current anti-pattern found in `ddd-domain/src/main/java/com/ddd/domain/exception/` (`@ResponseStatus` on `AuthException`, `BusinessException`, `ResourceNotFoundException`, `DuplicateResourceException`).
  - Provide explicit instructions on removing `@ResponseStatus` from domain classes so `ddd-domain` remains pure Java.

- [ ] **Step 3: Verification & Review**
  - Verify compliance with DDD rules specified in `.agents/rules/agent-rules.md`.

---

### Task 2: Standardize Response Envelope & Error Codes

**Files:**
- Modify: `docs/guidelines/exception-handling-guidance.md`

**Interfaces:**
- Consumes: `BaseResponse<T>`, `ErrorCode`, `ErrorCodeEnum`.
- Produces: Error code naming conventions, error payload contract, and registration guide.

- [ ] **Step 1: Document `BaseResponse<T>` Error Structure**
  - Specify the JSON format returned to API clients:
    ```json
    {
      "data": null,
      "message": "Entity not found",
      "exceptionCode": "RESOURCE_NOT_FOUND"
    }
    ```
  - Document validation error payload where `data` holds field-specific error maps.

- [ ] **Step 2: Document `ErrorCode` & `ErrorCodeEnum` Standard**
  - Document the code convention (`<DOMAIN_MODULE>.<NUMBER>`, e.g., `AUTH.001`, `EVENT.001`, `STUDENT.001`).
  - Document using `withFormattedMessage(Object... args)` for parameterized error messages.
  - Provide a step-by-step example of adding a new error code to `ErrorCodeEnum`.

- [ ] **Step 3: Verification & Review**
  - Verify that the error response format matches `BaseResponse` in `ddd-api/src/main/java/com/ddd/api/common/BaseResponse.java`.

---

### Task 3: Author GlobalExceptionHandler Reference & Spring Security Handling

**Files:**
- Modify: `docs/guidelines/exception-handling-guidance.md`

**Interfaces:**
- Consumes: `GlobalExceptionHandler`, Spring Security filters (`AuthenticationEntryPoint`, `AccessDeniedHandler`).
- Produces: Implementation standards for `@RestControllerAdvice` and Security error interception.

- [ ] **Step 1: Document GlobalExceptionHandler Standard Implementation**
  - Detail `@ExceptionHandler` methods for:
    - 400: `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `IllegalArgumentException`.
    - 401: `UnauthorizedException`, `AuthException`, `BadCredentialsException`.
    - 403: `AccessDeniedException`.
    - 404: `ResourceNotFoundException`, `NoResourceFoundException`.
    - 409: `DuplicateResourceException`.
    - 422: `BusinessException` / domain rule violations.
    - 500: Catch-all `Exception.class` with logging standards (`log.error` with stack trace for 500, `log.warn` for 4xx).
  - Clarify the missing `BusinessException` handler in `GlobalExceptionHandler.java`.

- [ ] **Step 2: Document Spring Security Filter Exception Interception**
  - Explain why `@RestControllerAdvice` cannot catch filter-level authentication failures (e.g., missing/invalid JWT tokens before reaching `DispatcherServlet`).
  - Provide reference implementation for `CustomAuthenticationEntryPoint` and `CustomAccessDeniedHandler` writing `BaseResponse` directly using `ObjectMapper`.

- [ ] **Step 3: Verification & Review**
  - Verify all HTTP status codes align with standard REST practices.

---

### Task 4: Developer Cheat Sheet, Best Practices, and Anti-Patterns

**Files:**
- Modify: `docs/guidelines/exception-handling-guidance.md`

**Interfaces:**
- Consumes: Real codebase examples from `AuthServiceImpl`, `FanpageServiceImpl`, `GlobalExceptionHandler`.
- Produces: Quick-reference guide with copy-pasteable snippets and DOs / DON'Ts.

- [ ] **Step 1: Write Developer Recipes / Cheat Sheet**
  - Recipe 1: "How to handle 'Not Found' in Repository/Service query (`.orElseThrow(...)`)."
  - Recipe 2: "How to validate business rules and reject invalid states in Domain/Application services."
  - Recipe 3: "How to handle duplicate resources during registration/creation."
  - Recipe 4: "How to create a new custom business exception."

- [ ] **Step 2: Write Anti-Patterns and Best Practices Checklist**
  - Do not swallow exceptions (e.g. empty catch blocks).
  - Do not log and rethrow (leads to duplicate log entries).
  - Do not expose database exceptions / SQL errors to clients.
  - Do not use checked exceptions for domain business logic.
  - Never import `org.springframework.http.*` inside `ddd-domain`.

- [ ] **Step 3: Verification & Review**
  - Review complete guidance against all rules in `agent-rules.md`.
