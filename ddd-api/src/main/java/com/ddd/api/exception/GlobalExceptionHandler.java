package com.ddd.api.exception;


import com.ddd.api.common.BaseResponse;
import com.ddd.api.common.enums.ErrorCodeEnum;
import com.ddd.domain.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

//import org.springframework.security.access.AccessDeniedException;

/// **
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

    private ResponseEntity<Object> wrapWithResponse(String code, String message, HttpStatus status) {
        return ResponseEntity.status(status).body(BaseResponse.error(code, message));
    }

    // 400 Bad Request

    /**
     * Bean-validation failures on @RequestBody (e.g. @NotNull, @Size).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        List<FieldError> fieldErrorList = exception.getBindingResult().getFieldErrors();
        fieldErrorList.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        log.warn("Method Argument Not Valid occurred: {}", errors, exception);
        return wrapWithResponse(ErrorCodeEnum.VALIDATION_ERROR.getCode(), errors.toString(), ErrorCodeEnum.VALIDATION_ERROR.getStatus());
    }

    /**
     * Constraint-validation failures on @PathVariable / @RequestParam.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> handleMethodException(
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
        log.warn("Method Validation occurred: {}", errors, exception);
        return wrapWithResponse(ErrorCodeEnum.VALIDATION_ERROR.getCode(),
                errors.toString(), ErrorCodeEnum.VALIDATION_ERROR.getStatus());
    }

    /**
     * General bad-input from service layer.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleBadRequest(
            IllegalArgumentException exception, WebRequest webRequest) {
        log.warn("Bad request [{}]: {}", webRequest.getDescription(false), exception.getMessage());
        return wrapWithResponse(ErrorCodeEnum.VALIDATION_ERROR.getCode(),
                exception.getMessage(), ErrorCodeEnum.VALIDATION_ERROR.getStatus());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(
            BadCredentialsException exception, WebRequest webRequest){
        log.warn("Bad credentials [{}]: {}", webRequest.getDescription(false), exception.getMessage());
        return wrapWithResponse(
                ErrorCodeEnum.AUTH_INVALID_CREDENTIALS.getCode(),
                ErrorCodeEnum.AUTH_INVALID_CREDENTIALS.getDefaultMessage(),
                ErrorCodeEnum.AUTH_INVALID_CREDENTIALS.getStatus());
    }

    //     401 Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(
            UnauthorizedException exception, WebRequest webRequest) {
        log.warn("Unauthorized [{}]: {}", webRequest.getDescription(false), exception.getMessage());
        return wrapWithResponse(ErrorCodeEnum.UNAUTHORIZED_ERROR.getCode(),
                ErrorCodeEnum.UNAUTHORIZED_ERROR.getDefaultMessage(),
                ErrorCodeEnum.UNAUTHORIZED_ERROR.getStatus());
    }

    // 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(
            AccessDeniedException exception, WebRequest webRequest) {
        log.warn("Access denied [{}]: {}", webRequest.getDescription(false), exception.getMessage());
        return wrapWithResponse(
                ErrorCodeEnum.ACCESS_DENIED_ERROR.getCode(),
                ErrorCodeEnum.ACCESS_DENIED_ERROR.getDefaultMessage(),
                ErrorCodeEnum.ACCESS_DENIED_ERROR.getStatus()
                );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 404 Not Found
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler( NoResourceFoundException.class)
    public ResponseEntity<Object> handleNotFound(
            NoResourceFoundException exception, WebRequest webRequest) {
        log.warn("Resource not found [{}]: {}", webRequest.getDescription(false), exception.getMessage());
        return wrapWithResponse(ErrorCodeEnum.NOT_FOUND_ERROR.getCode(),
                ErrorCodeEnum.NOT_FOUND_ERROR.getDefaultMessage(),
                ErrorCodeEnum.NOT_FOUND_ERROR.getStatus());
    }
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

    // 500 Internal Server Error — catch-all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(
            Exception exception, WebRequest webRequest) {
        // Log the full stack trace so developers can diagnose unexpected failures.
        log.error("Unexpected error [{}]", webRequest.getDescription(false), exception);
        return wrapWithResponse(ErrorCodeEnum.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCodeEnum.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                ErrorCodeEnum.INTERNAL_SERVER_ERROR.getStatus());
    }

}
