package com.ddd.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.CONFLICT)
public class StaffProfileAlreadyExistsException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public StaffProfileAlreadyExistsException() {
        super("User already has a staff profile.");
    }

    public StaffProfileAlreadyExistsException(String message) {
        super(message);
    }
}
