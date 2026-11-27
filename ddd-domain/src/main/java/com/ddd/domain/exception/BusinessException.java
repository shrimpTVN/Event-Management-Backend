package com.ddd.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException  extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final Map<String, String> fieldErrors;

    public BusinessException(String message) {
        super(message);
        this.fieldErrors = Collections.emptyMap();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.fieldErrors = Collections.emptyMap();
    }

    public BusinessException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null || fieldErrors.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public static BusinessException withField(String field, String message) {
        if (field == null || field.isBlank()) {
            return new BusinessException(message);
        }
        return new BusinessException(message, Map.of(field, message));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
