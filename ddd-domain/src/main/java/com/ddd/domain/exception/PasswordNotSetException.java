package com.ddd.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.CONFLICT)
public class PasswordNotSetException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public PasswordNotSetException() {
        super("User does not have a password set. Please use create-password instead.");
    }

    public PasswordNotSetException(String message) {
        super(message);
    }
}
