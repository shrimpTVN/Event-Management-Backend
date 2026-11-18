package com.ddd.domain.exception;

import java.io.Serial;

public class FanpageAdminProfileRequiredException extends BusinessException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FanpageAdminProfileRequiredException() {
        super("Please complete your fanpage admin profile before creating a fanpage.");
    }

    public FanpageAdminProfileRequiredException(String message) {
        super(message);
    }

    public FanpageAdminProfileRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
