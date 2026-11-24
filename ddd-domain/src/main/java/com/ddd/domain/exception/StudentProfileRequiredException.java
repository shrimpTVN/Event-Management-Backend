package com.ddd.domain.exception;

import java.io.Serial;

public class StudentProfileRequiredException extends BusinessException {
    @Serial
    private static final long serialVersionUID = 1L;

    public StudentProfileRequiredException() {
        super("Please complete your student profile before registering for events.");
    }

    public StudentProfileRequiredException(String message) {
        super(message);
    }

    public StudentProfileRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
