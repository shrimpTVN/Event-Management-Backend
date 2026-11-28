package com.ddd.api.exception;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.common.enums.ErrorCodeEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.EventAttendanceException;
import com.ddd.domain.exception.EventRegistrationException;
import com.ddd.domain.exception.ForbiddenException;
import com.ddd.domain.exception.InvalidOldPasswordException;
import com.ddd.domain.exception.StudentIdAlreadyExistsException;
import org.springframework.security.authentication.DisabledException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerEventTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private WebRequest webRequest;

    @Test
    void handleBusinessException_withFieldErrors_returns400AndDataMap() {
        BusinessException ex = BusinessException.withField("dateOpen", "Registration open date cannot be in the past");

        ResponseEntity<Object> response = handler.handleBusinessException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        BaseResponse<?> body = assertInstanceOf(BaseResponse.class, response.getBody());
        assertEquals(ErrorCodeEnum.BUSINESS_RULE_VIOLATION.getCode(), body.exceptionCode());
        assertEquals("Registration open date cannot be in the past", body.message());
        Map<?, ?> data = assertInstanceOf(Map.class, body.data());
        assertEquals("Registration open date cannot be in the past", data.get("dateOpen"));
    }

    @Test
    void handleBusinessException_withoutFieldErrors_returns400AndNullData() {
        BusinessException ex = new BusinessException("A completed event cannot be updated.");

        ResponseEntity<Object> response = handler.handleBusinessException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        BaseResponse<?> body = assertInstanceOf(BaseResponse.class, response.getBody());
        assertEquals(ErrorCodeEnum.BUSINESS_RULE_VIOLATION.getCode(), body.exceptionCode());
        assertEquals("A completed event cannot be updated.", body.message());
        assertNull(body.data());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void handleMethodArgumentException_returns400WithStructuredFieldErrorsInData() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "eventUpdateRequestDto");
        bindingResult.addError(new FieldError("eventUpdateRequestDto", "capacity", "must be greater than 0"));
        MethodParameter param = new MethodParameter(GlobalExceptionHandlerEventTest.class.getDeclaredMethod("dummyTarget", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        BaseResponse<?> body = assertInstanceOf(BaseResponse.class, response.getBody());
        assertEquals(ErrorCodeEnum.VALIDATION_ERROR.getCode(), body.exceptionCode());
        assertEquals("must be greater than 0", body.message());
        Map<?, ?> data = assertInstanceOf(Map.class, body.data());
        assertEquals("must be greater than 0", data.get("capacity"));
    }

    @Test
    void handleMethodException_returns400WithStructuredFieldErrorsInData() throws NoSuchMethodException {
        MethodParameter param = new MethodParameter(GlobalExceptionHandlerEventTest.class.getDeclaredMethod("dummyTarget", String.class), 0);
        ParameterValidationResult validationResult = mock(ParameterValidationResult.class);
        MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
        when(resolvable.getDefaultMessage()).thenReturn("must not be blank");
        when(validationResult.getMethodParameter()).thenReturn(param);
        when(validationResult.getResolvableErrors()).thenReturn(List.of(resolvable));

        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        when(ex.getParameterValidationResults()).thenReturn(List.of(validationResult));

        ResponseEntity<Object> response = handler.handleMethodException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        BaseResponse<?> body = assertInstanceOf(BaseResponse.class, response.getBody());
        assertEquals(ErrorCodeEnum.VALIDATION_ERROR.getCode(), body.exceptionCode());
        assertEquals("must not be blank", body.message());
        Map<?, ?> data = assertInstanceOf(Map.class, body.data());
        assertEquals("must not be blank", data.get(param.getParameterName()));
    }

    @Test
    void handleForbiddenAndDomainCodeExceptions_returnExpectedStatusesAndCodes() {
        ResponseEntity<Object> forbidden = handler.handleForbiddenException(
                new ForbiddenException("You are not admin of the fanpage"), webRequest);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertEquals("002", assertInstanceOf(BaseResponse.class, forbidden.getBody()).exceptionCode());

        ResponseEntity<Object> oldPwd = handler.handleInvalidOldPasswordException(
                new InvalidOldPasswordException("Old password is incorrect."), webRequest);
        assertEquals(HttpStatus.BAD_REQUEST, oldPwd.getStatusCode());
        assertEquals("AUTH.009", assertInstanceOf(BaseResponse.class, oldPwd.getBody()).exceptionCode());

        ResponseEntity<Object> studentDup = handler.handleStudentIdAlreadyExistsException(
                new StudentIdAlreadyExistsException("Student ID already exists"), webRequest);
        assertEquals(HttpStatus.CONFLICT, studentDup.getStatusCode());
        assertEquals("STUDENT.002", assertInstanceOf(BaseResponse.class, studentDup.getBody()).exceptionCode());

        ResponseEntity<Object> disabled = handler.handleDisabledException(
                new DisabledException("User is disabled"), webRequest);
        assertEquals(HttpStatus.UNAUTHORIZED, disabled.getStatusCode());
        assertEquals("AUTH.010", assertInstanceOf(BaseResponse.class, disabled.getBody()).exceptionCode());

        ResponseEntity<Object> dupEmail = handler.handleDuplicateResource(
                DuplicateResourceException.withCode("AUTH.003", "Email already exists"), webRequest);
        assertEquals(HttpStatus.CONFLICT, dupEmail.getStatusCode());
        assertEquals("AUTH.003", assertInstanceOf(BaseResponse.class, dupEmail.getBody()).exceptionCode());

        ResponseEntity<Object> cancelLate = handler.handleEventRegistrationException(
                EventRegistrationException.withCode("EVENT.007", "Too late"), webRequest);
        assertEquals("EVENT.007", assertInstanceOf(BaseResponse.class, cancelLate.getBody()).exceptionCode());

        ResponseEntity<Object> selfCheckDisabled = handler.handleEventAttendanceException(
                EventAttendanceException.withCode("EVENT.008", "Disabled"), webRequest);
        assertEquals("EVENT.008", assertInstanceOf(BaseResponse.class, selfCheckDisabled.getBody()).exceptionCode());

        ResponseEntity<Object> otpInvalid = handler.handleBusinessException(
                BusinessException.withCode("AUTH.016", "Invalid OTP"), webRequest);
        assertEquals("AUTH.016", assertInstanceOf(BaseResponse.class, otpInvalid.getBody()).exceptionCode());

        ResponseEntity<Object> methodNotAllowed = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("GET"), webRequest);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, methodNotAllowed.getStatusCode());
        assertEquals("008", assertInstanceOf(BaseResponse.class, methodNotAllowed.getBody()).exceptionCode());

        var mockRequest = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/auth/refresh-token");
        ResponseEntity<Object> lockConflict = handler.handleLockFailure(mockRequest);
        assertEquals(HttpStatus.CONFLICT, lockConflict.getStatusCode());
        assertEquals(ErrorCodeEnum.CONCURRENT_MODIFICATION.getCode(),
                assertInstanceOf(BaseResponse.class, lockConflict.getBody()).exceptionCode());

        ResponseEntity<Object> badCreds = handler.handleBadCredentialsException(
                new org.springframework.security.authentication.BadCredentialsException("bad"), webRequest);
        assertEquals(HttpStatus.UNAUTHORIZED, badCreds.getStatusCode());
        assertEquals("AUTH.005", assertInstanceOf(BaseResponse.class, badCreds.getBody()).exceptionCode());

        ResponseEntity<Object> unauth = handler.handleUnauthorized(
                new com.ddd.domain.exception.UnauthorizedException("Custom unauth message"), webRequest);
        assertEquals(HttpStatus.UNAUTHORIZED, unauth.getStatusCode());
        assertEquals("Custom unauth message", assertInstanceOf(BaseResponse.class, unauth.getBody()).message());
    }

    @SuppressWarnings("unused")
    private void dummyTarget(String input) {
    }
}
