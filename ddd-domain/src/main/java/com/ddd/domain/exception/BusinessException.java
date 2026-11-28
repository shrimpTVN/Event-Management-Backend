package com.ddd.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException  extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String errorCode;
    private final Map<String, String> fieldErrors;

    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
        this.fieldErrors = Collections.emptyMap();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.fieldErrors = Collections.emptyMap();
    }

    public BusinessException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.errorCode = null;
        this.fieldErrors = fieldErrors == null || fieldErrors.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    private BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = Collections.emptyMap();
    }

    public static BusinessException withCode(String errorCode, String message) {
        return new BusinessException(errorCode, message);
    }

    public static BusinessException withField(String field, String message) {
        if (field == null || field.isBlank()) {
            return new BusinessException(message);
        }
        return new BusinessException(message, Map.of(field, message));
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
