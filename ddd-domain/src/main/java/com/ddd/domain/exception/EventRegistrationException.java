package com.ddd.domain.exception;

import java.io.Serial;

public class EventRegistrationException  extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 1L;

    public EventRegistrationException(String message){
        super(message);
    }

    public EventRegistrationException(String message, Throwable cause){
        super(message, cause);
    }
}
