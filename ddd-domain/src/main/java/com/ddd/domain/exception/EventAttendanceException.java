package com.ddd.domain.exception;

import java.io.Serial;

public class EventAttendanceException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 1L;

    public EventAttendanceException(String message) {
        super(message);
    }

    public EventAttendanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
