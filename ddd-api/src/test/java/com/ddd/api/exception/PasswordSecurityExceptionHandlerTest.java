package com.ddd.api.exception;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.common.enums.ErrorCodeEnum;
import com.ddd.domain.exception.PasswordAlreadySetException;
import com.ddd.domain.exception.PasswordNotSetException;
import com.ddd.domain.exception.StaffIdAlreadyExistsException;
import com.ddd.domain.exception.StaffProfileAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PasswordSecurityExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private WebRequest webRequest;

    @Test
    @DisplayName("handlePasswordAlreadySetException should return 409 with PASSWORD_ALREADY_SET code")
    void handlePasswordAlreadySetException() {
        PasswordAlreadySetException ex = new PasswordAlreadySetException();

        ResponseEntity<Object> response = handler.handlePasswordAlreadySetException(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof BaseResponse<?>);
        BaseResponse<?> body = (BaseResponse<?>) response.getBody();
        assertEquals(ErrorCodeEnum.PASSWORD_ALREADY_SET.getCode(), body.exceptionCode());
        assertEquals("User already has a password set. Please use change-password instead.", body.message());
    }

    @Test
    @DisplayName("handlePasswordNotSetException should return 409 with PASSWORD_NOT_SET code")
    void handlePasswordNotSetException() {
        PasswordNotSetException ex = new PasswordNotSetException();

        ResponseEntity<Object> response = handler.handlePasswordNotSetException(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof BaseResponse<?>);
        BaseResponse<?> body = (BaseResponse<?>) response.getBody();
        assertEquals(ErrorCodeEnum.PASSWORD_NOT_SET.getCode(), body.exceptionCode());
        assertEquals("User does not have a password set. Please use create-password instead.", body.message());
    }

    @Test
    @DisplayName("handleStaffProfileAlreadyExistsException should return 409 with STAFF_PROFILE_ALREADY_EXISTS code")
    void handleStaffProfileAlreadyExistsException() {
        StaffProfileAlreadyExistsException ex = new StaffProfileAlreadyExistsException();

        ResponseEntity<Object> response = handler.handleStaffProfileAlreadyExistsException(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof BaseResponse<?>);
        BaseResponse<?> body = (BaseResponse<?>) response.getBody();
        assertEquals(ErrorCodeEnum.STAFF_PROFILE_ALREADY_EXISTS.getCode(), body.exceptionCode());
        assertEquals("User already has a staff profile.", body.message());
    }

    @Test
    @DisplayName("handleStaffIdAlreadyExistsException should return 409 with STAFF_ID_ALREADY_EXISTS code")
    void handleStaffIdAlreadyExistsException() {
        StaffIdAlreadyExistsException ex = new StaffIdAlreadyExistsException();

        ResponseEntity<Object> response = handler.handleStaffIdAlreadyExistsException(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof BaseResponse<?>);
        BaseResponse<?> body = (BaseResponse<?>) response.getBody();
        assertEquals(ErrorCodeEnum.STAFF_ID_ALREADY_EXISTS.getCode(), body.exceptionCode());
        assertEquals("Staff ID already exists.", body.message());
    }
}
