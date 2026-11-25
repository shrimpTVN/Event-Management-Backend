package com.ddd.domain.exception;

import java.io.Serial;

public class EventFullException extends EventRegistrationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EventFullException() {
        super("Event is full.");
    }
}
