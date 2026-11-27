package com.ddd.api.exception;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.common.enums.ErrorCodeEnum;
import com.ddd.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

    @SuppressWarnings("unused")
    private void dummyTarget(String input) {
    }
}
