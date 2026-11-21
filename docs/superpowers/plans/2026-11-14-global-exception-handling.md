# Enterprise Global Exception Handling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish an enterprise-grade, DDD-compliant global exception handling architecture (Approach 3) that eliminates Spring/HTTP dependencies from `ddd-domain`, adds MDC request tracing (`traceId`), produces structured field-level validation errors, and unifies security filter error responses under a backward-compatible `BaseResponse<T>`.

**Architecture:** Refactor `ddd-domain` to use a pure Java `DomainException` root with zero framework annotations. Introduce `TraceIdFilter` in `ddd-infrastructure` to populate MDC context and the `X-Request-ID` header. Extend `BaseResponse<T>` in `ddd-api` with `traceId`, `timestamp`, and `List<ValidationErrorDetail>`, update `GlobalExceptionHandler` to map exceptions cleanly without raw map string dumps, and delegate security filter exceptions via `HandlerExceptionResolver`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security 6, Jakarta Validation, SLF4J / MDC, Jackson, JUnit 5, AssertJ, Mockito.

**Spec:** [`docs/superpowers/specs/2026-11-14-global-exception-handling-design.md`](file:///home/shrimp/code/ddd/docs/superpowers/specs/2026-11-14-global-exception-handling-design.md)

## Global Constraints

- `ddd-domain` must remain pure Java with ZERO `org.springframework.*` imports or annotations.
- Transport mapping (HTTP status codes) belongs solely in `ddd-api`.
- Existing `BaseResponse` fields (`data`, `message`, `exceptionCode`) must remain backward-compatible for existing frontend callers.
- All new error response fields (`traceId`, `timestamp`, `errors`) must use `@JsonInclude(JsonInclude.Include.NON_NULL)`.
- No raw exceptions (`RuntimeException`, `IllegalArgumentException`) thrown in application/domain business flows without a clear domain rationale.

## Review Focus

1. Domain exception classes retain backward-compatible constructor signatures `(String message)` and `(String message, Throwable cause)` so existing service callers do not break.
2. `TraceIdFilter` always clears `MDC` in a `finally` block to prevent thread-pool memory leaks across concurrent HTTP requests.
3. `BaseResponse` serialization with `null` fields (e.g. `errors` when no validation occurred) omits those keys from the JSON output.
4. `MethodArgumentNotValidException` with multiple field errors returns all errors in the `errors` array with accurate `field`, `message`, and `rejectedValue`.
5. Security filters (`JwtAccessDeniedHandler`, `JwtAuthenticationEntryPoint`) correctly forward exceptions to `HandlerExceptionResolver` without committing the HTTP response beforehand.

---

### Task 1: Domain Exception Hierarchy Refactoring in `ddd-domain`

**Files:**
- Create: `ddd-domain/src/main/java/com/ddd/domain/exception/DomainException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/BusinessException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/ResourceNotFoundException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/DuplicateResourceException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/AuthException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/UnauthorizedException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/EventAttendanceException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/EventRegistrationException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/InvalidFileException.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/exception/FileStorageException.java`
- Test: `ddd-domain/src/test/java/com/ddd/domain/exception/DomainExceptionTest.java`

**Interfaces:**
- Consumes: None (pure Java)
- Produces: `DomainException` abstract class extending `RuntimeException` with `getErrorCode()` method.

- [ ] **Step 1: Write failing unit test for `DomainException` and sub-exceptions**

Create `ddd-domain/src/test/java/com/ddd/domain/exception/DomainExceptionTest.java`:
```java
package com.ddd.domain.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void businessException_shouldInheritFromDomainExceptionAndRetainValues() {
        BusinessException ex = new BusinessException("001", "Domain rule violated");
        assertThat(ex).isInstanceOf(DomainException.class);
        assertThat(ex.getErrorCode()).isEqualTo("001");
        assertThat(ex.getMessage()).isEqualTo("Domain rule violated");
    }

    @Test
    void domainExceptions_shouldNotHaveSpringAnnotations() {
        Class<?>[] exceptionClasses = {
            DomainException.class,
            BusinessException.class,
            ResourceNotFoundException.class,
            DuplicateResourceException.class,
            AuthException.class,
            UnauthorizedException.class,
            EventAttendanceException.class,
            EventRegistrationException.class,
            InvalidFileException.class,
            FileStorageException.class
        };

        for (Class<?> clazz : exceptionClasses) {
            assertThat(clazz.getPackage().getName()).startsWith("com.ddd.domain");
            for (var annotation : clazz.getAnnotations()) {
                assertThat(annotation.annotationType().getName())
                        .doesNotStartWith("org.springframework");
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl ddd-domain -Dtest=DomainExceptionTest`
Expected: FAIL (cannot resolve `DomainException`, annotations still present).

- [ ] **Step 3: Implement `DomainException` and clean annotations from domain exceptions**

1. Create `DomainException.java`:
```java
package com.ddd.domain.exception;

import java.io.Serial;

public abstract class DomainException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    protected DomainException(String message) {
        super(message);
        this.errorCode = null;
    }

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```
2. Remove `@ResponseStatus` and `import org.springframework...` from:
   - `BusinessException` (extend `DomainException`, add `(String errorCode, String message)` constructor)
   - `ResourceNotFoundException` (extend `DomainException`)
   - `DuplicateResourceException` (extend `DomainException`)
   - `AuthException` (extend `DomainException`)
   - `UnauthorizedException` (extend `DomainException`)
   - `EventAttendanceException` (extend `DomainException`)
   - `EventRegistrationException` (extend `DomainException`)
   - `InvalidFileException` (extend `DomainException`)
   - `FileStorageException` (extend `RuntimeException`)

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl ddd-domain -Dtest=DomainExceptionTest`
Expected: PASS

- [ ] **Step 5: Run full domain test suite**

Run: `mvn clean test -pl ddd-domain`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add ddd-domain/
git commit -m "refactor(domain): remove spring annotations and establish DomainException hierarchy"
```

---

### Task 2: Request Tracing & MDC Filter in `ddd-infrastructure`

**Files:**
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/config/filter/TraceIdFilter.java`
- Test: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/config/filter/TraceIdFilterTest.java`

**Interfaces:**
- Consumes: Jakarta Servlet API (`Filter`, `HttpServletRequest`, `HttpServletResponse`), SLF4J `MDC`.
- Produces: `TraceIdFilter` populating `MDC.put("traceId", ...)` and header `X-Request-ID`.

- [ ] **Step 1: Write unit test for `TraceIdFilter`**

Create `ddd-infrastructure/src/test/java/com/ddd/infrastructure/config/filter/TraceIdFilterTest.java`:
```java
package com.ddd.infrastructure.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilter_whenHeaderProvided_shouldUseHeaderAndSetResponse() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("custom-trace-id-123");

        doAnswer(invocation -> {
            assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isEqualTo("custom-trace-id-123");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(TraceIdFilter.TRACE_ID_HEADER, "custom-trace-id-123");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void doFilter_whenHeaderMissing_shouldGenerateUuid() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(null);

        doAnswer(invocation -> {
            assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNotBlank();
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(eq(TraceIdFilter.TRACE_ID_HEADER), anyString());
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl ddd-infrastructure -Dtest=TraceIdFilterTest`
Expected: FAIL (class not found).

- [ ] **Step 3: Implement `TraceIdFilter`**

Create `ddd-infrastructure/src/main/java/com/ddd/infrastructure/config/filter/TraceIdFilter.java`:
```java
package com.ddd.infrastructure.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {
    public static final String TRACE_ID_HEADER = "X-Request-ID";
    public static final String MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, traceId);
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl ddd-infrastructure -Dtest=TraceIdFilterTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add ddd-infrastructure/
git commit -m "feat(infra): add TraceIdFilter with MDC request correlation"
```

---

### Task 3: Response Envelope (`BaseResponse`) and `ValidationErrorDetail` in `ddd-api`

**Files:**
- Create: `ddd-api/src/main/java/com/ddd/api/common/ValidationErrorDetail.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/common/BaseResponse.java`
- Test: `ddd-api/src/test/java/com/ddd/api/common/BaseResponseTest.java`

**Interfaces:**
- Consumes: SLF4J `MDC`
- Produces:
  - `ValidationErrorDetail(String field, String message, Object rejectedValue)`
  - `BaseResponse<T>(T data, String message, String exceptionCode, String traceId, Instant timestamp, List<ValidationErrorDetail> errors)`

- [ ] **Step 1: Write unit test for `BaseResponse` serialization and methods**

Create `ddd-api/src/test/java/com/ddd/api/common/BaseResponseTest.java`:
```java
package com.ddd.api.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaseResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        MDC.put("traceId", "test-trace-id-456");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void of_shouldPopulateDataAndTraceId() {
        BaseResponse<String> response = BaseResponse.of("sample-payload");
        assertThat(response.data()).isEqualTo("sample-payload");
        assertThat(response.message()).isEqualTo(BaseResponse.SUCCESS_REQUEST_MESSAGE);
        assertThat(response.traceId()).isEqualTo("test-trace-id-456");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.errors()).isNull();
    }

    @Test
    void errorWithValidation_shouldPopulateErrorsList() {
        List<ValidationErrorDetail> errors = List.of(
                new ValidationErrorDetail("email", "must not be blank", null)
        );
        BaseResponse<Void> response = BaseResponse.error("000", "Validation failed", errors);
        assertThat(response.exceptionCode()).isEqualTo("000");
        assertThat(response.message()).isEqualTo("Validation failed");
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).field()).isEqualTo("email");
    }

    @Test
    void serialization_shouldOmitNullFields() throws Exception {
        BaseResponse<String> response = BaseResponse.of("hello");
        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"data\":\"hello\"");
        assertThat(json).contains("\"traceId\":\"test-trace-id-456\"");
        assertThat(json).doesNotContain("\"errors\"");
        assertThat(json).doesNotContain("\"exceptionCode\"");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl ddd-api -Dtest=BaseResponseTest`
Expected: FAIL (missing fields, records not found).

- [ ] **Step 3: Create `ValidationErrorDetail` and update `BaseResponse`**

1. Create `ValidationErrorDetail.java`:
```java
package com.ddd.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorDetail(
    String field,
    String message,
    Object rejectedValue
) {}
```
2. Update `BaseResponse.java` adding `traceId`, `timestamp`, `errors` with factory methods maintaining backward compatibility:
```java
package com.ddd.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(
    T data,
    String message,
    String exceptionCode,
    String traceId,
    Instant timestamp,
    List<ValidationErrorDetail> errors
) {
    public static final String SUCCESS_REQUEST_MESSAGE = "Success";

    public static <T> BaseResponse<T> of(T data) {
        return new BaseResponse<>(data, SUCCESS_REQUEST_MESSAGE, null, currentTraceId(), Instant.now(), null);
    }

    public static <T> BaseResponse<T> ok() {
        return new BaseResponse<>(null, SUCCESS_REQUEST_MESSAGE, null, currentTraceId(), Instant.now(), null);
    }

    public static <T> BaseResponse<T> error(String exceptionCode, String exceptionMessage) {
        return new BaseResponse<>(null, exceptionMessage, exceptionCode, currentTraceId(), Instant.now(), null);
    }

    public static <T> BaseResponse<T> error(String exceptionCode, String exceptionMessage, List<ValidationErrorDetail> errors) {
        return new BaseResponse<>(null, exceptionMessage, exceptionCode, currentTraceId(), Instant.now(), errors);
    }

    @SuppressWarnings("unchecked")
    public static <T> BaseResponse<T> empty() {
        return new BaseResponse<>((T) Collections.emptyList(), SUCCESS_REQUEST_MESSAGE, null, currentTraceId(), Instant.now(), null);
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl ddd-api -Dtest=BaseResponseTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add ddd-api/
git commit -m "feat(api): expand BaseResponse with traceId, timestamp, and ValidationErrorDetail"
```

---

### Task 4: Structured Validation & Domain Exception Handling in `GlobalExceptionHandler` (`ddd-api`)

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/exception/GlobalExceptionHandler.java`
- Test: `ddd-api/src/test/java/com/ddd/api/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `DomainException`, `MethodArgumentNotValidException`, `BaseResponse`, `ValidationErrorDetail`.
- Produces: HTTP JSON response with proper HTTP statuses (400, 401, 403, 404, 409, 500) and structured error bodies.

- [ ] **Step 1: Write unit tests for `GlobalExceptionHandler`**

Create `ddd-api/src/test/java/com/ddd/api/exception/GlobalExceptionHandlerTest.java`:
```java
package com.ddd.api.exception;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.common.ValidationErrorDetail;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_shouldReturn404WithBaseResponse() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found with id 10");
        ResponseEntity<Object> response = handler.handleResourceNotFound(ex, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        BaseResponse<Void> body = (BaseResponse<Void>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("User not found with id 10");
    }

    @Test
    void handleBusinessException_shouldReturn400() {
        BusinessException ex = new BusinessException("005", "Event closed");
        ResponseEntity<Object> response = handler.handleBusinessException(ex, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        BaseResponse<Void> body = (BaseResponse<Void>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.exceptionCode()).isEqualTo("005");
        assertThat(body.message()).isEqualTo("Event closed");
    }

    @Test
    void handleMethodArgumentException_shouldReturnStructuredErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("req", "email", "invalid-email", false, null, null, "must be valid")
        ));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        BaseResponse<Void> body = (BaseResponse<Void>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.errors()).hasSize(1);
        ValidationErrorDetail detail = body.errors().get(0);
        assertThat(detail.field()).isEqualTo("email");
        assertThat(detail.message()).isEqualTo("must be valid");
        assertThat(detail.rejectedValue()).isEqualTo("invalid-email");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl ddd-api -Dtest=GlobalExceptionHandlerTest`
Expected: FAIL (handler does not return structured errors yet).

- [ ] **Step 3: Update `GlobalExceptionHandler.java`**

Implement:
1. `wrapWithValidationErrors(...)` returning `BaseResponse.error(code, message, errors)`.
2. Extract `fieldErrors` as `new ValidationErrorDetail(field, message, rejectedValue)` in `handleMethodArgumentException`.
3. Extract parameter validation results in `handleMethodException`.
4. Ensure `handleBusinessException`, `handleResourceNotFound`, etc. use HTTP status codes and domain error codes cleanly.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl ddd-api -Dtest=GlobalExceptionHandlerTest`
Expected: PASS

- [ ] **Step 5: Run all API tests**

Run: `mvn clean test -pl ddd-api`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add ddd-api/
git commit -m "refactor(api): upgrade GlobalExceptionHandler with structured validation details"
```

---

### Task 5: Security Filter Exception Delegation via `HandlerExceptionResolver` (`ddd-infrastructure`)

**Files:**
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/config/security/handler/JwtAccessDeniedHandler.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/config/security/handler/JwtAuthenticationEntryPoint.java`
- Test: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/config/security/handler/SecurityHandlersTest.java`

**Interfaces:**
- Consumes: Spring `HandlerExceptionResolver` bean `@Qualifier("handlerExceptionResolver")`.
- Produces: Filter-level exception dispatch into Spring MVC's `@RestControllerAdvice`.

- [ ] **Step 1: Write unit test for `JwtAccessDeniedHandler` and `JwtAuthenticationEntryPoint`**

Create `ddd-infrastructure/src/test/java/com/ddd/infrastructure/config/security/handler/SecurityHandlersTest.java`:
```java
package com.ddd.infrastructure.config.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.mockito.Mockito.*;

class SecurityHandlersTest {

    private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    void accessDeniedHandler_shouldDelegateToResolver() {
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(resolver);
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        handler.handle(request, response, ex);

        verify(resolver).resolveException(request, response, null, ex);
    }

    @Test
    void authenticationEntryPoint_shouldDelegateToResolver() {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(resolver);
        AuthenticationException ex = mock(AuthenticationException.class);

        entryPoint.commence(request, response, ex);

        verify(resolver).resolveException(request, response, null, ex);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl ddd-infrastructure -Dtest=SecurityHandlersTest`
Expected: FAIL (constructor parameter mismatch).

- [ ] **Step 3: Update `JwtAccessDeniedHandler` and `JwtAuthenticationEntryPoint`**

1. Update `JwtAccessDeniedHandler.java`:
```java
package com.ddd.infrastructure.config.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public JwtAccessDeniedHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        resolver.resolveException(request, response, null, accessDeniedException);
    }
}
```
2. Update `JwtAuthenticationEntryPoint.java`:
```java
package com.ddd.infrastructure.config.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        resolver.resolveException(request, response, null, authException);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl ddd-infrastructure -Dtest=SecurityHandlersTest`
Expected: PASS

- [ ] **Step 5: Run full project build and verify all modules pass**

Run: `mvn clean test`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add ddd-infrastructure/
git commit -m "refactor(infra): delegate security filter exceptions to HandlerExceptionResolver"
```
