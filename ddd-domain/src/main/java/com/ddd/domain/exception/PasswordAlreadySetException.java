package com.ddd.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.CONFLICT)
public class PasswordAlreadySetException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public PasswordAlreadySetException() {
        super("User already has a password set. Please use change-password instead.");
    }

    public PasswordAlreadySetException(String message) {
        super(message);
    }
}
