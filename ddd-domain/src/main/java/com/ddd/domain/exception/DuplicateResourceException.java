package com.ddd.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public DuplicateResourceException(String message) {
        super(message);
        this.errorCode = null;
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    private DuplicateResourceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static DuplicateResourceException withCode(String errorCode, String message) {
        return new DuplicateResourceException(errorCode, message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
