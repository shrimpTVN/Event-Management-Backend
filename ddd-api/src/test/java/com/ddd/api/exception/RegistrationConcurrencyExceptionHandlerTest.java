package com.ddd.api.exception;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.common.enums.ErrorCodeEnum;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.EventFullException;
import com.ddd.domain.exception.EventGenderFullException;
import com.ddd.domain.exception.EventRegistrationException;
import com.ddd.domain.exception.StudentProfileRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationConcurrencyExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @Mock
    private WebRequest webRequest;

    @Test
    void handleEventFullReturns409WithEvent004Code() {
        when(request.getRequestURI()).thenReturn("/registrations/me/events/10");

        ResponseEntity<Object> response = handler.handleEventFull(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        BaseResponse<?> body = assertInstanceOf(BaseResponse.class, response.getBody());
        assertEquals("EVENT.004", body.exceptionCode());
        assertEquals("Event is full.", body.message());
        assertNull(body.data());
    }

    @Test
    void handleEventGenderFullReturns409WithEvent005Code() {
        when(request.getRequestURI()).thenReturn("/registrations/me/events/10");

        ResponseEntity<Object> response = handler.handleEventGenderFull(
                new EventGenderFullException("Slots for male participants are full."), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        BaseResponse<?> body = assertInstanceOf(BaseResponse.class, response.getBody());
        assertEquals("EVENT.005", body.exceptionCode());
        assertEquals("Slots for male participants are full.", body.message());
        assertNull(body.data());
    }

    @Test
    void handleLockFailureOnRegistrationEndpointReturnsRegistrationBusyMessage() {
        when(request.getRequestURI()).thenReturn("/api/registrations/me/events/10");
        when(request.getContextPath()).thenReturn("/api");

        ResponseEntity<Object> response = handler.handleLockFailure(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        BaseResponse<?> body = assertInstanceOf(BaseResponse.class, response.getBody());
        assertEquals(ErrorCodeEnum.CONCURRENT_MODIFICATION.getCode(), body.exceptionCode());
        assertEquals("Registration is busy. Please try again.", body.message());
        assertNull(body.data());
    }

    @Test
    void handleLockFailureOnNonRegistrationEndpointReturnsGenericBusyMessage() {
        when(request.getRequestURI()).thenReturn("/events/10");
        when(request.getContextPath()).thenReturn("");

        ResponseEntity<Object> response = handler.handleLockFailure(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        BaseResponse<?> body = assertInstanceOf(BaseResponse.class, response.getBody());
        assertEquals("005", body.exceptionCode());
        assertEquals("Resource is busy. Please try again.", body.message());
    }

    @Test
    void unchangedDuplicateProfileAndEventRegistrationErrorsRemainIntact() {
        when(webRequest.getDescription(false)).thenReturn("uri=/registrations/me/events/10");

        ResponseEntity<Object> duplicateResponse = handler.handleDuplicateResource(
                new DuplicateResourceException("Already registered"), webRequest);
        assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());
        BaseResponse<?> duplicateBody = assertInstanceOf(BaseResponse.class, duplicateResponse.getBody());
        assertEquals(ErrorCodeEnum.DUPLICATE_RESOURCE.getCode(), duplicateBody.exceptionCode());
        assertEquals("Already registered", duplicateBody.message());

        ResponseEntity<Object> profileResponse = handler.handleStudentProfileRequiredException(
                new StudentProfileRequiredException(), webRequest);
        assertEquals(ErrorCodeEnum.STUDENT_PROFILE_REQUIRED.getStatus(), profileResponse.getStatusCode());
        BaseResponse<?> profileBody = assertInstanceOf(BaseResponse.class, profileResponse.getBody());
        assertEquals(ErrorCodeEnum.STUDENT_PROFILE_REQUIRED.getCode(), profileBody.exceptionCode());

        ResponseEntity<Object> registrationResponse = handler.handleEventRegistrationException(
                new EventRegistrationException("Registration closed"), webRequest);
        assertEquals(ErrorCodeEnum.EVENT_REGISTRATION_ERROR.getStatus(), registrationResponse.getStatusCode());
        BaseResponse<?> registrationBody = assertInstanceOf(BaseResponse.class, registrationResponse.getBody());
        assertEquals("EVENT.002", registrationBody.exceptionCode());
        assertEquals("Registration closed", registrationBody.message());

        // Verify CannotAcquireLockException is a PessimisticLockingFailureException handled by handleLockFailure
        assertInstanceOf(PessimisticLockingFailureException.class, new CannotAcquireLockException("timeout"));
    }
}
