# Specification: Enterprise Global Exception Handling Architecture (Approach 3)

- **Date**: 2026-11-14
- **Status**: Proposed / Design Approved
- **Author**: Antigravity & Engineering Team
- **Target Modules**: `ddd-domain`, `ddd-infrastructure`, `ddd-application`, `ddd-api`

---

## 1. Context & Motivation

The current exception handling implementation in the project provides centralized capture via `@RestControllerAdvice` and a unified `BaseResponse<T>`. However, an architectural audit identified five critical gaps against enterprise DDD and industry standards:
1. **DDD Boundary Violations**: Domain exception classes (`BusinessException`, `AuthException`, `ResourceNotFoundException`, `InvalidFileException`, `FileStorageException`) import Spring Web annotations (`@ResponseStatus`, `HttpStatus`), leaking presentation-tier concerns into the delivery-agnostic domain core.
2. **Unstructured Validation Responses**: `MethodArgumentNotValidException` and `HandlerMethodValidationException` serialize errors via `errors.toString()`, preventing frontend and mobile consumers from mapping errors back to specific UI input fields.
3. **Filter Divergence**: Security filter entry points (`JwtAccessDeniedHandler`) emit hardcoded JSON strings instead of delegating to Spring's centralized exception resolution pipeline.
4. **Lack of Request Observability**: No correlation/trace ID is propagated in error responses, making log correlation difficult during production incidents.
5. **Flat Exception Hierarchy**: Domain exceptions directly extend `RuntimeException` without a shared abstract base.

This document specifies **Approach 3 (Hybrid: Standard `BaseResponse<T>` with Structured Errors, MDC Request Tracing, and Pure Java Domain Exceptions)**.

---

## 2. Architectural Design & Layer Responsibilities

```
[ddd-domain] ◄────── [ddd-application] ◄────── [ddd-api] (REST / HTTP)
       ▲                                            ▲
       └────────────── [ddd-infrastructure] ────────┘
```

### 2.1 `ddd-domain` (Pure Domain Core)
- **Zero Spring/Web dependencies**: Strictly pure Java.
- **Root Abstract Exception**: `DomainException extends RuntimeException` holding an optional domain `errorCode`.
- **Domain Exceptions**:
  - `BusinessException`: Domain constraint violations & invariant failures.
  - `ResourceNotFoundException`: Aggregate or entity missing by ID or key.
  - `DuplicateResourceException`: Conflict on unique domain invariants.
  - `AuthException` / `UnauthorizedException`: Domain-level ownership or permission mismatch.
  - `EventAttendanceException` / `EventRegistrationException`: Domain workflow failures.
  - `InvalidFileException`: File validation failures at the domain port level.

### 2.2 `ddd-infrastructure` (Technical Adapters & Filters)
- **TraceIdFilter**:
  - Order: `Ordered.HIGHEST_PRECEDENCE`.
  - Reads `X-Request-ID` HTTP header or generates `UUID.randomUUID().toString()`.
  - Sets MDC key `"traceId"` and writes `X-Request-ID` response header.
  - Clears MDC in a `finally` block.
- **Security Exception Delegation**:
  - `JwtAccessDeniedHandler` and `JwtAuthenticationEntryPoint` inject `HandlerExceptionResolver`.
  - Invokes `resolver.resolveException(request, response, null, ex)` so filter exceptions route directly to `GlobalExceptionHandler`.

### 2.3 `ddd-api` (Presentation & HTTP Mapping)
- **Envelope Extension (`BaseResponse<T>`)**:
  - Adds `traceId` (from MDC), `timestamp` (`Instant.now()`), and `errors` (`List<ValidationErrorDetail>`).
  - Fields are marked `@JsonInclude(JsonInclude.Include.NON_NULL)` for backward compatibility.
- **`ValidationErrorDetail`**:
  - Java record: `(String field, String message, Object rejectedValue)`.
- **`GlobalExceptionHandler`**:
  - Maps `MethodArgumentNotValidException` to `List<ValidationErrorDetail>`.
  - Maps `DomainException` subclasses to appropriate HTTP status codes (400, 401, 403, 404, 409).
  - Sanitizes `Exception.class` to 500 while logging full stack trace with trace context.

---

## 3. Data Flow Specification

1. **Incoming Request**:
   - `TraceIdFilter` establishes the MDC `traceId`.
2. **Security Checks**:
   - If token expired or forbidden, filter delegates via `HandlerExceptionResolver` to `GlobalExceptionHandler` returning `BaseResponse` with 401/403.
3. **Execution**:
   - Controller delegates to `CommandService` / `QueryService`.
   - If a business rule fails, domain model or service throws a `DomainException` (e.g. `throw new BusinessException(...)`).
4. **Resolution**:
   - `GlobalExceptionHandler` intercepts the exception.
   - Converts to `BaseResponse.error(code, message)` with current `traceId` and `timestamp`.
   - Returns appropriate HTTP status entity.

---

## 4. Testing Strategy

1. **Unit Tests**:
   - Domain exception instantiation without Spring dependencies.
   - `GlobalExceptionHandler` unit tests verifying status code mapping and `BaseResponse` fields.
   - Bean validation tests asserting that `errors` contains field-specific `ValidationErrorDetail` items.
2. **Integration / MockMvc Tests**:
   - End-to-end endpoint invocations validating:
     - 400 Bad Request with field validation errors.
     - 404 Not Found for missing entities.
     - 401 Unauthorized for expired/missing JWT tokens.
     - 403 Forbidden for unauthorized roles.
     - `X-Request-ID` header presence and matching `traceId` in JSON payload.
