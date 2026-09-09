package com.exception;


import com.dto.auth.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

///**
// * Centralized exception handler for all REST controllers.
// *
// * <p>HTTP status mapping:
// * <ul>
// *   <li>400 Bad Request              — {@link IllegalArgumentException}, {@link MethodArgumentNotValidException}, {@link HandlerMethodValidationException}</li>
// *   <li>401 Unauthorized             — {@link UnauthorizedException}</li>
// *   <li>403 Forbidden                — {@link AccessDeniedException}</li>
// *   <li>404 Not Found                — {@link ResourceNotFoundException}</li>
// *   <li>409 Conflict                 — {@link ConcurrentSeatBookingException}, {@link DuplicateResourceException}</li>
// *   <li>422 Unprocessable Entity     — {@link BusinessRuleException}</li>
// *   <li>502 Bad Gateway              — {@link PaymentException}</li>
// *   <li>500 Internal Server Error    — all other {@link Exception}</li>
// * </ul>
// */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────────────────────────────────
    // 400 Bad Request
    // ─────────────────────────────────────────────────────────────────────────

    /** Bean-validation failures on @RequestBody (e.g. @NotNull, @Size). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        List<FieldError> fieldErrorList = exception.getBindingResult().getFieldErrors();
        fieldErrorList.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    /** Constraint-validation failures on @PathVariable / @RequestParam. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, String>> handleMethodException(
            HandlerMethodValidationException exception) {
        Map<String, String> errors = new HashMap<>();
        List<ParameterValidationResult> results = exception.getParameterValidationResults();
        results.forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            String combinedMsg = result.getResolvableErrors()
                    .stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            errors.put(paramName, combinedMsg);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    /** General bad-input from service layer. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException exception, WebRequest webRequest) {
        log.warn("Bad request [{}]: {}", webRequest.getDescription(false), exception.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), webRequest.getDescription(false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 401 Unauthorized
    // ─────────────────────────────────────────────────────────────────────────

//    @ExceptionHandler(UnauthorizedException.class)
//    public ResponseEntity<ErrorResponseDto> handleUnauthorized(
//            UnauthorizedException exception, WebRequest webRequest) {
//        log.warn("Unauthorized [{}]: {}", webRequest.getDescription(false), exception.getMessage());
//        return buildError(HttpStatus.UNAUTHORIZED, exception.getMessage(), webRequest.getDescription(false));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // 403 Forbidden
//    // ─────────────────────────────────────────────────────────────────────────
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
//            AccessDeniedException exception, WebRequest webRequest) {
//        log.warn("Access denied [{}]: {}", webRequest.getDescription(false), exception.getMessage());
//        return buildError(
//                HttpStatus.FORBIDDEN,
//                "Bạn không có quyền thực hiện hành động này.",
//                webRequest.getDescription(false));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // 404 Not Found
//    // ─────────────────────────────────────────────────────────────────────────
//
//    @ExceptionHandler(ResourceNotFoundException.class)
//    public ResponseEntity<ErrorResponseDto> handleNotFound(
//            ResourceNotFoundException exception, WebRequest webRequest) {
//        log.warn("Resource not found [{}]: {}", webRequest.getDescription(false), exception.getMessage());
//        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), webRequest.getDescription(false));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // 409 Conflict
//    // ─────────────────────────────────────────────────────────────────────────
//
//    /** Optimistic-locking / concurrent seat collision. */
//    @ExceptionHandler(ConcurrentSeatBookingException.class)
//    public ResponseEntity<ErrorResponseDto> handleConcurrentSeatBooking(
//            ConcurrentSeatBookingException exception, HttpServletRequest request) {
//        log.warn("Booking conflict [{}]: {}", request.getRequestURI(), exception.getMessage());
//        return buildError(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
//    }
//
//    /** Attempt to create a resource that already exists. */
//    @ExceptionHandler(DuplicateResourceException.class)
//    public ResponseEntity<ErrorResponseDto> handleDuplicateResource(
//            DuplicateResourceException exception, WebRequest webRequest) {
//        log.warn("Duplicate resource [{}]: {}", webRequest.getDescription(false), exception.getMessage());
//        return buildError(HttpStatus.CONFLICT, exception.getMessage(), webRequest.getDescription(false));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // 422 Unprocessable Entity
//    // ─────────────────────────────────────────────────────────────────────────
//
//    /** Valid request but violates a domain business rule. */
//    @ExceptionHandler(BusinessRuleException.class)
//    public ResponseEntity<ErrorResponseDto> handleBusinessRule(
//            BusinessRuleException exception, WebRequest webRequest) {
//        log.warn("Business rule violation [{}]: {}", webRequest.getDescription(false), exception.getMessage());
//        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), webRequest.getDescription(false));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // 502 Bad Gateway
//    // ─────────────────────────────────────────────────────────────────────────
//
//    /** External payment gateway communication failure. */
//    @ExceptionHandler(PaymentException.class)
//    public ResponseEntity<ErrorResponseDto> handlePaymentException(
//            PaymentException exception, WebRequest webRequest) {
//        log.error("Payment gateway error [{}]: {}", webRequest.getDescription(false), exception.getMessage(), exception);
//        return buildError(HttpStatus.BAD_GATEWAY, exception.getMessage(), webRequest.getDescription(false));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // 500 Internal Server Error — catch-all
//    // ─────────────────────────────────────────────────────────────────────────
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponseDto> handleException(
//            Exception exception, WebRequest webRequest) {
//        // Log the full stack trace so developers can diagnose unexpected failures.
//        log.error("Unexpected error [{}]", webRequest.getDescription(false), exception);
//        return buildError(
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.",
//                webRequest.getDescription(false));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // Helper
//    // ─────────────────────────────────────────────────────────────────────────
//
    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message, String path) {
        ErrorResponse body = new ErrorResponse(path, status, message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
