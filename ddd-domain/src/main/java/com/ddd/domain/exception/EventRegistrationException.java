package com.ddd.domain.exception;

import java.io.Serial;

public class EventRegistrationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public EventRegistrationException(String message) {
        super(message);
        this.errorCode = null;
    }

    public EventRegistrationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    private EventRegistrationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static EventRegistrationException withCode(String errorCode, String message) {
        return new EventRegistrationException(errorCode, message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
