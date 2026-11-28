package com.ddd.domain.exception;

import java.io.Serial;

public class EventAttendanceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public EventAttendanceException(String message) {
        super(message);
        this.errorCode = null;
    }

    public EventAttendanceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    private EventAttendanceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static EventAttendanceException withCode(String errorCode, String message) {
        return new EventAttendanceException(errorCode, message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
