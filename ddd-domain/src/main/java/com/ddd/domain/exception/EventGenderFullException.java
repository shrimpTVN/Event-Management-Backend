package com.ddd.domain.exception;

import java.io.Serial;

public class EventGenderFullException extends EventRegistrationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EventGenderFullException() {
        super("Event slots for your gender are full.");
    }

    public EventGenderFullException(String message) {
        super(message);
    }
}
